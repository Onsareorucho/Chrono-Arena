package shared;

import java.io.Serializable;
import java.util.Objects;

/** Simple 2D grid coordinate. Immutable and serializable. */
public class Position implements Serializable {

    private static final long serialVersionUID = 1L;

    public final int x;
    public final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Returns a new Position offset by dx/dy. */
    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    /** Chebyshev (chessboard) distance — works well for grid-based range checks. */
    public int distanceTo(Position other) {
        return Math.max(Math.abs(x - other.x), Math.abs(y - other.y));
    }

    /** Manhattan distance — useful for movement cost. */
    public int manhattanDistance(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position p)) return false;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
