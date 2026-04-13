package server;

import server.logic.ActionQueue;
import server.logic.CollisionHandler;
import server.logic.CombatHandler;
import server.logic.ZoneCaptureHandler;
import server.network.ServerNetworkManager;
import server.util.KillSwitch;
import shared.GameConfig;
import shared.GameConstants;
import shared.GameEvent;

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
        // Four corner spawns — spread players across the 20x20 map
        int[][] spawnPoints = { {1, 1}, {18, 18}, {1, 18}, {18, 1} };

        networkManager.onPlayerJoined = (playerId, playerName) -> {
            // If the previous game finished, reset before starting a new one
            if (gameState.getPhase() == GameState.GamePhase.FINISHED) {
                gameState.reset(config.getGameDurationSeconds() * 1000L);
                gameLoop = new GameLoop(gameState, actionQueue, collisionHandler,
                                        itemSpawner, combatHandler, tickRateMs, networkManager);
            }

            int[] spawn = spawnPoints[(playerId - 1) % spawnPoints.length];
            Player newPlayer = new Player(playerId, playerName,
                    spawn[0], spawn[1], 100, false, 0, true, 0, false);
            gameState.addPlayer(newPlayer);
            System.out.println("Player " + playerId + " (" + playerName + ") spawned at ("
                    + spawn[0] + "," + spawn[1] + ")");

            // Start game once minimum players have joined
            if (gameState.getPhase() == GameState.GamePhase.WAITING
                    && gameState.getPlayers().size() >= GameConstants.MIN_PLAYERS) {
                System.out.println("Minimum players reached — starting game!");
                networkManager.broadcastEvent(GameEvent.gameStarting());
                gameState.setPhase(GameState.GamePhase.IN_PROGRESS);
                gameLoop.start();
            }
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

        // Wire event broadcasting into combat and collision handlers
        combatHandler.setEventBroadcaster(networkManager::broadcastEvent);
        collisionHandler.setEventBroadcaster(networkManager::broadcastEvent);

        // ── Initialize game loop ─────────────────────────────
        gameLoop = new GameLoop(gameState, actionQueue, collisionHandler,
                                itemSpawner, combatHandler, tickRateMs, networkManager);

        System.out.println("Server running — waiting for " + GameConstants.MIN_PLAYERS + " players to start");
    }

    public void stop() {
        gameLoop.stop();
        if (networkManager != null) networkManager.stop();
        System.out.println("Server stopped");
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown"));

        // Block main thread so the JVM stays alive
        try { Thread.currentThread().join(); } catch (InterruptedException ignored) {}
    }

    // expose for P2 / ActionProcessor
    public GameState getGameState()                    { return gameState; }
    public ActionQueue getActionQueue()                { return actionQueue; }
    public KillSwitch getKillSwitch()                  { return killSwitch; }
    public CombatHandler getCombatHandler()            { return combatHandler; }
    public ServerNetworkManager getNetworkManager()    { return networkManager; }
}
