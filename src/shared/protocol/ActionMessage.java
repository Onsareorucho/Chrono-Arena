package shared.protocol;

import shared.Direction;

/**
 * Carries a single player input action.
 * Sent client → server over UDP for low latency.
 *
 * sequenceNumber allows the server's PacketSequencer to:
 *   1. Drop exact duplicates (same number seen twice)
 *   2. Accept slightly out-of-order packets (within a 32-packet window)
 *   3. Drop very stale packets (more than 32 behind the latest)
 *
 * timestamp is the client wall-clock at send time — useful for the server
 * to measure client latency or log lag spikes, but NOT used for ordering
 * (clocks differ between machines). Ordering uses sequenceNumber only.
 */
public class ActionMessage extends Message {

    public enum ActionType {
        MOVE,        // direction field is set
        ATTACK,      // freeze-ray fire; no direction needed
        COLLECT,     // explicit item pickup (if server requires it)
        USE_ABILITY  // speed boost or other power-up activation
    }

    public int        playerId;
    public long       sequenceNumber;
    public ActionType actionType;
    public Direction  direction;   // meaningful for MOVE; NONE for others
    public long       timestamp;   // client System.currentTimeMillis()

    /** Required for Gson deserialization. */
    public ActionMessage() {
        super(MessageType.ACTION_MESSAGE);
    }

    // ── Factory methods (used by UDPClientHandler) ─────────────

    public static ActionMessage move(int playerId, long seqNum, Direction dir) {
        ActionMessage m = new ActionMessage();
        m.playerId       = playerId;
        m.sequenceNumber = seqNum;
        m.actionType     = ActionType.MOVE;
        m.direction      = dir;
        m.timestamp      = System.currentTimeMillis();
        return m;
    }

    public static ActionMessage attack(int playerId, long seqNum) {
        ActionMessage m = new ActionMessage();
        m.playerId       = playerId;
        m.sequenceNumber = seqNum;
        m.actionType     = ActionType.ATTACK;
        m.direction      = Direction.NONE;
        m.timestamp      = System.currentTimeMillis();
        return m;
    }

    public static ActionMessage useAbility(int playerId, long seqNum) {
        ActionMessage m = new ActionMessage();
        m.playerId       = playerId;
        m.sequenceNumber = seqNum;
        m.actionType     = ActionType.USE_ABILITY;
        m.direction      = Direction.NONE;
        m.timestamp      = System.currentTimeMillis();
        return m;
    }

    @Override
    public String toString() {
        return "ActionMessage{player=" + playerId
               + ", seq=" + sequenceNumber
               + ", type=" + actionType
               + ", dir=" + direction + "}";
    }
}
