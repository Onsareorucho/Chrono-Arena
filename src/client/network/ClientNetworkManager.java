package client.network;

import shared.*;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * ClientNetworkManager — single entry point for all networking on the client side.
 *
 * Person 3 (Tyler/GUI) only needs this class.
 *
 * Usage in GameClient.java:
 *   GameConfig config = new GameConfig();
 *   ClientNetworkManager net = new ClientNetworkManager(config, "Alice");
 *
 *   net.onGameStateUpdate = screen::updateState;
 *   net.onGameEvent       = screen::handleEvent;
 *   net.onGameOver        = screen::showResults;
 *   net.onPlayerJoined    = screen::addPlayer;
 *   net.onPlayerLeft      = screen::removePlayer;
 *   net.onKickReceived    = k -> screen.showError(k.getReason());
 *   net.onDisconnected    = () -> screen.showError("Lost connection");
 *
 *   net.connect();   // blocks briefly for JoinResponse
 *   net.start();
 *
 *   // From InputHandler:
 *   net.sendInput(PlayerInput.move(1, 0));
 *   net.sendAction(PlayerAction.freezeRay(1, 0));
 *
 *   net.stop();
 */
public class ClientNetworkManager {

    private static final Logger LOG = Logger.getLogger(ClientNetworkManager.class.getName());

    private final GameConfig config;
    private final String playerName;

    private TCPClientHandler tcp;
    private UDPClientHandler udp;

    private int playerId = -1;

    // ── GUI callbacks ──────────────────────────────────────────

    public Consumer<GameStateUpdate>  onGameStateUpdate = u -> {};
    public Consumer<GameEvent>        onGameEvent       = e -> {};
    public Consumer<GameResult>       onGameOver        = r -> {};
    public Consumer<KickNotification> onKickReceived    = k -> {};
    public Consumer<Integer>          onPlayerJoined    = id -> {};
    public Consumer<Integer>          onPlayerLeft      = id -> {};
    public Runnable                   onDisconnected    = () -> {};

    // ──────────────────────────────────────────────────────────

    public ClientNetworkManager(GameConfig config, String playerName) {
        this.config     = config;
        this.playerName = playerName;
    }

    // ── Lifecycle ──────────────────────────────────────────────

    /**
     * Connect to server and complete join handshake.
     * Blocks briefly until JoinResponse is received.
     */
    public void connect() throws IOException {
        tcp = new TCPClientHandler(config.getServerIp(), config.getServerTcpPort());

        // Wire TCP callbacks
        tcp.onGameStateUpdate = u  -> onGameStateUpdate.accept(u);
        tcp.onGameEvent       = e  -> onGameEvent.accept(e);
        tcp.onGameOver        = r  -> onGameOver.accept(r);
        tcp.onKickReceived    = k  -> onKickReceived.accept(k);
        tcp.onPlayerJoined    = id -> onPlayerJoined.accept(id);
        tcp.onPlayerLeft      = id -> onPlayerLeft.accept(id);
        tcp.onDisconnected    = () -> onDisconnected.run();

        JoinResponse jr = tcp.connect(playerName);
        playerId = jr.getPlayerId();

        // Create UDP handler now that we know the server's UDP port
        udp = new UDPClientHandler(config.getServerIp(), jr.getServerUdpPort(), playerId);
    }

    /** Start background threads. Call after connect(). */
    public void start() throws IOException {
        if (udp == null) throw new IllegalStateException("Call connect() first");
        tcp.start();
        udp.start();
    }

    public void stop() {
        if (tcp != null) tcp.stop();
        if (udp != null) udp.stop();
    }

    // ── Input sending — called from InputHandler ───────────────

    /** Send movement input via UDP. */
    public void sendInput(PlayerInput input) {
        if (udp != null) udp.sendInput(input);
    }

    /** Send action (freeze ray, powerup) via UDP. */
    public void sendAction(PlayerAction action) {
        if (udp != null) udp.sendAction(action);
    }

    // ── Accessors ──────────────────────────────────────────────

    public int getPlayerId() { return playerId; }
    public boolean isAlive() { return tcp != null && tcp.isRunning(); }
}
