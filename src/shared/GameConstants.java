package shared;

/**
 * Central constants shared by both server and client.
 * All values can be overridden at runtime via ConfigLoader —
 * these are just safe defaults.
 */
public final class GameConstants {

    private GameConstants() {} // no instances

    // Map dimensions (in grid cells)
    public static final int MAP_WIDTH         = 20;
    public static final int MAP_HEIGHT        = 20;

    // Server tick rate — one game loop iteration every 50ms = 20 ticks/sec
    public static final int TICK_RATE_MS      = 50;

    // How long a player must stand in a zone before capturing it (ms)
    public static final int ZONE_CAPTURE_TIME_MS = 3000;

    // How long a zone waits before resetting when its owner leaves (ms)
    public static final int GRACE_TIMER_MS    = 5000;

    // How long a player stays frozen after being hit (ms)
    public static final int FREEZE_DURATION_MS = 3000;

    // Freeze-ray weapon range (in grid cells)
    public static final int FREEZE_RANGE      = 3;

    // Points awarded per tick to a zone owner
    public static final int ZONE_POINTS_PER_TICK = 1;

    // Points deducted from a player that gets hit by freeze-ray
    public static final int FREEZE_HIT_PENALTY  = 10;

    // Default round length in seconds
    public static final int ROUND_DURATION_SECONDS = 180;

    // Max UDP packet size in bytes (keep under MTU)
    public static final int MAX_UDP_PACKET_BYTES = 1024;

    // How many missed heartbeats before a client is considered dead
    public static final int MAX_MISSED_HEARTBEATS = 5;

    // Heartbeat interval (ms)
    public static final int HEARTBEAT_INTERVAL_MS = 2000;
}
