import java.io.Serializable;

/**
 * Sent from server to clients when something notable happens in the game.
 * Unlike GameStateUpdate which is everything, this is just one specific thing.
 * 
 * Useful for triggering sound effects, animations, or notifications on clients.
 */
public class GameEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final EventType eventType;
    
    // Who caused this event (-1 if not applicable)
    private final int sourcePlayerId;
    
    // Who was affected (-1 if not applicable)
    private final int targetPlayerId;
    
    // Which zone or item this relates to (-1 if not applicable)
    private final int objectId;
    
    // Where it happened (for visual effects)
    private final float x;
    private final float y;
    
    // Optional extra info (like the item type, or points gained)
    private final String extraData;
    
    
    public GameEvent(EventType eventType, int sourcePlayerId, int targetPlayerId,
                     int objectId, float x, float y, String extraData) {
        this.eventType = eventType;
        this.sourcePlayerId = sourcePlayerId;
        this.targetPlayerId = targetPlayerId;
        this.objectId = objectId;
        this.x = x;
        this.y = y;
        this.extraData = extraData;
    }
    
    // Convenience constructors for common events
    
    public static GameEvent playerFroze(int attackerId, int victimId, float x, float y) {
        return new GameEvent(EventType.PLAYER_FROZEN, attackerId, victimId, -1, x, y, null);
    }
    
    public static GameEvent zoneCaptured(int playerId, int zoneId, float x, float y) {
        return new GameEvent(EventType.ZONE_CAPTURED, playerId, -1, zoneId, x, y, null);
    }
    
    public static GameEvent zoneLost(int playerId, int zoneId, float x, float y) {
        return new GameEvent(EventType.ZONE_LOST, playerId, -1, zoneId, x, y, null);
    }
    
    public static GameEvent itemCollected(int playerId, int itemId, String itemType, float x, float y) {
        return new GameEvent(EventType.ITEM_COLLECTED, playerId, -1, itemId, x, y, itemType);
    }
    
    public static GameEvent itemSpawned(int itemId, String itemType, float x, float y) {
        return new GameEvent(EventType.ITEM_SPAWNED, -1, -1, itemId, x, y, itemType);
    }
    
    
    public EventType getEventType() {
        return eventType;
    }
    
    public int getSourcePlayerId() {
        return sourcePlayerId;
    }
    
    public int getTargetPlayerId() {
        return targetPlayerId;
    }
    
    public int getObjectId() {
        return objectId;
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public String getExtraData() {
        return extraData;
    }
    
    
    @Override
    public String toString() {
        return "GameEvent{" + eventType + ", source=" + sourcePlayerId + 
               ", target=" + targetPlayerId + ", at=(" + x + "," + y + ")}";
    }
    
    
    // All the things that can happen in the game
    public enum EventType {
        PLAYER_FROZEN,      // someone got hit by freeze ray
        PLAYER_UNFROZEN,    // freeze effect wore off
        ZONE_CAPTURED,      // someone took control of a zone
        ZONE_LOST,          // someone lost control of a zone
        ZONE_CONTESTED,     // multiple players fighting for a zone
        ITEM_SPAWNED,       // new item appeared on map
        ITEM_COLLECTED,     // someone picked up an item
        PLAYER_JOINED,      // new player entered the game
        PLAYER_LEFT,        // player disconnected or quit
        GAME_STARTING,      // countdown before game starts
        GAME_ENDED          // round is over
    }
}
