package server;

import server.logic.ActionQueue;
import server.logic.CollisionHandler;
import server.logic.CombatHandler;
import server.logic.ZoneCaptureHandler;
import server.util.KillSwitch;
import shared.GameConfig;
import java.io.IOException;

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
        GameConfig config;
        try {
            config = new GameConfig();
        } catch (IOException e) {
            throw new RuntimeException("Could not load game.properties", e);
        }

        int mapWidth        = config.getMapWidth();
        int mapHeight       = config.getMapHeight();
        long gameDurationMs = config.getGameDurationSeconds() * 1000L;
        long tickRateMs     = config.getTickRateMs();

        // ── Initialize game state ────────────────────────────
        gameState = new GameState(gameDurationMs);

        // ── Set up zones ─────────────────────────────────────
        gameState.addZone(new Zone("A", 2,  2,  4, 4));
        gameState.addZone(new Zone("B", 14, 2,  4, 4));
        gameState.addZone(new Zone("C", 8,  10, 4, 4));

        // ── Initialize core components ───────────────────────
        actionQueue        = new ActionQueue();
        zoneCaptureHandler = new ZoneCaptureHandler();
        collisionHandler   = new CollisionHandler(gameState, zoneCaptureHandler);
        combatHandler      = new CombatHandler(collisionHandler);

        itemSpawner       = new ItemSpawner(gameState, mapWidth, mapHeight);
        killSwitch        = new KillSwitch(gameState);

        // ── Initialize game loop ─────────────────────────────
        gameLoop = new GameLoop(gameState, actionQueue, collisionHandler, itemSpawner, tickRateMs);

        // ── Start networking ─────────────────────────────────
        // TODO: P2 hooks in here
        // tcpServerHandler = new TCPServerHandler(gameState, actionQueue, config);
        // udpServerHandler = new UDPServerHandler(actionQueue, config);
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

    // expose for P2 / ActionProcessor
    public GameState getGameState()          { return gameState; }
    public ActionQueue getActionQueue()      { return actionQueue; }
    public KillSwitch getKillSwitch()        { return killSwitch; }
    public CombatHandler getCombatHandler()  { return combatHandler; }
}
