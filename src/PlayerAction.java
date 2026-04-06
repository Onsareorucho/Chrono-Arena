

import java.io.Serializable;

/**
 * Sent from client to server over UDP when the player does an action like
 * using the freeze ray. Separate from movement because actions are less
 * frequent but more important to get right.
 */
public class PlayerAction implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // What kind of action the player is doing
    private final ActionType actionType;
    
    // For targeted actions, which direction are they aiming
    // (normalized to -1, 0, or 1 for simplicity)
    private final int targetDirectionX;
    private final int targetDirectionY;
    
    // Client timestamp for latency calculations
    private final long clientTime;
    
    
    public PlayerAction(ActionType actionType, int targetX, int targetY) {
        this.actionType = actionType;
        this.targetDirectionX = targetX;
        this.targetDirectionY = targetY;
        this.clientTime = System.currentTimeMillis();
    }
    
    // Fire freeze ray in a direction
    public static PlayerAction freezeRay(int dirX, int dirY) {
        return new PlayerAction(ActionType.FREEZE_RAY, dirX, dirY);
    }
    
    // Use a powerup (no direction needed)
    public static PlayerAction usePowerup() {
        return new PlayerAction(ActionType.USE_POWERUP, 0, 0);
    }
    
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public int getTargetDirectionX() {
        return targetDirectionX;
    }
    
    public int getTargetDirectionY() {
        return targetDirectionY;
    }
    
    public long getClientTime() {
        return clientTime;
    }
    
    
    @Override
    public String toString() {
        return "PlayerAction{" + actionType + " -> (" + targetDirectionX + "," + targetDirectionY + ")}";
    }
    
    
    // The different actions a player can take
    public enum ActionType {
        FREEZE_RAY,     // shoot the freeze weapon
        USE_POWERUP     // activate a collected powerup
    }
}
