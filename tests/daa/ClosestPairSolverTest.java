package daa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** For n <= 2000 the D&C result is compared with the O(n²) brute force. */
class ClosestPairSolverTest {

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("matches brute force for random sets, n <= 2000")
    void matchesBruteForceRandom() {
        Random rnd = new Random(77);
        for (int trial = 0; trial < 200; trial++) {
            int n = 2 + rnd.nextInt(trial < 190 ? 300 : 1_999);
            Point[] pts = Experiment.generatePoints(Experiment.InputType.RANDOM, n, rnd);
            double fast = new ClosestPairSolver().solve(pts).distance();
            double slow = ClosestPairSolver.bruteForce(pts).distance();
            assertEquals(slow, fast, EPS, "n=" + n);
        }
    }

    @Test
    @DisplayName("matches brute force with duplicates / coincident points")
    void matchesBruteForceDuplicates() {
        Random rnd = new Random(78);
        for (int n : new int[]{2, 3, 4, 10, 100, 1_000, 2_000}) {
            Point[] pts = Experiment.generatePoints(Experiment.InputType.DUPLICATES, n, rnd);
            assertEquals(ClosestPairSolver.bruteForce(pts).distance(),
                    new ClosestPairSolver().solve(pts).distance(), EPS);
        }
    }

    @Test
    @DisplayName("points on a vertical line and on a horizontal line")
    void collinear() {
        Point[] vertical = new Point[500];
        Point[] horizontal = new Point[500];
        for (int i = 0; i < 500; i++) {
            vertical[i] = new Point(0, i * i);
            horizontal[i] = new Point(i * 3.5, 1);
        }
        assertEquals(1.0, new ClosestPairSolver().solve(vertical).distance(), EPS);
        assertEquals(3.5, new ClosestPairSolver().solve(horizontal).distance(), EPS);
    }

    @Test
    @DisplayName("two points; returned pair really has the reported distance")
    void twoPointsAndConsistency() {
        Point a = new Point(0, 0);
        Point b = new Point(3, 4);
        ClosestPairSolver.Result r = new ClosestPairSolver().solve(new Point[]{a, b});
        assertEquals(5.0, r.distance(), EPS);
        assertEquals(r.distance(), r.p().distanceTo(r.q()), EPS);
    }

    @Test
    @DisplayName("large input (n = 200000) runs fast and depth is logarithmic")
    void largeInput() {
        Random rnd = new Random(5);
        int n = 200_000;
        Point[] pts = Experiment.generatePoints(Experiment.InputType.RANDOM, n, rnd);
        Metrics m = new Metrics();
        ClosestPairSolver.Result r = new ClosestPairSolver(m).solve(pts);
        assertTrue(r.distance() >= 0);
        assertTrue(m.maxDepth() <= 20, "depth=" + m.maxDepth());
    }
}
