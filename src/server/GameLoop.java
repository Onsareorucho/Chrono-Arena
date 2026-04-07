package server;

import server.logic.ActionQueue;
import server.logic.CollisionHandler;
import server.logic.CombatHandler;
import server.network.ServerNetworkManager;
import shared.GameMessage;
import shared.GameResult;
import shared.MessageType;
import shared.PlayerAction;
import shared.PlayerInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameLoop {

    private final GameState gameState;
    private final ActionQueue actionQueue;
    private final CollisionHandler collisionHandler;
    private final ItemSpawner itemSpawner;
    private final ScheduledExecutorService scheduler;
    private final long tickRateMs;
    private final List<GameMessage> tickActions = new ArrayList<>();
    private final CombatHandler combatHandler;
    private final ServerNetworkManager networkManager; // P2

    public GameLoop(GameState gameState, ActionQueue actionQueue,
                    CollisionHandler collisionHandler, ItemSpawner itemSpawner,
                    CombatHandler combatHandler, long tickRateMs,
                    ServerNetworkManager networkManager) {
        this.gameState        = gameState;
        this.actionQueue      = actionQueue;
        this.collisionHandler = collisionHandler;
        this.itemSpawner      = itemSpawner;
        this.combatHandler    = combatHandler;
        this.tickRateMs       = tickRateMs;
        this.networkManager   = networkManager;
        this.scheduler        = Executors.newSingleThreadScheduledExecutor();
    }

    // lightweight constructor for testing — no scheduler, no network
    public GameLoop(GameState gameState) {
        this.gameState        = gameState;
        this.actionQueue      = null;
        this.collisionHandler = null;
        this.itemSpawner      = null;
        this.combatHandler    = null;
        this.tickRateMs       = 0;
        this.networkManager   = null;
        this.scheduler        = null;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::tick,
                0,
                tickRateMs,
                TimeUnit.MILLISECONDS);
        System.out.println("Game loop started — tick rate: " + tickRateMs + "ms");
    }

    public void stop() {
        scheduler.shutdown();
        System.out.println("Game loop stopped");
    }

    private void tick() {
        try {
            // stop loop if game is over
            if (gameState.getPhase() == GameState.GamePhase.FINISHED) {
                stop();
                return;
            }

            // ── 1. Drain action queue ────────────────────────────
            tickActions.clear();
            actionQueue.drainTo(tickActions);

            // ── 2. Process actions ───────────────────────────────
            for (GameMessage action : tickActions) {
                processAction(action);
            }

            // ── 3. Run collision + item spawner ──────────────────
            collisionHandler.update();
            itemSpawner.update();

            // ── 4. Update zone timers ────────────────────────────
            updateZones();

            // ── 5. Update frozen player timers ───────────────────
            updateFrozenPlayers();

            // ── 6. Tick game state ───────────────────────────────
            gameState.tick(tickRateMs);

            // ── 7. Broadcast state to all clients (P2) ───────────
            if (networkManager != null) {
                networkManager.broadcastGameState(gameState.snapshot());

                // Build score map and broadcast
                Map<Integer, Player> players = gameState.getPlayers();
                java.util.HashMap<Integer, Integer> scores = new java.util.HashMap<>();
                for (Map.Entry<Integer, Player> e : players.entrySet()) {
                    scores.put(e.getKey(), e.getValue().getPlayerScore());
                }
                networkManager.broadcastScores(scores);
            }

            // ── 8. Check for game over ───────────────────────────
            if (gameState.getPhase() == GameState.GamePhase.FINISHED) {
                Player winner = gameState.getWinner();
                if (winner != null) {
                    System.out.println("GAME OVER — Winner: " + winner.getPlayerName()
                            + " with " + winner.getPlayerScore() + " points");
                }

                // Broadcast GAME_OVER to all clients (P2)
                if (networkManager != null) {
                    // Build leaderboard
                    List<GameResult.PlayerScore> leaderboard = new ArrayList<>();
                    for (Player p : gameState.getPlayers().values()) {
                        leaderboard.add(new GameResult.PlayerScore(
                            p.getPlayerId(), p.getPlayerName(), p.getPlayerScore(),
                            0, 0, 0, 0
                        ));
                    }
                    // Sort by score descending
                    leaderboard.sort((a, b) -> b.totalScore - a.totalScore);

                    int winnerId   = winner != null ? winner.getPlayerId() : -1;
                    String winnerName = winner != null ? winner.getPlayerName() : "None";

                    GameResult result = new GameResult(leaderboard, winnerId,
                                                       winnerName, gameState.getTimeRemainingMs());
                    networkManager.broadcastGameOver(result);
                }

                stop();
                return;
            }

        } catch (Exception e) {
            System.err.println("Error during tick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processAction(GameMessage message) {
        Player player = gameState.getPlayer(message.getSenderId());
        if (player == null) return;

        switch (message.getType()) {
            case PLAYER_INPUT -> {
                if (player.isFrozen()) break;
                PlayerInput input = message.getPayloadAs(PlayerInput.class);
                int newX = Math.max(0, Math.min(19, player.getPlayerPositionX() + input.getDirectionX()));
                int newY = Math.max(0, Math.min(19, player.getPlayerPositionY() + input.getDirectionY()));
                player.setPlayerPositionX(newX);
                player.setPlayerPositionY(newY);
            }
            case PLAYER_ACTION -> {
                if (player.isFrozen()) break;
                PlayerAction action = message.getPayloadAs(PlayerAction.class);
                // action handled by CombatHandler when freeze ray is fired directly
            }
            default -> System.out.println("Unhandled message type: " + message.getType());
        }
    }

    public void updateZones() {
        for (Zone zone : gameState.getZones()) {
            switch (zone.getZoneState()) {

                case CAPTURING -> {
                    int ticks = Math.max(0, zone.getCaptureTicksLeft() - 1);
                    zone.setCaptureTicksLeft(ticks);
                    if (ticks <= 0) {
                        zone.setZoneState(ZoneState.CONTROLLED);
                        System.out.println("Zone " + zone.getZoneId() + " captured by " + zone.getContestingPlayerId());
                        zone.setControllingPlayerId(zone.getContestingPlayerId());
                        zone.setContestingPlayerId(-1);
                    }
                }

                case GRACE -> {
                    int ticks = Math.max(0, zone.getGraceTicksLeft() - 1);
                    zone.setGraceTicksLeft(ticks);
                    if (ticks <= 0) {
                        zone.setZoneState(ZoneState.UNCLAIMED);
                        zone.setControllingPlayerId(-1);
                        System.out.println("Zone " + zone.getZoneId() + " lost — grace period expired");
                    }
                }

                case CONTROLLED -> {
                    int ownerId = zone.getControllingPlayerId();
                    if (ownerId != -1) {
                        Player owner = gameState.getPlayer(ownerId);
                        if (owner != null) {
                            owner.setPlayerScore(owner.getPlayerScore() + 1);
                        }
                    }
                }

                case CONTESTED, UNCLAIMED -> {}
            }
        }
    }

    public void updateFrozenPlayers() {
        for (Player player : gameState.getPlayers().values()) {
            if (player.isFrozen()) {
                player.setFrozenTicksLeft(Math.max(0, player.getFrozenTicksLeft() - 1));
            }
        }
    }
}
