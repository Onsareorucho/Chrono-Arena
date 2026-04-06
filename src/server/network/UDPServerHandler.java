package server.network;

import shared.GameConstants;
import shared.protocol.ActionMessage;
import shared.protocol.Message;
import shared.protocol.MessageSerializer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UDPServerHandler receives all UDP action packets from clients.
 *
 * Architecture:
 *  - 1 receiver thread: blocks on DatagramSocket.receive(), pulls raw bytes
 *  - N worker threads: parse JSON and validate sequence numbers
 *    (parsing is CPU-bound and can be parallelized)
 *  - Validated ActionMessages are pushed onto the shared action queue
 *    provided by the server core (Person 1's ActionQueue)
 *
 * Fairness:
 *  The server core's ActionQueue is thread-safe; all workers enqueue
 *  concurrently, so the ordering within a single tick is determined by
 *  the order they arrive at the queue — which is fair across players
 *  because each player's packets are processed independently.
 *
 * Out-of-order / duplicate handling:
 *  PacketSequencer maintains per-player sliding windows. Any packet
 *  outside the window is silently dropped before reaching the queue.
 *
 * Integration:
 *  Set onActionReceived to hook into the server core ActionQueue:
 *    udpHandler.onActionReceived = actionQueue::add;
 */
public class UDPServerHandler {

    private static final Logger LOG = Logger.getLogger(UDPServerHandler.class.getName());
    private static final int WORKER_THREADS = 4;

    private final int udpPort;
    private final PacketSequencer sequencer = new PacketSequencer();

    private DatagramSocket socket;
    private volatile boolean running = false;

    /** Raw packet queue between the receiver thread and worker threads. */
    private final BlockingQueue<byte[]> rawPackets = new LinkedBlockingQueue<>(1000);

    private final ExecutorService workerPool = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("udp-worker");
        return t;
    });

    /**
     * Callback invoked (on a worker thread) for every valid, non-duplicate ActionMessage.
     * Wire this to ActionQueue::add in server initialization.
     */
    public Consumer<ActionMessage> onActionReceived = action -> {};

    public UDPServerHandler(int udpPort) {
        this.udpPort = udpPort;
    }

    // ──────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────

    public void start() throws IOException {
        socket = new DatagramSocket(udpPort);
        running = true;
        LOG.info("UDP server listening on port " + udpPort);

        // Single receiver thread — keeps DatagramSocket access single-threaded
        Thread receiver = new Thread(this::receiveLoop, "udp-receiver");
        receiver.setDaemon(true);
        receiver.start();

        // Worker pool — parse + validate + enqueue
        for (int i = 0; i < WORKER_THREADS; i++) {
            workerPool.submit(this::workerLoop);
        }
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
        workerPool.shutdownNow();
    }

    // ──────────────────────────────────────────────────────────
    // Receiver thread
    // ──────────────────────────────────────────────────────────

    private void receiveLoop() {
        byte[] buf = new byte[GameConstants.MAX_UDP_PACKET_BYTES];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        while (running) {
            try {
                socket.receive(packet);
                // Copy the relevant bytes (packet.getLength() may be < buf.length)
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);

                // Offer to worker queue; drop if full to avoid backpressure building up
                if (!rawPackets.offer(data)) {
                    LOG.warning("UDP raw packet queue full — dropping packet");
                }
            } catch (IOException e) {
                if (running) LOG.log(Level.WARNING, "UDP receive error: " + e.getMessage());
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Worker threads
    // ──────────────────────────────────────────────────────────

    private void workerLoop() {
        while (running) {
            try {
                byte[] data = rawPackets.take(); // blocks until work is available
                processPacket(data);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processPacket(byte[] data) {
        try {
            String json = new String(data, "UTF-8");
            Message msg = MessageSerializer.fromJson(json);

            if (msg.type != Message.MessageType.ACTION_MESSAGE) {
                LOG.warning("Received non-ACTION_MESSAGE via UDP — ignoring (type=" + msg.type + ")");
                return;
            }

            ActionMessage action = (ActionMessage) msg;

            // Validate sequence number — drops duplicates and very stale packets
            if (!sequencer.accept(action.playerId, action.sequenceNumber)) {
                // Silently drop — this is normal UDP behavior
                return;
            }

            onActionReceived.accept(action);

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to process UDP packet: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────
    // Player cleanup
    // ──────────────────────────────────────────────────────────

    /** Call when a player disconnects so their sequence state is freed. */
    public void removePlayer(int playerId) {
        sequencer.removePlayer(playerId);
    }
}
