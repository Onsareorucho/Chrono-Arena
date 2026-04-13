package server.network;

import shared.*;
import server.logic.ActionQueue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCPServerHandler manages all TCP connections for ChronoArena.
 *
 * Integrates with Moses's GameServer via:
 *   - onPlayerJoined callback → GameState.addPlayer()
 *   - onPlayerLeft callback  → GameState.removePlayer()
 *   - broadcast(GameMessage) → called by GameLoop each tick
 *
 * Uses Andrew's GameMessage + MessageSerializer for all serialization.
 *
 * Threading model:
 *   - 1 acceptor thread (blocks on ServerSocket.accept)
 *   - 1 reader thread per connected client
 *   - 1 heartbeat monitor thread
 *   - broadcast() is safe to call from the game loop thread
 */
public class TCPServerHandler {

    private static final Logger LOG = Logger.getLogger(TCPServerHandler.class.getName());

    private final int tcpPort;
    private final int udpPort;
    private int minPlayers = 2;

    private final ConcurrentHashMap<Integer, ClientConnection> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);

    private final ExecutorService clientPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "tcp-client-reader");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = false;
    private ServerSocket serverSocket;

    // ── Callbacks — set these before calling start() ───────────

    /** Called when a new player successfully joins. */
    public BiConsumer<Integer, String> onPlayerJoined = (id, name) -> {};

    /** Called when a player disconnects or is evicted. */
    public Consumer<Integer> onPlayerLeft = id -> {};

    // ──────────────────────────────────────────────────────────

    public TCPServerHandler(int tcpPort, int udpPort) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(tcpPort);
        running = true;
        LOG.info("TCP server listening on port " + tcpPort);

        Thread acceptor = new Thread(this::acceptLoop, "tcp-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();

        Thread monitor = new Thread(this::heartbeatMonitorLoop, "tcp-heartbeat-monitor");
        monitor.setDaemon(true);
        monitor.start();
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        // Notify all clients server is shutting down
        broadcast(new GameMessage(MessageType.KICK, KickNotification.serverShuttingDown()));
        clients.values().forEach(ClientConnection::close);
        clientPool.shutdownNow();
    }

    // ── Accept loop ────────────────────────────────────────────

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                int playerId = nextPlayerId.getAndIncrement();
                ClientConnection conn = new ClientConnection(playerId, socket);
                LOG.info("New TCP connection from " + conn.getRemoteAddress()
                         + " → playerId=" + playerId);
                clientPool.submit(() -> handleClient(conn));
            } catch (IOException e) {
                if (running) LOG.log(Level.WARNING, "Accept error: " + e.getMessage());
            }
        }
    }

    // ── Per-client reader ──────────────────────────────────────

    private void handleClient(ClientConnection conn) {
        int playerId = conn.getPlayerId();

        // First message must be JOIN_REQUEST
        GameMessage first = conn.receive();
        if (first == null || first.getType() != MessageType.JOIN_REQUEST) {
            LOG.warning("Player " + playerId + " did not send JOIN_REQUEST — dropping");
            conn.close();
            return;
        }

        JoinRequest jr = first.getPayloadAs(JoinRequest.class);
        LOG.info("Player " + playerId + " joining as '" + jr.getPlayerName() + "'");

        // Register client
        clients.put(playerId, conn);

        // Send JOIN_RESPONSE with player ID and UDP port
        JoinResponse response = new JoinResponse(
            playerId, 5.0f, 5.0f,          // start position
            20, 20,                          // map size (overridden by config in real game)
            180000L,                         // time remaining ms
            udpPort, minPlayers, clients.size()
        );
        conn.send(new GameMessage(MessageType.JOIN_RESPONSE, response));
        onPlayerJoined.accept(playerId, jr.getPlayerName());

        // Notify all others
        broadcast(new GameMessage(MessageType.PLAYER_JOINED,
                                  (java.io.Serializable) Integer.valueOf(playerId)));

        // Read loop
        while (conn.isAlive()) {
            GameMessage msg = conn.receive();
            if (msg == null) break;

            switch (msg.getType()) {
                case HEARTBEAT -> {
                    conn.send(new GameMessage(MessageType.HEARTBEAT, null));
                    conn.touch();
                }
                // All other messages (LEAVE_REQUEST etc.) handled here
                case LEAVE_REQUEST -> {
                    LOG.info("Player " + playerId + " leaving gracefully");
                    conn.send(new GameMessage(MessageType.LEAVE_RESPONSE, null));
                    break;
                }
                default -> LOG.fine("Received " + msg.getType() + " from player " + playerId);
            }
        }

        evictPlayer(playerId, "disconnected");
    }

    // ── Heartbeat monitor ──────────────────────────────────────

    private void heartbeatMonitorLoop() {
        while (running) {
            try { Thread.sleep(GameConstants.CLIENT_TIMEOUT_MS / 5); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }

            long now = System.currentTimeMillis();
            for (ClientConnection conn : clients.values()) {
                long elapsed = now - conn.getLastSeenMs();
                if (elapsed > GameConstants.CLIENT_TIMEOUT_MS / 5) {
                    conn.incrementMissedHeartbeats();
                    if (conn.getMissedHeartbeats() >= 5) {
                        LOG.warning("Player " + conn.getPlayerId() + " timed out");
                        evictPlayer(conn.getPlayerId(), "heartbeat timeout");
                    }
                }
            }
        }
    }

    // ── Broadcasting ───────────────────────────────────────────

    /**
     * Send a GameMessage to all connected clients.
     * Called by GameLoop each tick via ServerNetworkManager.
     */
    public void broadcast(GameMessage message) {
        clients.values().forEach(conn -> conn.send(message));
    }

    /** Send to one specific player. */
    public void sendTo(int playerId, GameMessage message) {
        ClientConnection conn = clients.get(playerId);
        if (conn != null) conn.send(message);
    }

    // ── KILL_SWITCH ────────────────────────────────────────────

    /**
     * Forcibly disconnect a misbehaving client.
     * Integrates with Moses's KillSwitch class.
     */
    public void kill(int playerId, String reason) {
        ClientConnection conn = clients.get(playerId);
        if (conn == null) return;
        LOG.info("KILL_SWITCH: evicting player " + playerId + " — " + reason);
        conn.send(new GameMessage(MessageType.KICK, KickNotification.adminKick(reason)));
        evictPlayer(playerId, "killed: " + reason);
    }

    private void evictPlayer(int playerId, String reason) {
        ClientConnection conn = clients.remove(playerId);
        if (conn == null) return;
        conn.close();
        onPlayerLeft.accept(playerId);
        broadcast(new GameMessage(MessageType.PLAYER_LEFT,
                                  (java.io.Serializable) Integer.valueOf(playerId)));
        LOG.info("Player " + playerId + " evicted: " + reason);
    }

    // ── Accessors ──────────────────────────────────────────────

    public Set<Integer> getConnectedPlayerIds() { return Collections.unmodifiableSet(clients.keySet()); }
    public int getPlayerCount()                 { return clients.size(); }
}
