package shared;


import java.io.Serializable;

/**
 * Sent from client to server over UDP whenever the player moves or changes direction.
 * This is the most frequent message in the game so it needs to be small and fast.
 * 
 * The server doesn't trust the client's position directly - it just uses the
 * direction to calculate where the player should be moving.
 */
public class PlayerInput implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Direction the player wants to move
    // -1 = left/up, 0 = not moving, 1 = right/down
    private final int directionX;
    private final int directionY;
    
    // Client's local timestamp so server can account for latency if needed
    private final long clientTime;
    
    
    public PlayerInput(int directionX, int directionY) {
        this.directionX = directionX;
        this.directionY = directionY;
        this.clientTime = System.currentTimeMillis();
    }
    
    // Standing still
    public static PlayerInput stop() {
        return new PlayerInput(0, 0);
    }
    
    // Moving in a direction
    public static PlayerInput move(int dx, int dy) {
        // Clamp to -1, 0, or 1
        int clampedX = Math.max(-1, Math.min(1, dx));
        int clampedY = Math.max(-1, Math.min(1, dy));
        return new PlayerInput(clampedX, clampedY);
    }
    
    
    public int getDirectionX() {
        return directionX;
    }
    
    public int getDirectionY() {
        return directionY;
    }
    
    public long getClientTime() {
        return clientTime;
    }
    
    public boolean isMoving() {
        return directionX != 0 || directionY != 0;
    }
    
    
    @Override
    public String toString() {
        return "PlayerInput{dx=" + directionX + ", dy=" + directionY + "}";
    }
}
