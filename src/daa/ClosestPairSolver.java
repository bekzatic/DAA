package daa;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Closest pair of points in the plane, divide-and-conquer.
 * <ol>
 *   <li>Sort points by x once (Θ(n log n)).</li>
 *   <li>Split at the median x, solve both halves recursively → δ = min(δL, δR).</li>
 *   <li>Each recursive call also returns its points sorted by y (merge step, like
 *       MergeSort), so no re-sorting is needed at every level.</li>
 *   <li>Build the strip of points with |x − midX| &lt; δ in y-order and compare each
 *       point only with following points while Δy &lt; δ (at most 7 of them).</li>
 * </ol>
 * Recurrence: T(n) = 2T(n/2) + Θ(n) = Θ(n log n).
 */
public final class ClosestPairSolver {

    private static final int BRUTE_FORCE_THRESHOLD = 3;
    private static final Comparator<Point> BY_X =
            Comparator.comparingDouble(Point::x).thenComparingDouble(Point::y);

    /** Result of a closest-pair query. */
    public record Result(Point p, Point q, double distance) { }

    private final Metrics metrics;

    private double best;
    private Point bestP;
    private Point bestQ;

    public ClosestPairSolver() {
        this(new Metrics());
    }

    public ClosestPairSolver(Metrics metrics) {
        this.metrics = metrics;
    }

    public Metrics metrics() {
        return metrics;
    }

    /** Divide-and-conquer Θ(n log n). The input array is not modified. */
    public Result solve(Point[] points) {
        if (points == null || points.length < 2) {
            throw new IllegalArgumentException("need at least two points");
        }
        for (Point p : points) {
            if (p == null) {
                throw new IllegalArgumentException("points must not contain null");
            }
        }
        Point[] pts = points.clone();
        metrics.addAllocation();
        Arrays.sort(pts, BY_X);
        Point[] aux = new Point[pts.length];
        metrics.addAllocation();

        best = Double.POSITIVE_INFINITY;
        bestP = bestQ = null;
        solve(pts, aux, 0, pts.length);
        return new Result(bestP, bestQ, best);
    }

    /** Solves pts[lo, hi); on return that range is sorted by y. */
    private void solve(Point[] pts, Point[] aux, int lo, int hi) {
        metrics.enter();
        try {
            int n = hi - lo;
            if (n <= BRUTE_FORCE_THRESHOLD) {
                for (int i = lo; i < hi; i++) {
                    for (int j = i + 1; j < hi; j++) {
                        consider(pts[i], pts[j]);
                    }
                }
                insertionSortByY(pts, lo, hi);
                return;
            }

            int mid = (lo + hi) >>> 1;
            double midX = pts[mid].x();   // read before the halves get re-ordered by y

            solve(pts, aux, lo, mid);
            solve(pts, aux, mid, hi);

            mergeByY(pts, aux, lo, mid, hi);

            // strip: points close to the dividing line, already in y-order
            int s = 0;
            for (int i = lo; i < hi; i++) {
                if (Math.abs(pts[i].x() - midX) < best) {
                    aux[s++] = pts[i];
                }
            }
            for (int i = 0; i < s; i++) {
                for (int j = i + 1; j < s; j++) {
                    metrics.addComparison();
                    if (aux[j].y() - aux[i].y() >= best) {
                        break;
                    }
                    consider(aux[i], aux[j]);
                }
            }
        } finally {
            metrics.exit();
        }
    }

    private void consider(Point p, Point q) {
        metrics.addComparison();
        double d = p.distanceTo(q);
        if (d < best) {
            best = d;
            bestP = p;
            bestQ = q;
        }
    }

    private void mergeByY(Point[] pts, Point[] aux, int lo, int mid, int hi) {
        System.arraycopy(pts, lo, aux, lo, hi - lo);
        int i = lo;
        int j = mid;
        for (int k = lo; k < hi; k++) {
            if (i >= mid) {
                pts[k] = aux[j++];
            } else if (j >= hi) {
                pts[k] = aux[i++];
            } else if (aux[j].y() < aux[i].y()) {
                pts[k] = aux[j++];
            } else {
                pts[k] = aux[i++];
            }
        }
    }

    private static void insertionSortByY(Point[] a, int lo, int hi) {
        for (int i = lo + 1; i < hi; i++) {
            Point x = a[i];
            int j = i - 1;
            while (j >= lo && a[j].y() > x.y()) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = x;
        }
    }

    /** O(n²) reference implementation used for testing and comparison. */
    public static Result bruteForce(Point[] points) {
        if (points == null || points.length < 2) {
            throw new IllegalArgumentException("need at least two points");
        }
        double best = Double.POSITIVE_INFINITY;
        Point bp = null;
        Point bq = null;
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                double d = points[i].distanceTo(points[j]);
                if (d < best) {
                    best = d;
                    bp = points[i];
                    bq = points[j];
                }
            }
        }
        return new Result(bp, bq, best);
    }
}
