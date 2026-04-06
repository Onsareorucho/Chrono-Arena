package shared.protocol;

/**
 * Notifies all clients of a lifecycle or state change event for one player.
 * Sent server → all clients over TCP.
 *
 * GUI usage examples:
 *   JOINED   → add player sprite to arena
 *   LEFT     → remove player sprite, release any owned zones
 *   FROZEN   → play freeze animation, grey out player controls briefly
 *   UNFROZEN → restore normal appearance
 *   KILLED   → show "You were removed" dialog to that client
 */
public class PlayerEvent extends Message {

    public enum EventType {
        JOINED,    // new player connected
        LEFT,      // player disconnected or was evicted
        FROZEN,    // hit by freeze-ray — player cannot move
        UNFROZEN,  // freeze duration expired
        KILLED     // KILL_SWITCH: server force-removed this player
    }

    public EventType eventType;
    public int       playerId;
    public String    details;   // human-readable description (can be null)

    public PlayerEvent() { super(MessageType.PLAYER_EVENT); }

    public PlayerEvent(EventType eventType, int playerId, String details) {
        super(MessageType.PLAYER_EVENT);
        this.eventType = eventType;
        this.playerId  = playerId;
        this.details   = details;
    }
}
