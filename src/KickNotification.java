import java.io.Serializable;

/**
 * Sent from server to a client when they're being kicked.
 * This is for the KILL_SWITCH requirement - server can boot erratic clients.
 */
public class KickNotification implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String reason;
    private final boolean canRejoin;
    
    
    public KickNotification(String reason, boolean canRejoin) {
        this.reason = reason;
        this.canRejoin = canRejoin;
    }
    
    // Common kick reasons
    public static KickNotification serverShuttingDown() {
        return new KickNotification("Server is shutting down", false);
    }
    
    public static KickNotification tooManyPackets() {
        return new KickNotification("Sending too many packets (possible exploit)", false);
    }
    
    public static KickNotification invalidInput() {
        return new KickNotification("Invalid game input detected", true);
    }
    
    public static KickNotification timeout() {
        return new KickNotification("Connection timed out", true);
    }
    
    public static KickNotification adminKick(String reason) {
        return new KickNotification("Kicked by admin: " + reason, false);
    }
    
    
    public String getReason() {
        return reason;
    }
    
    public boolean canRejoin() {
        return canRejoin;
    }
    
    
    @Override
    public String toString() {
        return "Kicked: " + reason + (canRejoin ? " (can rejoin)" : " (banned)");
    }
}
