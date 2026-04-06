import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

/**
 * The big state dump that the server sends to all clients periodically.
 * Contains everything a client needs to render the current game state.
 * 
 * This gets sent over TCP since it's larger and we need reliability.
 */
public class GameStateUpdate implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // All players currently in the game
    private final List<PlayerSnapshot> players;
    
    // All zones and who controls them
    private final List<ZoneSnapshot> zones;
    
    // All items currently on the map
    private final List<ItemSnapshot> items;
    
    // How much time is left in the round (milliseconds)
    private final long timeRemainingMs;
    
    // Server's tick number so clients can detect missed updates
    private final int tickNumber;
    
    
    public GameStateUpdate(List<PlayerSnapshot> players, List<ZoneSnapshot> zones,
                           List<ItemSnapshot> items, long timeRemainingMs, int tickNumber) {
        // Make copies so the original lists can't be modified
        this.players = new ArrayList<>(players);
        this.zones = new ArrayList<>(zones);
        this.items = new ArrayList<>(items);
        this.timeRemainingMs = timeRemainingMs;
        this.tickNumber = tickNumber;
    }
    
    
    public List<PlayerSnapshot> getPlayers() {
        return players;
    }
    
    public List<ZoneSnapshot> getZones() {
        return zones;
    }
    
    public List<ItemSnapshot> getItems() {
        return items;
    }
    
    public long getTimeRemainingMs() {
        return timeRemainingMs;
    }
    
    public int getTickNumber() {
        return tickNumber;
    }
    
    
    // A snapshot of a single player's state at this moment
    public static class PlayerSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final int playerId;
        public final String name;
        public final float x;
        public final float y;
        public final int score;
        public final boolean isFrozen;
        public final boolean hasSpeedBoost;
        public final boolean hasFreezeRay;
        
        public PlayerSnapshot(int playerId, String name, float x, float y, int score,
                              boolean isFrozen, boolean hasSpeedBoost, boolean hasFreezeRay) {
            this.playerId = playerId;
            this.name = name;
            this.x = x;
            this.y = y;
            this.score = score;
            this.isFrozen = isFrozen;
            this.hasSpeedBoost = hasSpeedBoost;
            this.hasFreezeRay = hasFreezeRay;
        }
    }
    
    
    // A snapshot of a zone's state
    public static class ZoneSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final int zoneId;
        public final float x;
        public final float y;
        public final float radius;
        public final int ownerId;        // -1 if unowned
        public final int contestedById;  // -1 if not contested
        public final float captureProgress; // 0.0 to 1.0
        
        public ZoneSnapshot(int zoneId, float x, float y, float radius,
                            int ownerId, int contestedById, float captureProgress) {
            this.zoneId = zoneId;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.ownerId = ownerId;
            this.contestedById = contestedById;
            this.captureProgress = captureProgress;
        }
    }
    
    
    // A snapshot of an item on the map
    public static class ItemSnapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final int itemId;
        public final ItemType type;
        public final float x;
        public final float y;
        
        public ItemSnapshot(int itemId, ItemType type, float x, float y) {
            this.itemId = itemId;
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }
    
    
    // Types of items that can spawn
    public enum ItemType {
        ENERGY,      // gives points
        FREEZE_RAY,  // gives the freeze weapon
        SPEED_BOOST  // temporary speed increase
    }
}
