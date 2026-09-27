package daa;

/**
 * Immutable 2D point used by {@link ClosestPairSolver}.
 */
public record Point(double x, double y) {

    /** Euclidean distance between this point and {@code other}. */
    public double distanceTo(Point other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
