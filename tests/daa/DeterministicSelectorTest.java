package daa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Deterministic select is checked against Arrays.sort(a)[k]. */
class DeterministicSelectorTest {

    private static int reference(int[] a, int k) {
        int[] c = a.clone();
        Arrays.sort(c);
        return c[k];
    }

    @Test
    @DisplayName("1000 random trials match Arrays.sort(a)[k]")
    void randomTrials() {
        Random rnd = new Random(2024);
        for (int trial = 0; trial < 1_000; trial++) {
            int n = 1 + rnd.nextInt(trial < 900 ? 200 : 20_000);
            int[] a = new int[n];
            int range = rnd.nextBoolean() ? Integer.MAX_VALUE : 10;   // half of trials duplicate-heavy
            for (int i = 0; i < n; i++) a[i] = rnd.nextInt(range);
            int k = rnd.nextInt(n);
            int expected = reference(a, k);
            int actual = new DeterministicSelector().select(a.clone(), k);
            assertEquals(expected, actual, "trial " + trial + ", n=" + n + ", k=" + k);
        }
    }

    @Test
    @DisplayName("every k on structured inputs")
    void everyKOnStructuredInputs() {
        Random rnd = new Random(1);
        for (Experiment.InputType t : Experiment.InputType.values()) {
            int[] a = Experiment.generateArray(t, 257, rnd);
            for (int k = 0; k < a.length; k++) {
                assertEquals(reference(a, k), new DeterministicSelector().select(a.clone(), k));
            }
        }
    }

    @Test
    @DisplayName("single element and all-equal arrays")
    void edgeCases() {
        assertEquals(5, new DeterministicSelector().select(new int[]{5}, 0));
        int[] same = new int[1000];
        Arrays.fill(same, 3);
        assertEquals(3, new DeterministicSelector().select(same, 500));
    }

    @Test
    @DisplayName("comparisons grow linearly: ratio per element stays bounded")
    void linearComparisons() {
        Random rnd = new Random(9);
        double maxRatio = 0;
        for (int n : new int[]{10_000, 100_000, 1_000_000}) {
            Metrics m = new Metrics();
            new DeterministicSelector(m).select(
                    Experiment.generateArray(Experiment.InputType.RANDOM, n, rnd), n / 2);
            maxRatio = Math.max(maxRatio, (double) m.comparisons() / n);
        }
        assertTrue(maxRatio < 40, "comparisons/n = " + maxRatio);
    }
}
