package shared;

import java.io.Serializable;

public class GameEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final EventType eventType;
    private final int sourcePlayerId;  // -1 if not applicable
    private final int targetPlayerId;  // -1 if not applicable
    private final int objectId;        // zone or item id, -1 if not applicable
    private final float x;
    private final float y;
    private final String extraData;    // item type, points gained, etc.


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

    // ── Factory methods ──────────────────────────────────────

    public static GameEvent playerFroze(int attackerId, int victimId, float x, float y) {
        return new GameEvent(EventType.PLAYER_FROZEN, attackerId, victimId, -1, x, y, null);
    }

    public static GameEvent playerUnfroze(int playerId, float x, float y) {
        return new GameEvent(EventType.PLAYER_UNFROZEN, playerId, -1, -1, x, y, null);
    }

    public static GameEvent zoneCaptured(int playerId, int zoneId, float x, float y) {
        return new GameEvent(EventType.ZONE_CAPTURED, playerId, -1, zoneId, x, y, null);
    }

    public static GameEvent zoneLost(int playerId, int zoneId, float x, float y) {
        return new GameEvent(EventType.ZONE_LOST, playerId, -1, zoneId, x, y, null);
    }

    public static GameEvent zoneContested(int zoneId, float x, float y) {
        return new GameEvent(EventType.ZONE_CONTESTED, -1, -1, zoneId, x, y, null);
    }

    public static GameEvent itemCollected(int playerId, int itemId, String itemType, float x, float y) {
        return new GameEvent(EventType.ITEM_COLLECTED, playerId, -1, itemId, x, y, itemType);
    }

    public static GameEvent itemSpawned(int itemId, String itemType, float x, float y) {
        return new GameEvent(EventType.ITEM_SPAWNED, -1, -1, itemId, x, y, itemType);
    }

    public static GameEvent playerJoined(int playerId) {
        return new GameEvent(EventType.PLAYER_JOINED, playerId, -1, -1, 0, 0, null);
    }

    public static GameEvent playerLeft(int playerId) {
        return new GameEvent(EventType.PLAYER_LEFT, playerId, -1, -1, 0, 0, null);
    }

    public static GameEvent gameEnded(int winnerId, String winnerName) {
        return new GameEvent(EventType.GAME_ENDED, winnerId, -1, -1, 0, 0, winnerName);
    }

    public static GameEvent gameStarting() {
        return new GameEvent(EventType.GAME_STARTING, -1, -1, -1, 0, 0, null);
    }


    // ── Getters ──────────────────────────────────────────────

    public EventType getEventType()     { return eventType; }
    public int getSourcePlayerId()      { return sourcePlayerId; }
    public int getTargetPlayerId()      { return targetPlayerId; }
    public int getObjectId()            { return objectId; }
    public float getX()                 { return x; }
    public float getY()                 { return y; }
    public String getExtraData()        { return extraData; }


    @Override
    public String toString() {
        return "GameEvent{" + eventType + ", source=" + sourcePlayerId +
               ", target=" + targetPlayerId + ", at=(" + x + "," + y + ")}";
    }


    public enum EventType {
        PLAYER_FROZEN,
        PLAYER_UNFROZEN,
        ZONE_CAPTURED,
        ZONE_LOST,
        ZONE_CONTESTED,
        ITEM_SPAWNED,
        ITEM_COLLECTED,
        PLAYER_JOINED,
        PLAYER_LEFT,
        GAME_STARTING,
        GAME_ENDED
    }
}
