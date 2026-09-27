package daa;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Benchmark harness: runs every algorithm on several sizes and input types,
 * measures time with {@link System#nanoTime()}, records metrics and writes CSV.
 * <p>
 * Each configuration is executed {@code reps} times on a fresh copy of the same input;
 * the reported time is the <b>median</b> (robust to GC pauses / JIT hiccups).
 * A warm-up phase runs first so that the JIT has compiled the hot methods.
 */
public final class Experiment {

    public enum InputType { RANDOM, SORTED, REVERSED, DUPLICATES }

    /** One CSV row. */
    public record Row(String algorithm, InputType type, int n, double timeMs,
                      int maxDepth, long comparisons, long swaps,
                      long allocations, long recursiveCalls) {

        static final String HEADER =
                "algorithm,input_type,size_class,n,time_ms,max_depth,comparisons,swaps,allocations,recursive_calls";

        String toCsv() {
            return String.format(Locale.ROOT, "%s,%s,%s,%d,%.4f,%d,%d,%d,%d,%d",
                    algorithm, type, sizeClass(n), n, timeMs, maxDepth,
                    comparisons, swaps, allocations, recursiveCalls);
        }
    }

    private static final long SEED = 42L;

    private final int[] sizes;
    private final int[] bruteForceSizes;
    private final int reps;
    private final List<Row> rows = new ArrayList<>();

    public Experiment(int[] sizes, int[] bruteForceSizes, int reps) {
        this.sizes = sizes;
        this.bruteForceSizes = bruteForceSizes;
        this.reps = reps;
    }

    public static Experiment full() {
        return new Experiment(
                new int[]{1_000, 5_000, 10_000, 50_000, 100_000, 500_000, 1_000_000},
                new int[]{500, 1_000, 2_000, 5_000, 10_000},
                5);
    }

    public static Experiment quick() {
        return new Experiment(new int[]{1_000, 10_000, 100_000}, new int[]{500, 1_000, 2_000}, 3);
    }

    public static String sizeClass(int n) {
        if (n <= 10_000) return "small";
        if (n <= 100_000) return "medium";
        return "large";
    }

    // ------------------------------------------------------------------ inputs

    public static int[] generateArray(InputType type, int n, Random rnd) {
        int[] a = new int[n];
        switch (type) {
            case RANDOM -> { for (int i = 0; i < n; i++) a[i] = rnd.nextInt(); }
            case SORTED -> { for (int i = 0; i < n; i++) a[i] = i; }
            case REVERSED -> { for (int i = 0; i < n; i++) a[i] = n - i; }
            case DUPLICATES -> { for (int i = 0; i < n; i++) a[i] = rnd.nextInt(10); }
        }
        return a;
    }

    public static Point[] generatePoints(InputType type, int n, Random rnd) {
        Point[] p = new Point[n];
        for (int i = 0; i < n; i++) {
            if (type == InputType.DUPLICATES) {
                // integer grid with few distinct coordinates -> many coincident points
                int side = Math.max(2, (int) Math.sqrt(n) / 2);
                p[i] = new Point(rnd.nextInt(side), rnd.nextInt(side));
            } else {
                p[i] = new Point(rnd.nextDouble() * 1_000_000, rnd.nextDouble() * 1_000_000);
            }
        }
        return p;
    }

    // ------------------------------------------------------------------ running

    public List<Row> run() {
        warmUp();
        InputType[] arrayTypes = InputType.values();
        for (int n : sizes) {
            for (InputType t : arrayTypes) {
                int[] base = generateArray(t, n, new Random(SEED + n));
                rows.add(measureMergeSort(base, t));
                rows.add(measureQuickSort(base, t));
                rows.add(measureArraysSort(base, t));
                rows.add(measureSelect(base, t));
            }
            for (InputType t : new InputType[]{InputType.RANDOM, InputType.DUPLICATES}) {
                Point[] pts = generatePoints(t, n, new Random(SEED + n));
                rows.add(measureClosest(pts, t));
            }
            System.out.printf("  finished n = %,d%n", n);
        }
        for (int n : bruteForceSizes) {
            Point[] pts = generatePoints(InputType.RANDOM, n, new Random(SEED + n));
            rows.add(measureClosest(pts, InputType.RANDOM));
            rows.add(measureBruteForce(pts));
        }
        return rows;
    }

    private void warmUp() {
        Random rnd = new Random(7);
        for (int r = 0; r < 20; r++) {
            int[] a = generateArray(InputType.RANDOM, 20_000, rnd);
            new MergeSorter().sort(a.clone());
            new QuickSorter().sort(a.clone());
            new DeterministicSelector().select(a.clone(), a.length / 2);
            Arrays.sort(a.clone());
            new ClosestPairSolver().solve(generatePoints(InputType.RANDOM, 5_000, rnd));
        }
    }

    private Row measureMergeSort(int[] base, InputType t) {
        double[] times = new double[reps];
        Metrics m = null;
        for (int r = 0; r < reps; r++) {
            int[] a = base.clone();
            m = new Metrics();
            MergeSorter s = new MergeSorter(m);
            long t0 = System.nanoTime();
            s.sort(a);
            times[r] = (System.nanoTime() - t0) / 1e6;
        }
        return row("MergeSort", t, base.length, times, m);
    }

    private Row measureQuickSort(int[] base, InputType t) {
        double[] times = new double[reps];
        Metrics m = null;
        int worstDepth = 0;
        for (int r = 0; r < reps; r++) {
            int[] a = base.clone();
            m = new Metrics();
            QuickSorter s = new QuickSorter(m, new Random(SEED + r));
            long t0 = System.nanoTime();
            s.sort(a);
            times[r] = (System.nanoTime() - t0) / 1e6;
            worstDepth = Math.max(worstDepth, m.maxDepth());
        }
        Row row = row("QuickSort", t, base.length, times, m);
        return new Row(row.algorithm(), t, row.n(), row.timeMs(), worstDepth,
                row.comparisons(), row.swaps(), row.allocations(), row.recursiveCalls());
    }

    private Row measureArraysSort(int[] base, InputType t) {
        double[] times = new double[reps];
        for (int r = 0; r < reps; r++) {
            int[] a = base.clone();
            long t0 = System.nanoTime();
            Arrays.sort(a);
            times[r] = (System.nanoTime() - t0) / 1e6;
        }
        return new Row("ArraysSort", t, base.length, median(times), 0, 0, 0, 0, 0);
    }

    private Row measureSelect(int[] base, InputType t) {
        double[] times = new double[reps];
        Metrics m = null;
        for (int r = 0; r < reps; r++) {
            int[] a = base.clone();
            m = new Metrics();
            DeterministicSelector s = new DeterministicSelector(m);
            long t0 = System.nanoTime();
            s.select(a, a.length / 2);
            times[r] = (System.nanoTime() - t0) / 1e6;
        }
        return row("Select", t, base.length, times, m);
    }

    private Row measureClosest(Point[] pts, InputType t) {
        double[] times = new double[reps];
        Metrics m = null;
        for (int r = 0; r < reps; r++) {
            m = new Metrics();
            ClosestPairSolver s = new ClosestPairSolver(m);
            long t0 = System.nanoTime();
            s.solve(pts);
            times[r] = (System.nanoTime() - t0) / 1e6;
        }
        return row("ClosestPair", t, pts.length, times, m);
    }

    private Row measureBruteForce(Point[] pts) {
        int r2 = Math.max(1, reps / 2);
        double[] times = new double[r2];
        for (int r = 0; r < r2; r++) {
            long t0 = System.nanoTime();
            ClosestPairSolver.bruteForce(pts);
            times[r] = (System.nanoTime() - t0) / 1e6;
        }
        long n = pts.length;
        return new Row("ClosestPairBrute", InputType.RANDOM, pts.length, median(times),
                0, n * (n - 1) / 2, 0, 0, 0);
    }

    private static Row row(String name, InputType t, int n, double[] times, Metrics m) {
        return new Row(name, t, n, median(times), m.maxDepth(), m.comparisons(),
                m.swaps(), m.allocations(), m.recursiveCalls());
    }

    private static double median(double[] xs) {
        double[] c = xs.clone();
        Arrays.sort(c);
        int mid = c.length / 2;
        return c.length % 2 == 1 ? c[mid] : (c[mid - 1] + c[mid]) / 2.0;
    }

    // ------------------------------------------------------------------ output

    public void writeCsv(Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path))) {
            w.println(Row.HEADER);
            for (Row r : rows) {
                w.println(r.toCsv());
            }
        }
    }

    public void printSummary() {
        System.out.printf("%-17s %-10s %10s %12s %9s %15s%n",
                "algorithm", "input", "n", "time(ms)", "depth", "comparisons");
        System.out.println("-".repeat(78));
        for (Row r : rows) {
            System.out.printf(Locale.ROOT, "%-17s %-10s %,10d %12.3f %9d %,15d%n",
                    r.algorithm(), r.type(), r.n(), r.timeMs(), r.maxDepth(), r.comparisons());
        }
    }
}
