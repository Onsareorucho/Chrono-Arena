package client.network;

import shared.GameConstants;
import shared.protocol.*;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCPClientHandler manages the client side of the TCP connection.
 *
 * Responsibilities:
 *  - Connect to the server and send a JoinRequest
 *  - Receive the JoinResponse (gets our playerId + UDP port)
 *  - Continuously receive messages from the server:
 *      GameStateUpdate → delivered to GUI via onGameStateUpdate
 *      ScoreUpdate     → delivered to GUI via onScoreUpdate
 *      PlayerEvent     → delivered to GUI via onPlayerEvent
 *      KillClient      → triggers graceful shutdown
 *      GameOver        → notifies GUI of round end
 *  - Send periodic Heartbeat messages so the server knows we're alive
 *
 * Threading:
 *  - Caller creates this on the main thread and calls connect()
 *  - A reader thread is spawned inside start() to receive messages
 *  - A heartbeat thread sends pings every HEARTBEAT_INTERVAL_MS
 *  - GUI callbacks are invoked on the reader thread — if your GUI
 *    toolkit (Swing) requires EDT, wrap callbacks with SwingUtilities.invokeLater
 *
 * Integration:
 *  GameScreen calls: tcpClient.onGameStateUpdate = screen::updateState;
 */
public class TCPClientHandler {

    private static final Logger LOG = Logger.getLogger(TCPClientHandler.class.getName());

    private final String serverIp;
    private final int tcpPort;
    private final String playerName;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream  in;

    private volatile boolean running = false;
    private volatile int assignedPlayerId = -1;
    private volatile int udpPort = -1;

    // ──────────────────────────────────────────────────────────
    // Callbacks — wire these to the GUI before calling connect()
    // ──────────────────────────────────────────────────────────

    /** Called when the server sends a full game state snapshot. */
    public Consumer<GameStateUpdate> onGameStateUpdate = update -> {};

    /** Called when the server sends a lightweight score-only update. */
    public Consumer<ScoreUpdate> onScoreUpdate = update -> {};

    /** Called when a player joins, leaves, gets frozen, etc. */
    public Consumer<PlayerEvent> onPlayerEvent = event -> {};

    /** Called when this client is killed by the server. */
    public Consumer<KillClient> onKillReceived = kill -> {};

    /** Called when the round ends with final scores. */
    public Consumer<GameOver> onGameOver = go -> {};

    /** Called when the connection drops unexpectedly. */
    public Runnable onDisconnected = () -> {};

    // ──────────────────────────────────────────────────────────
    // Construction
    // ──────────────────────────────────────────────────────────

    public TCPClientHandler(String serverIp, int tcpPort, String playerName) {
        this.serverIp   = serverIp;
        this.tcpPort    = tcpPort;
        this.playerName = playerName;
    }

    // ──────────────────────────────────────────────────────────
    // Connection
    // ──────────────────────────────────────────────────────────

    /**
     * Connect to the server, send JoinRequest, and wait for JoinResponse.
     *
     * @return The JoinResponse (check response.accepted before continuing)
     * @throws IOException if connection or join fails
     */
    public JoinResponse connect() throws IOException {
        socket = new Socket(serverIp, tcpPort);
        socket.setTcpNoDelay(true);
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in  = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        LOG.info("Connected to " + serverIp + ":" + tcpPort);

        // Send join request
        send(new JoinRequest(playerName));

        // Wait for response (blocking — no reader thread yet)
        Message response = readMessage();
        if (response == null || response.type != Message.MessageType.JOIN_RESPONSE) {
            throw new IOException("Did not receive JoinResponse from server");
        }

        JoinResponse jr = (JoinResponse) response;
        if (jr.accepted) {
            assignedPlayerId = jr.assignedPlayerId;
            udpPort = jr.udpPort;
            LOG.info("Joined as playerId=" + assignedPlayerId + ", UDP port=" + udpPort);
        }
        return jr;
    }

    /**
     * Start the reader and heartbeat threads.
     * Call only after a successful connect().
     */
    public void start() {
        running = true;

        Thread reader = new Thread(this::readLoop, "tcp-client-reader");
        reader.setDaemon(true);
        reader.start();

        Thread heartbeat = new Thread(this::heartbeatLoop, "tcp-client-heartbeat");
        heartbeat.setDaemon(true);
        heartbeat.start();
    }

    public void stop() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    // ──────────────────────────────────────────────────────────
    // Reader loop
    // ──────────────────────────────────────────────────────────

    private void readLoop() {
        while (running) {
            Message msg = readMessage();
            if (msg == null) break;
            dispatch(msg);
        }
        if (running) {
            running = false;
            onDisconnected.run();
        }
    }

    private void dispatch(Message msg) {
        switch (msg.type) {
            case GAME_STATE_UPDATE -> onGameStateUpdate.accept((GameStateUpdate) msg);
            case SCORE_UPDATE      -> onScoreUpdate.accept((ScoreUpdate) msg);
            case PLAYER_EVENT      -> onPlayerEvent.accept((PlayerEvent) msg);
            case KILL_CLIENT       -> {
                KillClient kc = (KillClient) msg;
                LOG.warning("Server killed this client: " + kc.reason);
                onKillReceived.accept(kc);
                stop();
            }
            case GAME_OVER         -> onGameOver.accept((GameOver) msg);
            case HEARTBEAT_ACK     -> {} // heartbeat roundtrip — could log RTT here
            default -> LOG.fine("Unhandled message type: " + msg.type);
        }
    }

    // ──────────────────────────────────────────────────────────
    // Heartbeat loop
    // ──────────────────────────────────────────────────────────

    private void heartbeatLoop() {
        while (running) {
            try {
                Thread.sleep(GameConstants.HEARTBEAT_INTERVAL_MS);
                if (running && assignedPlayerId != -1) {
                    send(new Heartbeat(assignedPlayerId));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Low-level I/O (length-prefixed messages, matches server)
    // ──────────────────────────────────────────────────────────

    private synchronized void send(Message message) {
        try {
            byte[] bytes = MessageSerializer.toJson(message).getBytes("UTF-8");
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Send failed: " + e.getMessage());
            running = false;
        }
    }

    private Message readMessage() {
        try {
            int length = in.readInt();
            if (length <= 0 || length > 65536) return null;
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            return MessageSerializer.fromJson(new String(bytes, "UTF-8"));
        } catch (IOException e) {
            if (running) LOG.log(Level.WARNING, "Read failed: " + e.getMessage());
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────
    // Accessors
    // ──────────────────────────────────────────────────────────

    public int getAssignedPlayerId() { return assignedPlayerId; }
    public int getUdpPort()          { return udpPort; }
    public boolean isRunning()       { return running; }
}
