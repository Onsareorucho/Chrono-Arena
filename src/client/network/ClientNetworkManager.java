package client.network;

import server.util.ConfigLoader;
import shared.Direction;
import shared.protocol.*;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * ClientNetworkManager is the single entry point for all networking on the client side.
 *
 * Person 3 (GUI) only needs to interact with this class.
 * It owns both TCPClientHandler and UDPClientHandler and wires them together.
 *
 * Typical usage in GameClient.java:
 * ─────────────────────────────────────────────────────────────────────────────
 *   ConfigLoader config = new ConfigLoader("game.properties");
 *
 *   ClientNetworkManager net = new ClientNetworkManager(config, "Alice");
 *
 *   // Wire GUI callbacks
 *   net.onGameStateUpdate = gameScreen::updateState;
 *   net.onScoreUpdate     = gameScreen::updateScores;
 *   net.onPlayerEvent     = gameScreen::handlePlayerEvent;
 *   net.onGameOver        = gameScreen::showGameOver;
 *   net.onKillReceived    = msg -> showDialog("Kicked: " + msg.reason);
 *   net.onDisconnected    = () -> showDialog("Lost connection to server");
 *
 *   // Connect (blocks briefly for JoinResponse)
 *   net.connect(); // throws IOException if server unreachable or join rejected
 *
 *   // Start background threads
 *   net.start();
 *
 *   // From InputHandler:
 *   net.sendMove(Direction.UP);
 *   net.sendAttack();
 *
 *   // On exit:
 *   net.stop();
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * Thread safety:
 *   sendMove / sendAttack may be called from the Swing EDT.
 *   All callbacks are invoked on background reader threads.
 *   GUI callbacks must use SwingUtilities.invokeLater if they touch Swing components.
 */
public class ClientNetworkManager {

    private static final Logger LOG = Logger.getLogger(ClientNetworkManager.class.getName());

    private final TCPClientHandler tcp;
    private       UDPClientHandler udp; // created after connect() gives us udpPort

    private final String serverIp;
    private final ConfigLoader config;

    private int playerId = -1;

    // ── GUI callbacks (set before calling connect()) ───────────

    public Consumer<GameStateUpdate> onGameStateUpdate = u -> {};
    public Consumer<ScoreUpdate>     onScoreUpdate     = u -> {};
    public Consumer<PlayerEvent>     onPlayerEvent     = e -> {};
    public Consumer<GameOver>        onGameOver        = g -> {};
    public Consumer<KillClient>      onKillReceived    = k -> {};
    public Runnable                  onDisconnected    = () -> {};

    // ──────────────────────────────────────────────────────────
    // Constructor
    // ──────────────────────────────────────────────────────────

    public ClientNetworkManager(ConfigLoader config, String playerName) {
        this.config   = config;
        this.serverIp = config.getServerIp();
        this.tcp      = new TCPClientHandler(serverIp, config.getTcpPort(), playerName);
    }

    // ──────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────

    /**
     * Connect to the server and complete the join handshake.
     * Blocks until a JoinResponse is received.
     *
     * @throws IOException if the TCP connection fails
     * @throws IllegalStateException if the server rejects the join
     */
    public void connect() throws IOException {
        JoinResponse jr = tcp.connect();
        if (!jr.accepted) {
            throw new IllegalStateException("Server rejected join: " + jr.rejectReason);
        }
        playerId = jr.assignedPlayerId;
        LOG.info("Joined as playerId=" + playerId);

        // Now we know the UDP port
        udp = new UDPClientHandler(serverIp, jr.udpPort, playerId);
    }

    /**
     * Start background reader and heartbeat threads.
     * Call after connect() succeeds.
     *
     * @throws IOException if the UDP socket cannot be created
     */
    public void start() throws IOException {
        if (udp == null) throw new IllegalStateException("Call connect() before start()");

        // Wire TCP callbacks to our public fields
        tcp.onGameStateUpdate = msg -> onGameStateUpdate.accept(msg);
        tcp.onScoreUpdate     = msg -> onScoreUpdate.accept(msg);
        tcp.onPlayerEvent     = msg -> onPlayerEvent.accept(msg);
        tcp.onGameOver        = msg -> onGameOver.accept(msg);
        tcp.onKillReceived    = msg -> onKillReceived.accept(msg);
        tcp.onDisconnected    = () -> onDisconnected.run();

        tcp.start();
        udp.start();
    }

    public void stop() {
        tcp.stop();
        if (udp != null) udp.stop();
    }

    // ──────────────────────────────────────────────────────────
    // Input sending (called from InputHandler)
    // ──────────────────────────────────────────────────────────

    /** Send a movement input to the server via UDP. */
    public void sendMove(Direction direction) {
        if (udp != null) udp.sendMove(direction);
    }

    /** Fire the freeze-ray via UDP. */
    public void sendAttack() {
        if (udp != null) udp.sendAttack();
    }

    /** Activate a held power-up via UDP. */
    public void sendUseAbility() {
        if (udp != null) udp.sendUseAbility();
    }

    // ──────────────────────────────────────────────────────────
    // Accessors
    // ──────────────────────────────────────────────────────────

    public int getPlayerId()  { return playerId; }
    public boolean isAlive()  { return tcp.isRunning(); }
}
