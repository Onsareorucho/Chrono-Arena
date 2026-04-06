package server.network;

import shared.protocol.Message;
import shared.protocol.MessageSerializer;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps a single client's TCP connection on the server side.
 *
 * Thread-safety: send() is synchronized so multiple server threads
 * (game loop + broadcast) can safely write to the same socket without
 * interleaving partial messages.
 *
 * Wire format:
 *   Each message is sent as a length-prefixed UTF-8 JSON line:
 *     [4-byte big-endian int: byte length][JSON bytes]
 *   This lets the reader on the other side know exactly how many bytes
 *   to read before attempting to parse — avoids TCP stream fragmentation issues.
 */
public class ClientConnection {

    private static final Logger LOG = Logger.getLogger(ClientConnection.class.getName());

    private final int playerId;
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream  in;
    private volatile boolean alive = true;
    private volatile long lastSeenMs = System.currentTimeMillis();
    private volatile int missedHeartbeats = 0;

    public ClientConnection(int playerId, Socket socket) throws IOException {
        this.playerId = playerId;
        this.socket   = socket;
        this.out      = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.in       = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    // ──────────────────────────────────────────────────────────
    // Sending
    // ──────────────────────────────────────────────────────────

    /**
     * Serialize and send a message over TCP.
     * Synchronized to prevent concurrent writes from corrupting the stream.
     */
    public synchronized void send(Message message) {
        if (!alive) return;
        try {
            byte[] bytes = MessageSerializer.toJson(message).getBytes("UTF-8");
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Send failed for player " + playerId + ": " + e.getMessage());
            markDead();
        }
    }

    // ──────────────────────────────────────────────────────────
    // Receiving
    // ──────────────────────────────────────────────────────────

    /**
     * Blocking read of the next message from this client.
     * Returns null if the connection is closed or an error occurs.
     *
     * Called from the dedicated reader thread for this connection.
     */
    public Message receive() {
        if (!alive) return null;
        try {
            int length = in.readInt();
            if (length <= 0 || length > 65536) {
                LOG.warning("Suspicious message length " + length + " from player " + playerId);
                markDead();
                return null;
            }
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            touch();
            return MessageSerializer.fromJson(new String(bytes, "UTF-8"));
        } catch (EOFException e) {
            // Client closed the connection cleanly
            markDead();
            return null;
        } catch (IOException e) {
            if (alive) {
                LOG.log(Level.WARNING, "Receive error for player " + playerId + ": " + e.getMessage());
            }
            markDead();
            return null;
        }
    }

    // ──────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────

    public void close() {
        markDead();
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void markDead() {
        alive = false;
    }

    /** Update last-seen timestamp (called on every successful receive). */
    public void touch() {
        lastSeenMs = System.currentTimeMillis();
        missedHeartbeats = 0;
    }

    public void incrementMissedHeartbeats() {
        missedHeartbeats++;
    }

    // ──────────────────────────────────────────────────────────
    // Accessors
    // ──────────────────────────────────────────────────────────

    public int getPlayerId()           { return playerId; }
    public boolean isAlive()           { return alive; }
    public long getLastSeenMs()        { return lastSeenMs; }
    public int getMissedHeartbeats()   { return missedHeartbeats; }
    public String getRemoteAddress()   { return socket.getRemoteSocketAddress().toString(); }
}
