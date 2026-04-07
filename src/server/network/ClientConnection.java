package server.network;

import shared.GameMessage;
import shared.MessageSerializer;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps a single client's TCP connection on the server side.
 *
 * Uses Andrew's MessageSerializer.writeToStream() / readFromStream()
 * for all serialization — no Gson, no custom framing needed since
 * MessageSerializer handles the length-prefixing internally.
 *
 * Thread-safety: send() is synchronized so the game loop broadcast
 * thread and the heartbeat monitor can both write safely.
 */
public class ClientConnection {

    private static final Logger LOG = Logger.getLogger(ClientConnection.class.getName());

    private final int playerId;
    private final Socket socket;
    private final OutputStream out;
    private final InputStream in;

    private volatile boolean alive = true;
    private volatile long lastSeenMs = System.currentTimeMillis();
    private volatile int missedHeartbeats = 0;

    public ClientConnection(int playerId, Socket socket) throws IOException {
        this.playerId = playerId;
        this.socket   = socket;
        this.out      = new BufferedOutputStream(socket.getOutputStream());
        this.in       = new BufferedInputStream(socket.getInputStream());
    }

    // ── Sending ────────────────────────────────────────────────

    /**
     * Send a GameMessage to this client over TCP.
     * Uses MessageSerializer.writeToStream() which handles length-prefixing.
     * Synchronized to prevent concurrent writes corrupting the stream.
     */
    public synchronized void send(GameMessage message) {
        if (!alive) return;
        try {
            MessageSerializer.writeToStream(message, out);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Send failed for player " + playerId + ": " + e.getMessage());
            markDead();
        }
    }

    // ── Receiving ──────────────────────────────────────────────

    /**
     * Blocking read of the next GameMessage from this client.
     * Returns null if the connection is closed or an error occurs.
     * Called from the dedicated reader thread for this connection.
     */
    public GameMessage receive() {
        if (!alive) return null;
        try {
            GameMessage msg = MessageSerializer.readFromStream(in);
            touch();
            return msg;
        } catch (EOFException e) {
            markDead();
            return null;
        } catch (IOException | ClassNotFoundException e) {
            if (alive) {
                LOG.log(Level.WARNING, "Receive error for player " + playerId + ": " + e.getMessage());
            }
            markDead();
            return null;
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────

    public void close() {
        markDead();
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void markDead() { alive = false; }

    public void touch() {
        lastSeenMs = System.currentTimeMillis();
        missedHeartbeats = 0;
    }

    public void incrementMissedHeartbeats() { missedHeartbeats++; }

    // ── Accessors ──────────────────────────────────────────────

    public int getPlayerId()          { return playerId; }
    public boolean isAlive()          { return alive; }
    public long getLastSeenMs()       { return lastSeenMs; }
    public int getMissedHeartbeats()  { return missedHeartbeats; }
    public String getRemoteAddress()  { return socket.getRemoteSocketAddress().toString(); }
}
