package shared.protocol;

/**
 * Server's reply to a JoinRequest.
 *
 * If accepted == true:
 *   - assignedPlayerId is the permanent ID for this session
 *   - udpPort is the port the client should send ActionMessages to
 *
 * If accepted == false:
 *   - rejectReason explains why (e.g., "Game already in progress")
 */
public class JoinResponse extends Message {

    public boolean accepted;
    public int     assignedPlayerId;
    public int     udpPort;
    public String  rejectReason;

    /** Required for Gson deserialization. */
    public JoinResponse() {
        super(MessageType.JOIN_RESPONSE);
    }

    // ── Factory methods ────────────────────────────────────────

    public static JoinResponse accept(int playerId, int udpPort) {
        JoinResponse r = new JoinResponse();
        r.accepted         = true;
        r.assignedPlayerId = playerId;
        r.udpPort          = udpPort;
        return r;
    }

    public static JoinResponse reject(String reason) {
        JoinResponse r = new JoinResponse();
        r.accepted     = false;
        r.rejectReason = reason;
        return r;
    }
}
