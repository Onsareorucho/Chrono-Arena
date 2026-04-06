package server.network;

import shared.GameConstants;
import shared.protocol.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCPServerHandler manages all TCP connections for ChronoArena.
 *
 * Responsibilities:
 *  - Accept incoming client connections in a dedicated acceptor thread
 *  - Spawn one reader thread per connected client
 *  - Process JoinRequest / Heartbeat messages
 *  - Broadcast GameStateUpdate, ScoreUpdate, PlayerEvent to all clients
 *  - Detect and evict dead/erratic clients (heartbeat monitor)
 *  - Expose kill(playerId) for the KILL_SWITCH feature
 *
 * Threading model:
 *  - 1 acceptor thread (blocks on ServerSocket.accept)
 *  - 1 reader thread per client (blocks on ClientConnection.receive)
 *  - 1 heartbeat monitor thread
 *  - Broadcast calls may come from the game loop thread — all client maps
 *    use ConcurrentHashMap so no extra locking is needed for reads.
 *
 * Integration points:
 *  - Set onPlayerJoined / onPlayerLeft / onMessageReceived callbacks
 *    so the server core (Person 1) can react without polling.
 */
public class TCPServerHandler {

    private static final Logger LOG = Logger.getLogger(TCPServerHandler.class.getName());

    private final int tcpPort;
    private final int udpPort; // included in JoinResponse so client knows where to send UDP

    private final ConcurrentHashMap<Integer, ClientConnection> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);

    private final ExecutorService clientPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("tcp-client-reader");
        return t;
    });

    private volatile boolean running = false;
    private ServerSocket serverSocket;

    // ──────────────────────────────────────────────────────────
    // Callbacks — set these before calling start()
    // ──────────────────────────────────────────────────────────

    /** Called (on the reader thread) when a new player has joined. */
    public Consumer<Integer> onPlayerJoined = id -> {};

    /** Called (on reader or monitor thread) when a player disconnects. */
    public Consumer<Integer> onPlayerLeft = id -> {};

    /**
     * Called (on the reader thread) for every message received from a client
     * EXCEPT JoinRequest and Heartbeat, which are handled internally.
     */
    public Consumer<Message> onMessageReceived = msg -> {};

    // ──────────────────────────────────────────────────────────
    // Construction & lifecycle
    // ──────────────────────────────────────────────────────────

    public TCPServerHandler(int tcpPort, int udpPort) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
    }

    /** Bind the server socket and start the acceptor and monitor threads. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(tcpPort);
        running = true;
        LOG.info("TCP server listening on port " + tcpPort);

        // Acceptor thread
        Thread acceptor = new Thread(this::acceptLoop, "tcp-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();

        // Heartbeat monitor thread
        Thread monitor = new Thread(this::heartbeatMonitorLoop, "tcp-heartbeat-monitor");
        monitor.setDaemon(true);
        monitor.start();
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        broadcastRaw(new GameOver(Map.of(), -1)); // notify clients
        clients.values().forEach(ClientConnection::close);
        clientPool.shutdownNow();
    }

    // ──────────────────────────────────────────────────────────
    // Accept loop
    // ──────────────────────────────────────────────────────────

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true); // reduce latency for small packets
                socket.setSoTimeout(GameConstants.HEARTBEAT_INTERVAL_MS
                                    * (GameConstants.MAX_MISSED_HEARTBEATS + 1));

                int playerId = nextPlayerId.getAndIncrement();
                ClientConnection conn = new ClientConnection(playerId, socket);

                LOG.info("New TCP connection from " + conn.getRemoteAddress()
                         + " → assigned playerId=" + playerId);

                // Spawn reader thread for this client
                clientPool.submit(() -> handleClient(conn));

            } catch (IOException e) {
                if (running) LOG.log(Level.WARNING, "Accept error: " + e.getMessage());
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Per-client reader
    // ──────────────────────────────────────────────────────────

    private void handleClient(ClientConnection conn) {
        int playerId = conn.getPlayerId();

        // First message must be a JoinRequest
        Message first = conn.receive();
        if (first == null || first.type != Message.MessageType.JOIN_REQUEST) {
            LOG.warning("Player " + playerId + " did not send JoinRequest — dropping");
            conn.close();
            return;
        }

        JoinRequest jr = (JoinRequest) first;
        LOG.info("Player " + playerId + " joining as '" + jr.playerName + "'");

        // Register and acknowledge
        clients.put(playerId, conn);
        conn.send(JoinResponse.accept(playerId, udpPort));
        onPlayerJoined.accept(playerId);

        // Notify all others
        broadcast(new PlayerEvent(PlayerEvent.EventType.JOINED, playerId,
                                  jr.playerName + " joined"));

        // Read loop
        while (conn.isAlive()) {
            Message msg = conn.receive();
            if (msg == null) break;

            switch (msg.type) {
                case HEARTBEAT -> {
                    Heartbeat hb = (Heartbeat) msg;
                    conn.send(new HeartbeatAck(hb.sentAt));
                    conn.touch();
                }
                // All other messages forwarded to the server core
                default -> onMessageReceived.accept(msg);
            }
        }

        // Cleanup
        evictPlayer(playerId, "disconnected");
    }

    // ──────────────────────────────────────────────────────────
    // Heartbeat monitor
    // ──────────────────────────────────────────────────────────

    private void heartbeatMonitorLoop() {
        while (running) {
            try { Thread.sleep(GameConstants.HEARTBEAT_INTERVAL_MS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }

            long now = System.currentTimeMillis();
            for (ClientConnection conn : clients.values()) {
                long elapsed = now - conn.getLastSeenMs();
                if (elapsed > GameConstants.HEARTBEAT_INTERVAL_MS) {
                    conn.incrementMissedHeartbeats();
                    if (conn.getMissedHeartbeats() >= GameConstants.MAX_MISSED_HEARTBEATS) {
                        LOG.warning("Player " + conn.getPlayerId()
                                    + " timed out after " + conn.getMissedHeartbeats()
                                    + " missed heartbeats");
                        evictPlayer(conn.getPlayerId(), "heartbeat timeout");
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Broadcasting
    // ──────────────────────────────────────────────────────────

    /**
     * Send a message to all connected clients.
     * Called from the game loop thread — safe because ConcurrentHashMap
     * iteration is weakly consistent and ClientConnection.send() is synchronized.
     */
    public void broadcast(Message message) {
        broadcastRaw(message);
    }

    private void broadcastRaw(Message message) {
        clients.values().forEach(conn -> conn.send(message));
    }

    /**
     * Send a message to one specific player.
     */
    public void sendTo(int playerId, Message message) {
        ClientConnection conn = clients.get(playerId);
        if (conn != null) conn.send(message);
    }

    // ──────────────────────────────────────────────────────────
    // KILL_SWITCH
    // ──────────────────────────────────────────────────────────

    /**
     * Forcibly disconnect a client.
     * Sends a KillClient notice first so the client can display an error,
     * then closes the socket.
     */
    public void kill(int playerId, String reason) {
        ClientConnection conn = clients.get(playerId);
        if (conn == null) {
            LOG.warning("kill() called for unknown player " + playerId);
            return;
        }
        LOG.info("KILL_SWITCH: evicting player " + playerId + " — reason: " + reason);
        conn.send(new KillClient(playerId, reason));
        evictPlayer(playerId, "killed: " + reason);
    }

    private void evictPlayer(int playerId, String reason) {
        ClientConnection conn = clients.remove(playerId);
        if (conn == null) return; // already removed by another thread

        conn.close();
        onPlayerLeft.accept(playerId);
        broadcast(new PlayerEvent(PlayerEvent.EventType.LEFT, playerId, reason));
        LOG.info("Player " + playerId + " evicted: " + reason);
    }

    // ──────────────────────────────────────────────────────────
    // Accessors
    // ──────────────────────────────────────────────────────────

    public Set<Integer> getConnectedPlayerIds() {
        return Collections.unmodifiableSet(clients.keySet());
    }

    public int getPlayerCount() { return clients.size(); }
}
