package shared;

import java.io.Serializable;

/** Direction enum used in movement actions. */
public enum Direction {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT,
    NONE;

    /** Returns the delta-X for this direction (positive = right). */
    public int getDeltaX() {
        return switch (this) {
            case LEFT, UP_LEFT, DOWN_LEFT   -> -1;
            case RIGHT, UP_RIGHT, DOWN_RIGHT -> 1;
            default -> 0;
        };
    }

    /** Returns the delta-Y for this direction (positive = down). */
    public int getDeltaY() {
        return switch (this) {
            case UP, UP_LEFT, UP_RIGHT       -> -1;
            case DOWN, DOWN_LEFT, DOWN_RIGHT ->  1;
            default -> 0;
        };
    }
}
