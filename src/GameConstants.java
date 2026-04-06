

public final class GameConstants {

    // Player settings
    
    // How fast players move (units per game tick)
    public static final float PLAYER_SPEED = 1.0f;
    
    // Speed boost multiplier when player has the powerup
    public static final float SPEED_BOOST_MULTIPLIER = 1.5f;
    
    // How long the speed boost lasts (in milliseconds)
    public static final long SPEED_BOOST_DURATION_MS = 5000;
    
    // How long a player stays frozen when hit by freeze ray (in milliseconds)
    public static final long FREEZE_DURATION_MS = 3000;
    

    // Zone capture mechanics
    
    // How long you need to stay in a zone to capture it (in milliseconds)
    public static final long ZONE_CAPTURE_TIME_MS = 3000;
    
    // Points earned per second while holding a zone
    public static final int ZONE_POINTS_PER_SECOND = 10;
    
    // Grace period before losing zone control after leaving (in milliseconds)
    // Set to 0 for instant loss, or 5000 for 5-second grace period
    public static final long ZONE_GRACE_PERIOD_MS = 5000;
    
    // How big each zone is (radius in game units)
    public static final float ZONE_RADIUS = 2.0f;
    

    // Items and pickups
    
    // How often new items spawn (in milliseconds)
    public static final long ITEM_SPAWN_INTERVAL_MS = 10000;
    
    // Points gained from collecting energy
    public static final int ENERGY_POINTS = 50;
    
    // Max items that can exist on the map at once
    public static final int MAX_ITEMS_ON_MAP = 5;
    
    // How close you need to be to pick up an item (radius in game units)
    public static final float ITEM_PICKUP_RADIUS = 0.5f;
    

    // Freeze ray weapon
    
    // Range of freeze ray attack (in game units)
    public static final float FREEZE_RAY_RANGE = 3.0f;
    
    // Cooldown between freeze ray uses (in milliseconds)
    public static final long FREEZE_RAY_COOLDOWN_MS = 8000;
    
    // Points deducted from frozen player
    public static final int FREEZE_RAY_POINT_PENALTY = 25;
    

    // UDP / TCP network settings
    
    // Max size of a UDP packet (bytes) - keep it under MTU to avoid fragmentation
    public static final int MAX_UDP_PACKET_SIZE = 512;
    
    // Max size of a TCP message (bytes)
    public static final int MAX_TCP_MESSAGE_SIZE = 4096;
    
    // How often server sends state updates to clients (in milliseconds)
    public static final long SERVER_BROADCAST_INTERVAL_MS = 50;
    
    // Timeout for considering a client disconnected (in milliseconds)
    public static final long CLIENT_TIMEOUT_MS = 10000;
    

    // Map settings
    
    // Default map dimensions (can be overridden by config)
    public static final int DEFAULT_MAP_WIDTH = 20;
    public static final int DEFAULT_MAP_HEIGHT = 20;
    
    // Number of control zones in the arena
    public static final int NUM_ZONES = 3;
    

    // General game settings
    
    // Default round time (in seconds, can be overridden by config)
    public static final int DEFAULT_GAME_DURATION_SECONDS = 180;
    
    // Max players allowed in a game
    public static final int MAX_PLAYERS = 8;
    
    // Min players required to start
    public static final int MIN_PLAYERS = 2;


    // Private constructor
    private GameConstants() {
        throw new UnsupportedOperationException("GameConstants is a utility class");
    }
}
