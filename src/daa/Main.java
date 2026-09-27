package daa;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/**
 * Entry point.
 * <pre>
 *   java -cp target/classes daa.Main              # demo + full experiment
 *   java -cp target/classes daa.Main --quick      # demo + small experiment
 *   java -cp target/classes daa.Main --demo       # demo only
 * </pre>
 * Results are written to {@code results/results.csv}.
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        boolean quick = Arrays.asList(args).contains("--quick");
        boolean demoOnly = Arrays.asList(args).contains("--demo");

        demo();
        if (demoOnly) {
            return;
        }

        System.out.println();
        System.out.println("=== Running experiments (" + (quick ? "quick" : "full") + ") ===");
        Experiment exp = quick ? Experiment.quick() : Experiment.full();
        long t0 = System.nanoTime();
        exp.run();
        Path out = Path.of("results", "results.csv");
        exp.writeCsv(out);
        System.out.printf("Done in %.1f s. CSV written to %s%n%n",
                (System.nanoTime() - t0) / 1e9, out.toAbsolutePath());
        exp.printSummary();
    }

    private static void demo() {
        Random rnd = new Random(2026);
        System.out.println("=== Divide-and-Conquer demo ===");

        int[] a = new int[20];
        for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(100);
        System.out.println("Input array     : " + Arrays.toString(a));

        int[] ms = a.clone();
        MergeSorter merge = new MergeSorter();
        merge.sort(ms);
        System.out.println("MergeSort       : " + Arrays.toString(ms));
        System.out.println("                  " + merge.metrics());

        int[] qs = a.clone();
        QuickSorter quick = new QuickSorter(new Metrics(), new Random(1));
        quick.sort(qs);
        System.out.println("QuickSort       : " + Arrays.toString(qs));
        System.out.println("                  " + quick.metrics());

        int k = a.length / 2;
        DeterministicSelector sel = new DeterministicSelector();
        int kth = sel.select(a.clone(), k);
        int[] ref = a.clone();
        Arrays.sort(ref);
        System.out.printf("Select k=%d      : %d (Arrays.sort reference: %d)%n", k, kth, ref[k]);
        System.out.println("                  " + sel.metrics());

        Point[] pts = Experiment.generatePoints(Experiment.InputType.RANDOM, 1_000, rnd);
        ClosestPairSolver cps = new ClosestPairSolver();
        ClosestPairSolver.Result fast = cps.solve(pts);
        ClosestPairSolver.Result brute = ClosestPairSolver.bruteForce(pts);
        System.out.printf("ClosestPair n=1000: d=%.6f between %s and %s%n",
                fast.distance(), fast.p(), fast.q());
        System.out.printf("Brute force check : d=%.6f  -> %s%n", brute.distance(),
                Double.compare(fast.distance(), brute.distance()) == 0 ? "MATCH" : "MISMATCH");
        System.out.println("                  " + cps.metrics());
    }
}
