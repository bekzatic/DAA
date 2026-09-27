package daa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** MergeSort and QuickSort are checked against Arrays.sort(). */
class SortingTest {

    private static final Random RND = new Random(12345);

    private static void checkAgainstJdk(Consumer<int[]> sorter, int[] input) {
        int[] expected = input.clone();
        Arrays.sort(expected);
        int[] actual = input.clone();
        sorter.accept(actual);
        assertArrayEquals(expected, actual);
    }

    private static void runAllCases(Consumer<int[]> sorter) {
        checkAgainstJdk(sorter, new int[0]);                       // empty
        checkAgainstJdk(sorter, new int[]{42});                    // single element
        checkAgainstJdk(sorter, new int[]{2, 1});                  // two elements
        checkAgainstJdk(sorter, new int[]{7, 7, 7, 7, 7, 7, 7});   // all equal
        checkAgainstJdk(sorter, new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1});
        for (int n : new int[]{3, 10, 16, 17, 33, 100, 1_000, 10_000, 100_000}) {
            for (Experiment.InputType t : Experiment.InputType.values()) {
                checkAgainstJdk(sorter, Experiment.generateArray(t, n, RND));
            }
        }
        for (int trial = 0; trial < 200; trial++) {                // many small random arrays
            int n = RND.nextInt(64);
            int[] a = new int[n];
            for (int i = 0; i < n; i++) a[i] = RND.nextInt(20) - 10;
            checkAgainstJdk(sorter, a);
        }
    }

    @Test
    @DisplayName("MergeSort matches Arrays.sort on all input types")
    void mergeSortMatchesJdk() {
        runAllCases(a -> new MergeSorter().sort(a));
    }

    @Test
    @DisplayName("QuickSort matches Arrays.sort on all input types")
    void quickSortMatchesJdk() {
        runAllCases(a -> new QuickSorter().sort(a));
    }

    @Test
    @DisplayName("null input is ignored")
    void nullIsIgnored() {
        new MergeSorter().sort(null);
        new QuickSorter().sort(null);
    }

    @Test
    @DisplayName("MergeSort depth is ~log2(n/cutoff)")
    void mergeSortDepthIsLogarithmic() {
        int n = 1 << 16;
        Metrics m = new Metrics();
        new MergeSorter(m).sort(Experiment.generateArray(Experiment.InputType.RANDOM, n, RND));
        int bound = (int) Math.ceil(Math.log((double) n / MergeSorter.CUTOFF) / Math.log(2)) + 2;
        assertTrue(m.maxDepth() <= bound, "depth " + m.maxDepth() + " > " + bound);
    }

    @Test
    @DisplayName("QuickSort depth <= floor(log2 n) + 1 (smaller-first recursion)")
    void quickSortDepthIsBounded() {
        for (Experiment.InputType t : Experiment.InputType.values()) {
            int n = 200_000;
            Metrics m = new Metrics();
            new QuickSorter(m, new Random(1)).sort(Experiment.generateArray(t, n, RND));
            int bound = (int) Math.floor(Math.log(n) / Math.log(2)) + 1;
            assertTrue(m.maxDepth() <= bound, t + ": depth " + m.maxDepth() + " > " + bound);
        }
    }
}
