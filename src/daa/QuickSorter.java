package daa;

import java.util.Random;

/**
 * Randomized QuickSort.
 * <ul>
 *   <li>Uniformly random pivot.</li>
 *   <li>In-place 3-way (Dijkstra) partition: &lt; pivot | == pivot | &gt; pivot.
 *       Equal keys are never recursed on, so duplicate-heavy input stays fast.</li>
 *   <li>Recurses into the <b>smaller</b> side and loops over the larger one, so the
 *       stack depth is at most ⌊log₂ n⌋ + 1 even in the worst case.</li>
 * </ul>
 * Expected time Θ(n log n); worst case O(n²) (probability vanishingly small).
 */
public final class QuickSorter {

    private final Metrics metrics;
    private final Random random;

    public QuickSorter() {
        this(new Metrics(), new Random());
    }

    public QuickSorter(Metrics metrics, Random random) {
        this.metrics = metrics;
        this.random = random;
    }

    public Metrics metrics() {
        return metrics;
    }

    public void sort(int[] a) {
        if (a == null || a.length < 2) {
            return;
        }
        sort(a, 0, a.length - 1);
    }

    /** Sorts the closed range a[lo..hi]. */
    private void sort(int[] a, int lo, int hi) {
        metrics.enter();
        try {
            while (lo < hi) {
                int p = lo + random.nextInt(hi - lo + 1);
                swap(a, lo, p);
                int pivot = a[lo];

                // 3-way partition: [lo, lt) < pivot, [lt, gt] == pivot, (gt, hi] > pivot
                int lt = lo;
                int i = lo + 1;
                int gt = hi;
                while (i <= gt) {
                    metrics.addComparison();
                    if (a[i] < pivot) {
                        swap(a, lt++, i++);
                    } else {
                        metrics.addComparison();
                        if (a[i] > pivot) {
                            swap(a, i, gt--);
                        } else {
                            i++;
                        }
                    }
                }

                int leftSize = lt - lo;
                int rightSize = hi - gt;
                if (leftSize < rightSize) {
                    sort(a, lo, lt - 1);   // recurse on smaller part
                    lo = gt + 1;           // iterate on larger part
                } else {
                    sort(a, gt + 1, hi);
                    hi = lt - 1;
                }
            }
        } finally {
            metrics.exit();
        }
    }

    private void swap(int[] a, int i, int j) {
        if (i != j) {
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
            metrics.addSwap();
        }
    }
}
