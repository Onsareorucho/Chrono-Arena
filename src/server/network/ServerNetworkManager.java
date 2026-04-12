package server.network;

import server.GameStateSnapshot;
import server.Item;
import server.Player;
import server.Zone;
import server.logic.ActionQueue;
import shared.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * ServerNetworkManager — single entry point for all networking on the server side.
 *
 * Wired into GameServer.java:
 *   ServerNetworkManager net = new ServerNetworkManager(config, actionQueue);
 *   net.onPlayerJoined = playerId -> gameState.addPlayer(new Player(playerId, ...));
 *   net.onPlayerLeft   = gameState::handlePlayerDisconnect;
 *   net.start();
 *
 * Called from GameLoop each tick:
 *   net.broadcastGameState(gameState.snapshot());
 *   net.broadcastScores(scoreMap);
 *
 * KILL_SWITCH (via KillSwitch.java):
 *   net.killPlayer(playerId, reason);
 */
public class ServerNetworkManager {

    private static final Logger LOG = Logger.getLogger(ServerNetworkManager.class.getName());

    private final TCPServerHandler tcp;
    private final UDPServerHandler udp;

    // ── Callbacks ──────────────────────────────────────────────

    /** Wire to: playerId -> gameState.addPlayer(new Player(playerId, ...)) */
    public java.util.function.BiConsumer<Integer, String> onPlayerJoined = (id, name) -> {};

    /** Wire to: gameState::handlePlayerDisconnect */
    public Consumer<Integer> onPlayerLeft = id -> {};

    // ──────────────────────────────────────────────────────────

    public ServerNetworkManager(GameConfig config, ActionQueue actionQueue) {
        this.tcp = new TCPServerHandler(config.getServerTcpPort(), config.getServerUdpPort());
        this.udp = new UDPServerHandler(config.getServerUdpPort(), actionQueue);
    }

    public void start() throws IOException {
        tcp.onPlayerJoined = (id, name) -> onPlayerJoined.accept(id, name);
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

    /** Broadcast full game state snapshot to all clients every tick. */
    public void broadcastGameState(GameStateSnapshot snapshot) {
        tcp.broadcast(new GameMessage(MessageType.GAME_STATE_UPDATE, toUpdate(snapshot)));
    }

    /**
     * Converts the server-internal GameStateSnapshot into the shared GameStateUpdate
     * that the client networking layer expects.
     */
    private static GameStateUpdate toUpdate(GameStateSnapshot snap) {
        List<GameStateUpdate.PlayerSnapshot> players = new java.util.ArrayList<>();
        if (snap.players != null) {
            for (Player p : snap.players) {
                players.add(new GameStateUpdate.PlayerSnapshot(
                    p.getPlayerId(),
                    p.getPlayerName(),
                    (float) p.getPlayerPositionX(),
                    (float) p.getPlayerPositionY(),
                    p.getPlayerScore(),
                    p.isFrozen(),
                    p.isHasSpeedBoost(),
                    p.isArmed()          // isArmed == hasFreezeRay
                ));
            }
        }

        List<GameStateUpdate.ZoneSnapshot> zones = new java.util.ArrayList<>();
        if (snap.zones != null) {
            for (int i = 0; i < snap.zones.size(); i++) {
                Zone z = snap.zones.get(i);
                int totalTicks = (int) Math.max(1, GameConstants.ZONE_CAPTURE_TIME_MS / 50);
                float captureProgress = (z.getCaptureTicksLeft() == 0) ? 1f
                    : 1f - (float) z.getCaptureTicksLeft() / totalTicks;
                zones.add(new GameStateUpdate.ZoneSnapshot(
                    i,                                   // int id (index; renderer just needs unique)
                    (float) z.getZonePositionX(),
                    (float) z.getZonePositionY(),
                    (float) z.getZoneWidth(),            // width used as size hint ("radius")
                    z.getControllingPlayerId(),
                    z.getContestingPlayerId(),
                    captureProgress
                ));
            }
        }

        List<GameStateUpdate.ItemSnapshot> items = new java.util.ArrayList<>();
        if (snap.items != null) {
            for (int i = 0; i < snap.items.size(); i++) {
                Item item = snap.items.get(i);
                if (!item.isAvailable()) continue;
                items.add(new GameStateUpdate.ItemSnapshot(
                    i,
                    item.getItemType(),
                    (float) item.getItemPositionX(),
                    (float) item.getItemPositionY()
                ));
            }
        }

        return new GameStateUpdate(players, zones, items,
                                   snap.timeRemainingMs, (int)(snap.tickNumber & Integer.MAX_VALUE));
    }

    /** Broadcast score map to all clients. */
    public void broadcastScores(Map<Integer, Integer> scores) {
        tcp.broadcast(new GameMessage(MessageType.SCORE_UPDATE, (Serializable) scores));
    }

    /** Broadcast a game event (zone captured, item picked up, freeze, etc.) */
    public void broadcastEvent(GameEvent event) {
        tcp.broadcast(new GameMessage(MessageType.GAME_EVENT, event));
    }

    /** Broadcast game over with final results. */
    public void broadcastGameOver(GameResult result) {
        tcp.broadcast(new GameMessage(MessageType.GAME_END, result));
    }

    /** Send a message to one specific player only. */
    public void sendTo(int playerId, GameMessage message) {
        tcp.sendTo(playerId, message);
    }

    // ── KILL_SWITCH ────────────────────────────────────────────

    /** Forcibly disconnect a misbehaving client. Called by KillSwitch.java. */
    public void killPlayer(int playerId, String reason) {
        tcp.kill(playerId, reason);
    }

    // ── Accessors ──────────────────────────────────────────────

    public Set<Integer> getConnectedPlayerIds() { return tcp.getConnectedPlayerIds(); }
    public int getPlayerCount()                 { return tcp.getPlayerCount(); }
}
