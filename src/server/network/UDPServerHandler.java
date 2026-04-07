package server.network;

import server.logic.ActionQueue;
import shared.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UDPServerHandler receives all UDP action/movement packets from clients.
 *
 * Uses Andrew's MessageSerializer.deserialize() for packet parsing and
 * his SequenceTracker for duplicate/out-of-order detection.
 *
 * Validated GameMessages are pushed onto Moses's ActionQueue via addAction().
 *
 * Threading:
 *   - 1 receiver thread: pulls raw bytes from DatagramSocket
 *   - 4 worker threads: deserialize + validate + enqueue
 */
public class UDPServerHandler {

    private static final Logger LOG = Logger.getLogger(UDPServerHandler.class.getName());
    private static final int WORKER_THREADS = 4;

    private final int udpPort;
    private final ActionQueue actionQueue;
    private final SequenceTracker sequenceTracker = new SequenceTracker();

    private DatagramSocket socket;
    private volatile boolean running = false;

    private final BlockingQueue<byte[]> rawPackets = new LinkedBlockingQueue<>(1000);

    private final ExecutorService workerPool = Executors.newFixedThreadPool(WORKER_THREADS, r -> {
        Thread t = new Thread(r, "udp-worker");
        t.setDaemon(true);
        return t;
    });

    /**
     * @param udpPort     port to listen on
     * @param actionQueue Moses's ActionQueue — receives validated GameMessages
     */
    public UDPServerHandler(int udpPort, ActionQueue actionQueue) {
        this.udpPort     = udpPort;
        this.actionQueue = actionQueue;
    }

    // ── Lifecycle ──────────────────────────────────────────────

    public void start() throws IOException {
        socket = new DatagramSocket(udpPort);
        running = true;
        LOG.info("UDP server listening on port " + udpPort);

        Thread receiver = new Thread(this::receiveLoop, "udp-receiver");
        receiver.setDaemon(true);
        receiver.start();

        for (int i = 0; i < WORKER_THREADS; i++) {
            workerPool.submit(this::workerLoop);
        }
    }

    public void stop() {
        running = false;
        if (socket != null) socket.close();
        workerPool.shutdownNow();
    }

    // ── Receiver thread ────────────────────────────────────────

    private void receiveLoop() {
        byte[] buf = new byte[GameConstants.MAX_UDP_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        while (running) {
            try {
                socket.receive(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);

                if (!rawPackets.offer(data)) {
                    LOG.warning("UDP queue full — dropping packet");
                }
            } catch (IOException e) {
                if (running) LOG.log(Level.WARNING, "UDP receive error: " + e.getMessage());
            }
        }
    }

    // ── Worker threads ─────────────────────────────────────────

    private void workerLoop() {
        while (running) {
            try {
                byte[] data = rawPackets.take();
                processPacket(data);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processPacket(byte[] data) {
        try {
            GameMessage msg = MessageSerializer.deserialize(data);

            // Only accept UDP message types
            if (msg.getType() != MessageType.PLAYER_INPUT
                    && msg.getType() != MessageType.PLAYER_ACTION) {
                LOG.warning("Unexpected message type via UDP: " + msg.getType());
                return;
            }

            // Use Andrew's SequenceTracker to drop duplicates/stale packets
            int senderId = msg.getSenderId();
            int seqNum   = msg.getSequenceNumber();

            if (sequenceTracker.isDuplicate(senderId, seqNum)) {
                return; // exact duplicate — drop silently
            }
            if (!sequenceTracker.shouldProcess(senderId, seqNum)) {
                return; // too old — drop silently
            }

            // Push to Moses's ActionQueue
            actionQueue.addAction(msg);

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to process UDP packet: " + e.getMessage());
        }
    }

    // ── Player cleanup ─────────────────────────────────────────

    /** Call when a player disconnects to free sequence tracking state. */
    public void removePlayer(int playerId) {
        sequenceTracker.removePlayer(playerId);
    }
}
