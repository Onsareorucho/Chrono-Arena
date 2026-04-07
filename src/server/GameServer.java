package server;

import server.logic.ActionQueue;
import server.logic.CollisionHandler;
import server.logic.CombatHandler;
import server.logic.ZoneCaptureHandler;
import server.network.ServerNetworkManager;
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
    private ServerNetworkManager networkManager;  // P2

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
        gameState = new GameState(gameDurationMs, tickRateMs);

        // ── Set up zones ─────────────────────────────────────
        gameState.addZone(new Zone("A", 2,  2,  4, 4));
        gameState.addZone(new Zone("B", 14, 2,  4, 4));
        gameState.addZone(new Zone("C", 8,  10, 4, 4));

        // ── Initialize core components ───────────────────────
        actionQueue        = new ActionQueue();
        zoneCaptureHandler = new ZoneCaptureHandler(tickRateMs);
        collisionHandler   = new CollisionHandler(gameState, zoneCaptureHandler);
        combatHandler      = new CombatHandler(collisionHandler, tickRateMs);
        itemSpawner        = new ItemSpawner(gameState, mapWidth, mapHeight, tickRateMs);
        killSwitch         = new KillSwitch(gameState);

        // ── Initialize networking (P2) ───────────────────────
        networkManager = new ServerNetworkManager(config, actionQueue);

        // When a player joins over TCP, add them to the game state
        networkManager.onPlayerJoined = playerId -> {
            Player newPlayer = new Player(playerId, "Player" + playerId, 5, 5, 100, false, 0, true, 0, false);
            gameState.addPlayer(newPlayer);
            System.out.println("Player " + playerId + " added to game state");
        };

        // When a player disconnects, clean up their state
        networkManager.onPlayerLeft = playerId -> {
            gameState.handlePlayerDisconnect(playerId);
            System.out.println("Player " + playerId + " removed from game state");
        };

        try {
            networkManager.start();
        } catch (IOException e) {
            throw new RuntimeException("Could not start networking: " + e.getMessage(), e);
        }

        // Wire kill switch to network manager so it can close TCP connections
        killSwitch.setNetworkManager(networkManager);

        // ── Initialize game loop ─────────────────────────────
        gameLoop = new GameLoop(gameState, actionQueue, collisionHandler,
                                itemSpawner, combatHandler, tickRateMs, networkManager);

        // ── Start game ───────────────────────────────────────
        gameState.setPhase(GameState.GamePhase.IN_PROGRESS);
        gameLoop.start();

        System.out.println("Server running — waiting for players");
    }

    public void stop() {
        gameLoop.stop();
        if (networkManager != null) networkManager.stop();
        System.out.println("Server stopped");
    }

    // expose for P2 / ActionProcessor
    public GameState getGameState()                    { return gameState; }
    public ActionQueue getActionQueue()                { return actionQueue; }
    public KillSwitch getKillSwitch()                  { return killSwitch; }
    public CombatHandler getCombatHandler()            { return combatHandler; }
    public ServerNetworkManager getNetworkManager()    { return networkManager; }
}
