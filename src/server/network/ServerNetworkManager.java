package server.network;

import server.logic.ActionQueue;
import shared.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * ServerNetworkManager — single entry point for networking on the server side.
 *
 * Moses hooks this into GameServer.java where the TODO comments are:
 *
 *   ServerNetworkManager net = new ServerNetworkManager(config, actionQueue);
 *   net.onPlayerJoined = gameState::addPlayer;
 *   net.onPlayerLeft   = gameState::removePlayer;
 *   net.start();
 *
 * Then in GameLoop each tick:
 *   net.broadcastGameState(snapshot);
 *   net.broadcastScores(scoreMap);
 *
 * KILL_SWITCH (via Moses's KillSwitch class or directly):
 *   net.killPlayer(playerId, "Too many invalid packets");
 */
public class ServerNetworkManager {

    private static final Logger LOG = Logger.getLogger(ServerNetworkManager.class.getName());

    private final TCPServerHandler tcp;
    private final UDPServerHandler udp;

    // ── Callbacks ──────────────────────────────────────────────

    /** Wire to GameState.addPlayer() */
    public Consumer<Integer> onPlayerJoined = id -> {};

    /** Wire to GameState.removePlayer() */
    public Consumer<Integer> onPlayerLeft = id -> {};

    // ──────────────────────────────────────────────────────────

    public ServerNetworkManager(GameConfig config, ActionQueue actionQueue) {
        this.tcp = new TCPServerHandler(config.getServerTcpPort(), config.getServerUdpPort());
        this.udp = new UDPServerHandler(config.getServerUdpPort(), actionQueue);
    }

    public void start() throws IOException {
        tcp.onPlayerJoined = id -> onPlayerJoined.accept(id);
        tcp.onPlayerLeft   = id -> {
            onPlayerLeft.accept(id);
            udp.removePlayer(id);
        };

        udp.start();
        tcp.start();
        LOG.info("ServerNetworkManager started.");
    }

    public void stop() {
        tcp.stop();
        udp.stop();
    }

    // ── Broadcasting ───────────────────────────────────────────

    /** Broadcast full game state to all clients every tick. */
    public void broadcastGameState(GameStateUpdate state) {
        tcp.broadcast(new GameMessage(MessageType.GAME_STATE_UPDATE, state));
    }

    /** Broadcast score update to all clients. */
    public void broadcastScores(java.io.Serializable scorePayload) {
        tcp.broadcast(new GameMessage(MessageType.SCORE_UPDATE, scorePayload));
    }

    /** Broadcast a game event (zone captured, item picked up, etc.) */
    public void broadcastEvent(GameEvent event) {
        tcp.broadcast(new GameMessage(MessageType.GAME_EVENT, event));
    }

    /** Broadcast game over with final results. */
    public void broadcastGameOver(GameResult result) {
        tcp.broadcast(new GameMessage(MessageType.GAME_END, result));
    }

    /** Send to one specific player only. */
    public void sendTo(int playerId, GameMessage message) {
        tcp.sendTo(playerId, message);
    }

    // ── KILL_SWITCH ────────────────────────────────────────────

    /** Forcibly disconnect a misbehaving client. */
    public void killPlayer(int playerId, String reason) {
        tcp.kill(playerId, reason);
    }

    // ── Accessors ──────────────────────────────────────────────

    public Set<Integer> getConnectedPlayerIds() { return tcp.getConnectedPlayerIds(); }
    public int getPlayerCount()                 { return tcp.getPlayerCount(); }
}
