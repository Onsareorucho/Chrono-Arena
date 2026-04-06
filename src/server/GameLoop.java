package server;

import server.logic.ActionQueue;
import server.logic.CollisionHandler;
import java.util.ArrayList;
import java.util.List;
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
    private final List<Object> tickActions = new ArrayList<>();

    public GameLoop(GameState gameState, ActionQueue actionQueue,
                    CollisionHandler collisionHandler, ItemSpawner itemSpawner,
                    long tickRateMs) {
        this.gameState = gameState;
        this.actionQueue = actionQueue;
        this.collisionHandler = collisionHandler;
        this.itemSpawner = itemSpawner;
        this.tickRateMs = tickRateMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
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
            for (Object action : tickActions) {
                processAction(action);
            }

            // ── 3. Run collision + item spawner ─────────────────
            collisionHandler.update();
            itemSpawner.update();

            // ── 4. Update zone timers ────────────────────────────
            updateZones();

            // ── 5. Update frozen player timers ───────────────────
            updateFrozenPlayers();

            // ── 6. Tick game state ───────────────────────────────
            gameState.tick(tickRateMs);

            // ── 7. Check for game over ───────────────────────────
            if (gameState.getPhase() == GameState.GamePhase.FINISHED) {
                Player winner = gameState.getWinner();
                if (winner != null) {
                    System.out.println("GAME OVER — Winner: " + winner.getPlayerName()
                            + " with " + winner.getPlayerScore() + " points");
                }
                // TODO: P2 broadcasts GAME_OVER message to all clients
                // tcpServerHandler.broadcastGameOver(winner);
                stop();
                return;
            }

        } catch (Exception e) {
            System.err.println("Error during tick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processAction(Object action) {
        // TODO: swap Object for ActionMessage when P4 delivers shared/
        // ActionProcessor will handle routing to the right handler
        // ActionProcessor.process(action, gameState);
        System.out.println("Processing action: " + action);
    }

    private void updateZones() {
        for (Zone zone : gameState.getZones()) {
            switch (zone.getZoneState()) {

                case CAPTURING -> {
                    // count down capture timer
                    int ticks = Math.max(0, zone.getCaptureTicksLeft() - 1);
                    zone.setCaptureTicksLeft(ticks);
                    if (ticks <= 0) {
                        // capture complete
                        zone.setZoneState(ZoneState.CONTROLLED);
                        System.out.println("Zone " + zone.getZoneId() + " captured by " + zone.getContestingPlayerId());
                        zone.setControllingPlayerId(zone.getContestingPlayerId());
                        zone.setContestingPlayerId(-1);
                    }
                }

                case GRACE -> {
                    // count down grace timer
                    int ticks = Math.max(0, zone.getGraceTicksLeft() - 1);
                    zone.setGraceTicksLeft(ticks);
                    if (ticks <= 0) {
                        // grace period expired — zone resets
                        zone.setZoneState(ZoneState.UNCLAIMED);
                        zone.setControllingPlayerId(-1);
                        System.out.println("Zone " + zone.getZoneId() + " lost — grace period expired");
                    }
                }

                case CONTROLLED -> {
                    // award points to controlling player every tick
                    int ownerId = zone.getControllingPlayerId();
                    if (ownerId != -1) {
                        Player owner = gameState.getPlayer(ownerId);
                        if (owner != null) {
                            owner.setPlayerScore(owner.getPlayerScore() + 1);
                        }
                    }
                }

                case CONTESTED, UNCLAIMED -> {
                    // nothing to count down
                }
            }
        }
    }

    private void updateFrozenPlayers() {
        for (Player player : gameState.getPlayers().values()) {
            if (player.isFrozen()) {
                player.setFrozenTicksLeft(Math.max(0, player.getFrozenTicksLeft() - 1));
            }
        }
    }
}