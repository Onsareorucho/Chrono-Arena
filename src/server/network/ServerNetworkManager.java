package server.network;

import server.util.ConfigLoader;
import shared.protocol.*;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * ServerNetworkManager is the single entry point for all networking on the server side.
 *
 * Person 1 (server core / GameServer.java) only needs this class.
 * It owns TCPServerHandler and UDPServerHandler and wires them together.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Typical usage in GameServer.java:
 *
 *   ConfigLoader config = new ConfigLoader("game.properties");
 *   ServerNetworkManager net = new ServerNetworkManager(config);
 *
 *   // Wire server-core callbacks
 *   net.onPlayerJoined  = playerId -> gameState.addPlayer(playerId);
 *   net.onPlayerLeft    = playerId -> gameState.removePlayer(playerId);
 *   net.onActionReceived = action  -> actionQueue.add(action);
 *
 *   net.start();
 *
 *   // Inside GameLoop every tick:
 *   net.broadcastGameState(tickNumber, gameStateJsonObject);
 *   net.broadcastScores(scoreMap);
 *
 *   // KILL_SWITCH (admin console or erratic-client detector):
 *   net.killPlayer(playerId, "Too many invalid packets");
 *
 *   // End of round:
 *   net.broadcastGameOver(finalScores, winnerPlayerId);
 *
 *   net.stop();
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Threading notes:
 *   - onPlayerJoined / onPlayerLeft are called on TCP reader threads.
 *   - onActionReceived is called on UDP worker threads (4 of them).
 *   - broadcast*() methods are safe to call from the game loop thread.
 *   - All internal data structures are thread-safe (ConcurrentHashMap, etc.)
 */
public class ServerNetworkManager {

    private static final Logger LOG = Logger.getLogger(ServerNetworkManager.class.getName());

    private final TCPServerHandler tcp;
    private final UDPServerHandler udp;

    // ── Callbacks — set these before calling start() ───────────

    /** Called when a new player successfully joins. Fired on a TCP reader thread. */
    public Consumer<Integer> onPlayerJoined   = id -> {};

    /** Called when a player disconnects or is evicted. Fired on reader or monitor thread. */
    public Consumer<Integer> onPlayerLeft     = id -> {};

    /**
     * Called for every valid, non-duplicate UDP ActionMessage.
     * Wire this directly to your ActionQueue:
     *   net.onActionReceived = actionQueue::add;
     */
    public Consumer<ActionMessage> onActionReceived = action -> {};

    // ──────────────────────────────────────────────────────────
    // Construction
    // ──────────────────────────────────────────────────────────

    public ServerNetworkManager(ConfigLoader config) {
        this.tcp = new TCPServerHandler(config.getTcpPort(), config.getUdpPort());
        this.udp = new UDPServerHandler(config.getUdpPort());
    }

    /** For testing: inject pre-configured handlers directly. */
    public ServerNetworkManager(TCPServerHandler tcp, UDPServerHandler udp) {
        this.tcp = tcp;
        this.udp = udp;
    }

    // ──────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────

    /**
     * Bind ports and start all networking threads.
     * Call once during server startup, after setting all callbacks.
     */
    public void start() throws IOException {
        // Wire callbacks from internal handlers to our public fields
        tcp.onPlayerJoined   = id  -> onPlayerJoined.accept(id);
        tcp.onPlayerLeft     = id  -> {
            onPlayerLeft.accept(id);
            udp.removePlayer(id); // clean up UDP sequence state
        };

        udp.onActionReceived = action -> onActionReceived.accept(action);

        udp.start();
        tcp.start(); // TCP last so UDP is ready before any client can join

        LOG.info("ServerNetworkManager started.");
    }

    public void stop() {
        tcp.stop();
        udp.stop();
        LOG.info("ServerNetworkManager stopped.");
    }

    // ──────────────────────────────────────────────────────────
    // Broadcasting (called from game loop — all thread-safe)
    // ──────────────────────────────────────────────────────────

    /**
     * Send the full game state to every connected client.
     * Call once per game tick from GameLoop.
     *
     * @param tickNumber  monotonically increasing tick counter
     * @param gameState   Gson JsonObject of the serialized GameState
     */
    public void broadcastGameState(long tickNumber, com.google.gson.JsonObject gameState) {
        tcp.broadcast(new GameStateUpdate(tickNumber, gameState));
    }

    /**
     * Send a lightweight score-only update to all clients.
     * Call whenever any score changes (typically every tick).
     *
     * @param scores map of playerId → current score
     */
    public void broadcastScores(java.util.Map<Integer, Integer> scores) {
        tcp.broadcast(new ScoreUpdate(scores));
    }

    /**
     * Notify all clients of a player lifecycle event.
     * Examples: FROZEN after freeze-ray hit, JOINED when someone connects.
     */
    public void broadcastPlayerEvent(PlayerEvent.EventType type, int playerId, String details) {
        tcp.broadcast(new PlayerEvent(type, playerId, details));
    }

    /**
     * End the round: send final scores and winner to all clients.
     */
    public void broadcastGameOver(java.util.Map<Integer, Integer> finalScores, int winnerPlayerId) {
        tcp.broadcast(new GameOver(finalScores, winnerPlayerId));
    }

    /**
     * Send a message to one specific player only.
     * Useful for sending personalised state (e.g. initial game state on join).
     */
    public void sendTo(int playerId, Message message) {
        tcp.sendTo(playerId, message);
    }

    // ──────────────────────────────────────────────────────────
    // KILL_SWITCH
    // ──────────────────────────────────────────────────────────

    /**
     * Forcibly disconnect a misbehaving client.
     * Sends them a KillClient message explaining why, then closes the socket.
     * The onPlayerLeft callback will fire after eviction.
     *
     * This is the KILL_SWITCH feature required by the spec.
     */
    public void killPlayer(int playerId, String reason) {
        tcp.kill(playerId, reason);
    }

    // ──────────────────────────────────────────────────────────
    // Accessors
    // ──────────────────────────────────────────────────────────

    public Set<Integer> getConnectedPlayerIds() {
        return tcp.getConnectedPlayerIds();
    }

    public int getPlayerCount() {
        return tcp.getPlayerCount();
    }
}
