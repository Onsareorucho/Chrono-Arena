package server;

import server.logic.ActionQueue;
import server.logic.CollisionHandler;
import server.logic.CombatHandler;
import server.logic.ZoneCaptureHandler;
import server.util.ConfigLoader;
import server.util.KillSwitch;

public class GameServer {

    private GameState gameState;
    private GameLoop gameLoop;
    private ActionQueue actionQueue;
    private ItemSpawner itemSpawner;
    private KillSwitch killSwitch;
    private CollisionHandler collisionHandler;
    private ZoneCaptureHandler zoneCaptureHandler;
    private CombatHandler combatHandler;

    public void start() {
        System.out.println("ChronoArena server starting...");

        // ── Load config ──────────────────────────────────────
        int mapWidth        = ConfigLoader.getInt("map.width");
        int mapHeight       = ConfigLoader.getInt("map.height");
        long gameDurationMs = ConfigLoader.getInt("game.duration.seconds") * 1000L;
        long tickRateMs     = ConfigLoader.getInt("game.tick.rate.ms");

        // ── Initialize game state ────────────────────────────
        gameState = new GameState(gameDurationMs);

        // ── Set up zones ─────────────────────────────────────
        gameState.addZone(new Zone("A", 2,  2,  4, 4));
        gameState.addZone(new Zone("B", 14, 2,  4, 4));
        gameState.addZone(new Zone("C", 8,  10, 4, 4));

        // ── Initialize core components ───────────────────────
        actionQueue       = new ActionQueue();
        zoneCaptureHandler = new ZoneCaptureHandler();
        combatHandler     = new CombatHandler(null); // CollisionHandler set below
        collisionHandler  = new CollisionHandler(gameState, zoneCaptureHandler, combatHandler);

        // wire CollisionHandler into CombatHandler
        combatHandler     = new CombatHandler(collisionHandler);

        itemSpawner       = new ItemSpawner(gameState, mapWidth, mapHeight);
        killSwitch        = new KillSwitch(gameState);

        // ── Initialize game loop ─────────────────────────────
        gameLoop = new GameLoop(gameState, actionQueue, tickRateMs);

        // ── Start networking ─────────────────────────────────
        // TODO: P2 hooks in here
        // tcpServerHandler = new TCPServerHandler(gameState, actionQueue);
        // udpServerHandler = new UDPServerHandler(actionQueue);
        // tcpServerHandler.start();
        // udpServerHandler.start();

        // ── Start game ───────────────────────────────────────
        gameState.setPhase(GameState.GamePhase.IN_PROGRESS);
        gameLoop.start();

        System.out.println("Server running — waiting for players");
    }

    public void stop() {
        gameLoop.stop();
        System.out.println("Server stopped");
    }

    // expose for P2 to access
    public GameState getGameState()   { return gameState; }
    public ActionQueue getActionQueue() { return actionQueue; }
    public KillSwitch getKillSwitch() { return killSwitch; }
}