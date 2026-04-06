package shared.protocol;

/**
 * Keepalive ping sent by the client every HEARTBEAT_INTERVAL_MS milliseconds.
 * The server replies immediately with a HeartbeatAck.
 *
 * If the server misses MAX_MISSED_HEARTBEATS consecutive heartbeats from a
 * client, it evicts that client via TCPServerHandler.evictPlayer().
 */
public class Heartbeat extends Message {

    public int  playerId;
    public long sentAt;   // client System.currentTimeMillis() at send time

    public Heartbeat() { super(MessageType.HEARTBEAT); }

    public Heartbeat(int playerId) {
        super(MessageType.HEARTBEAT);
        this.playerId = playerId;
        this.sentAt   = System.currentTimeMillis();
    }
}
