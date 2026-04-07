package client.network;

import shared.*;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UDPClientHandler sends player inputs (movement, actions) to the server via UDP.
 *
 * Uses Andrew's GameMessage + MessageSerializer.serialize() for packets.
 * Uses Andrew's SequenceGenerator for monotonic sequence numbers.
 *
 * Usage (from InputHandler):
 *   udpClient.sendInput(PlayerInput.move(1, 0));       // move right
 *   udpClient.sendAction(PlayerAction.freezeRay(1,0)); // fire right
 */
public class UDPClientHandler {

    private static final Logger LOG = Logger.getLogger(UDPClientHandler.class.getName());

    private final String serverIp;
    private final int serverUdpPort;
    private int playerId;

    private DatagramSocket socket;
    private InetAddress serverAddress;

    // Andrew's SequenceGenerator handles thread-safe monotonic sequence numbers
    private final SequenceGenerator sequenceGenerator = new SequenceGenerator();

    private volatile boolean running = false;

    public UDPClientHandler(String serverIp, int serverUdpPort, int playerId) {
        this.serverIp      = serverIp;
        this.serverUdpPort = serverUdpPort;
        this.playerId      = playerId;
    }

    // ── Lifecycle ──────────────────────────────────────────────

    public void start() throws IOException {
        serverAddress = InetAddress.getByName(serverIp);
        socket = new DatagramSocket(); // OS picks ephemeral port
        running = true;
        LOG.info("UDP client ready, sending to " + serverIp + ":" + serverUdpPort);
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }

    // ── Send helpers — called from InputHandler ────────────────

    /**
     * Send a movement input to the server.
     * @param input PlayerInput.move(dx, dy) or PlayerInput.stop()
     */
    public void sendInput(PlayerInput input) {
        GameMessage msg = new GameMessage(
            MessageType.PLAYER_INPUT,
            playerId,
            sequenceGenerator.next(),
            input
        );
        sendPacket(msg);
    }

    /**
     * Send an action (freeze ray, powerup) to the server.
     * @param action PlayerAction.freezeRay(dx, dy) or PlayerAction.usePowerup()
     */
    public void sendAction(PlayerAction action) {
        GameMessage msg = new GameMessage(
            MessageType.PLAYER_ACTION,
            playerId,
            sequenceGenerator.next(),
            action
        );
        sendPacket(msg);
    }

    // ── Low-level send ─────────────────────────────────────────

    private void sendPacket(GameMessage message) {
        if (!running) return;
        try {
            byte[] data = MessageSerializer.serialize(message);
            if (data.length > GameConstants.MAX_UDP_PACKET_SIZE) {
                LOG.warning("UDP packet too large (" + data.length + " bytes) — dropping");
                return;
            }
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverUdpPort);
            socket.send(packet);
        } catch (IOException e) {
            if (running) LOG.log(Level.WARNING, "UDP send failed: " + e.getMessage());
        }
    }

    // ── Accessors ──────────────────────────────────────────────

    public void setPlayerId(int playerId)  { this.playerId = playerId; }
    public void resetSequence()            { sequenceGenerator.reset(); }
    public boolean isRunning()             { return running; }
}
