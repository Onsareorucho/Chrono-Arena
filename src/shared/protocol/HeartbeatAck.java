package shared.protocol;

/**
 * Server's immediate reply to a Heartbeat.
 * Echoes back originalSentAt so the client can compute round-trip time:
 *
 *   long rttMs = System.currentTimeMillis() - ack.originalSentAt;
 *
 * The client can display this as a latency indicator in the HUD (optional).
 */
public class HeartbeatAck extends Message {

    /** The sentAt value from the original Heartbeat — echoed back for RTT calculation. */
    public long originalSentAt;

    public HeartbeatAck() { super(MessageType.HEARTBEAT_ACK); }

    public HeartbeatAck(long originalSentAt) {
        super(MessageType.HEARTBEAT_ACK);
        this.originalSentAt = originalSentAt;
    }
}
