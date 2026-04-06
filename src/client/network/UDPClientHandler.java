package client.network;

import shared.Direction;
import shared.GameConstants;
import shared.protocol.ActionMessage;
import shared.protocol.MessageSerializer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UDPClientHandler sends player input (movement, attacks) to the server via UDP.
 *
 * Why UDP for inputs:
 *  - Movement packets are latency-sensitive. A missed movement packet is less
 *    damaging than waiting for a TCP retransmit — the player just sends the
 *    next direction update 50ms later anyway.
 *  - The server's PacketSequencer handles deduplication on the other end.
 *
 * Sequence numbers:
 *  Each packet gets a monotonically increasing sequence number so the server
 *  can detect duplicates and ignore very out-of-order packets (e.g. packets
 *  that arrive 2 seconds late). The counter is per-client-session; it resets
 *  to 0 on a new game/reconnect by calling resetSequence().
 *
 * Usage (called from InputHandler every tick or on key event):
 *   udpClient.sendMove(Direction.UP);
 *   udpClient.sendAttack();
 */
public class UDPClientHandler {

    private static final Logger LOG = Logger.getLogger(UDPClientHandler.class.getName());

    private final String serverIp;
    private final int udpPort;
    private int playerId;

    private DatagramSocket socket;
    private InetAddress serverAddress;

    /** Monotonically increasing. AtomicLong because InputHandler runs on the EDT. */
    private final AtomicLong sequenceCounter = new AtomicLong(0);

    private volatile boolean running = false;

    public UDPClientHandler(String serverIp, int udpPort, int playerId) {
        this.serverIp = serverIp;
        this.udpPort  = udpPort;
        this.playerId = playerId;
    }

    // ──────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────

    public void start() throws IOException {
        serverAddress = InetAddress.getByName(serverIp);
        socket = new DatagramSocket(); // client picks its own ephemeral port
        running = true;
        LOG.info("UDP client ready, sending to " + serverIp + ":" + udpPort);
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
    }

    // ──────────────────────────────────────────────────────────
    // Send helpers — called from InputHandler
    // ──────────────────────────────────────────────────────────

    /**
     * Send a movement action to the server.
     * @param direction one of UP/DOWN/LEFT/RIGHT (and diagonals if enabled)
     */
    public void sendMove(Direction direction) {
        ActionMessage action = ActionMessage.move(playerId, nextSeq(), direction);
        sendPacket(action);
    }

    /**
     * Send a freeze-ray attack action.
     */
    public void sendAttack() {
        ActionMessage action = ActionMessage.attack(playerId, nextSeq());
        sendPacket(action);
    }

    /**
     * Activate a held power-up (speed boost, etc.) via UDP.
     */
    public void sendUseAbility() {
        ActionMessage action = ActionMessage.useAbility(playerId, nextSeq());
        sendPacket(action);
    }

    /**
     * Generic send for any ActionMessage (e.g., USE_ABILITY, COLLECT).
     */
    public void send(ActionMessage action) {
        sendPacket(action);
    }

    // ──────────────────────────────────────────────────────────
    // Low-level send
    // ──────────────────────────────────────────────────────────

    private void sendPacket(ActionMessage action) {
        if (!running) return;
        try {
            byte[] data = MessageSerializer.toJson(action).getBytes("UTF-8");
            if (data.length > GameConstants.MAX_UDP_PACKET_BYTES) {
                LOG.warning("ActionMessage too large (" + data.length + " bytes) — dropping");
                return;
            }
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, udpPort);
            socket.send(packet);
        } catch (IOException e) {
            if (running) LOG.log(Level.WARNING, "UDP send failed: " + e.getMessage());
        }
    }

    private long nextSeq() {
        return sequenceCounter.getAndIncrement();
    }

    // ──────────────────────────────────────────────────────────
    // Accessors
    // ──────────────────────────────────────────────────────────

    /** Call if the player ID changes (e.g., reconnect with new session). */
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    /** Reset the sequence counter — call at the start of a new round/reconnect. */
    public void resetSequence() {
        sequenceCounter.set(0);
    }

    public boolean isRunning() { return running; }
}
