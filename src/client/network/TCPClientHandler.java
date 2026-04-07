package client.network;

import shared.*;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCPClientHandler — client side of the TCP connection.
 * Uses Andrew's MessageSerializer + GameMessage system.
 */
public class TCPClientHandler {

    private static final Logger LOG = Logger.getLogger(TCPClientHandler.class.getName());

    private final String serverIp;
    private final int tcpPort;

    private Socket socket;
    private OutputStream out;
    private InputStream in;

    private volatile boolean running = false;
    private volatile int assignedPlayerId = -1;
    private volatile int serverUdpPort = -1;

    // ── Callbacks ──────────────────────────────────────────────

    public Consumer<GameStateUpdate>  onGameStateUpdate = u -> {};
    public Consumer<GameEvent>        onGameEvent       = e -> {};
    public Consumer<GameResult>       onGameOver        = r -> {};
    public Consumer<KickNotification> onKickReceived    = k -> {};
    public Consumer<Integer>          onPlayerJoined    = id -> {};
    public Consumer<Integer>          onPlayerLeft      = id -> {};
    public Runnable                   onDisconnected    = () -> {};

    // ──────────────────────────────────────────────────────────

    public TCPClientHandler(String serverIp, int tcpPort) {
        this.serverIp = serverIp;
        this.tcpPort  = tcpPort;
    }

    // ── Connection ─────────────────────────────────────────────

    public JoinResponse connect(String playerName) throws IOException {
        socket = new Socket(serverIp, tcpPort);
        socket.setTcpNoDelay(true);
        out = new BufferedOutputStream(socket.getOutputStream());
        in  = new BufferedInputStream(socket.getInputStream());

        LOG.info("Connected to " + serverIp + ":" + tcpPort);

        JoinRequest jr = new JoinRequest(playerName, 0);
        MessageSerializer.writeToStream(new GameMessage(MessageType.JOIN_REQUEST, jr), out);

        try {
            GameMessage response = MessageSerializer.readFromStream(in);
            if (response == null || response.getType() != MessageType.JOIN_RESPONSE) {
                throw new IOException("Did not receive JOIN_RESPONSE from server");
            }
            JoinResponse joinResp = response.getPayloadAs(JoinResponse.class);
            assignedPlayerId = joinResp.getPlayerId();
            serverUdpPort    = joinResp.getServerUdpPort();
            LOG.info("Joined as playerId=" + assignedPlayerId);
            return joinResp;
        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to deserialize JOIN_RESPONSE: " + e.getMessage(), e);
        }
    }

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

    // ── Reader loop ────────────────────────────────────────────

    private void readLoop() {
        while (running) {
            try {
                GameMessage msg = MessageSerializer.readFromStream(in);
                if (msg == null) break;
                dispatch(msg);
            } catch (IOException e) {
                if (running) LOG.log(Level.WARNING, "Read error: " + e.getMessage());
                break;
            } catch (ClassNotFoundException e) {
                if (running) LOG.log(Level.WARNING, "Deserialize error: " + e.getMessage());
                break;
            }
        }
        if (running) {
            running = false;
            onDisconnected.run();
        }
    }

    private void dispatch(GameMessage msg) {
        switch (msg.getType()) {
            case GAME_STATE_UPDATE -> onGameStateUpdate.accept(msg.getPayloadAs(GameStateUpdate.class));
            case GAME_EVENT        -> onGameEvent.accept(msg.getPayloadAs(GameEvent.class));
            case GAME_END          -> onGameOver.accept(msg.getPayloadAs(GameResult.class));
            case KICK              -> {
                onKickReceived.accept(msg.getPayloadAs(KickNotification.class));
                stop();
            }
            case PLAYER_JOINED -> onPlayerJoined.accept(msg.getPayloadAs(Integer.class));
            case PLAYER_LEFT   -> onPlayerLeft.accept(msg.getPayloadAs(Integer.class));
            case HEARTBEAT     -> {}
            default -> LOG.fine("Unhandled: " + msg.getType());
        }
    }

    // ── Heartbeat loop ─────────────────────────────────────────

    private void heartbeatLoop() {
        while (running) {
            try {
                Thread.sleep(GameConstants.CLIENT_TIMEOUT_MS / 5);
                if (running && assignedPlayerId != -1) {
                    send(new GameMessage(MessageType.HEARTBEAT, null));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ── Send ───────────────────────────────────────────────────

    public synchronized void send(GameMessage message) {
        try {
            MessageSerializer.writeToStream(message, out);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Send failed: " + e.getMessage());
            running = false;
        }
    }

    // ── Accessors ──────────────────────────────────────────────

    public int getAssignedPlayerId() { return assignedPlayerId; }
    public int getServerUdpPort()    { return serverUdpPort; }
    public boolean isRunning()       { return running; }
}
