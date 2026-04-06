package shared;

/**
 * Enumerates every action a player can perform during a match.
 *
 * Used by:
 *   - ActionMessage.ActionType  (networking layer — the wire representation)
 *   - ActionProcessor           (server logic — processes queued actions)
 *   - InputHandler              (client — maps key presses to actions)
 *
 * NOTE: ActionMessage.ActionType mirrors this enum for the wire format.
 * They are intentionally separate so the networking layer (shared.protocol)
 * does not need to know about server-side game logic, and vice versa.
 * ActionProcessor maps one to the other when it reads from the queue.
 */
public enum PlayerAction {

    /**
     * Move the player one cell in a Direction.
     * Accompanied by a Direction value in the ActionMessage.
     */
    MOVE,

    /**
     * Fire the freeze-ray at the nearest player within FREEZE_RANGE.
     * Subject to cooldown (tracked per-player in Player.java).
     */
    ATTACK,

    /**
     * Explicitly collect an item at the player's current position.
     * In many designs the server handles pickup automatically on zone entry,
     * but this action supports designs requiring a button press.
     */
    COLLECT,

    /**
     * Activate a held power-up (speed boost, etc.).
     * The specific effect depends on what is in the player's inventory.
     */
    USE_ABILITY
}
