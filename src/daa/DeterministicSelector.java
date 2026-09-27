package daa;

/**
 * Deterministic selection (BFPRT / Median-of-Medians).
 * <ol>
 *   <li>Split the range into groups of 5, sort each group with insertion sort and
 *       move each group's median to the front of the range.</li>
 *   <li>Recursively select the median of those ⌈n/5⌉ medians — this is the pivot.</li>
 *   <li>In-place 3-way partition around the pivot value.</li>
 *   <li>Recurse only into the side that contains index k (or stop if k hits the
 *       "equal" block).</li>
 * </ol>
 * The pivot is guaranteed to have ≥ ~3n/10 elements on each side, so
 * T(n) ≤ T(n/5) + T(7n/10) + Θ(n) = Θ(n) worst case.
 * <p>
 * The input array is permuted in place.
 */
public final class DeterministicSelector {

    private static final int SMALL = 5;

    private final Metrics metrics;

    public DeterministicSelector() {
        this(new Metrics());
    }

    public DeterministicSelector(Metrics metrics) {
        this.metrics = metrics;
    }

    public Metrics metrics() {
        return metrics;
    }

    /**
     * Returns the k-th smallest element (0-based) of {@code a}. Permutes {@code a}.
     */
    public int select(int[] a, int k) {
        return select(a, 0, a.length - 1, k);
    }

    /** k is an absolute index in [lo, hi]. */
    private int select(int[] a, int lo, int hi, int k) {
        metrics.enter();
        try {
            if (hi - lo < SMALL) {
                insertionSort(a, lo, hi);
                return a[k];
            }
            int pivot = medianOfMedians(a, lo, hi);

            int lt = lo;
            int i = lo;
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
            // [lo, lt) < pivot, [lt, gt] == pivot, (gt, hi] > pivot
            if (k < lt) {
                return select(a, lo, lt - 1, k);
            } else if (k > gt) {
                return select(a, gt + 1, hi, k);
            } else {
                return pivot;
            }
        } finally {
            metrics.exit();
        }
    }

    /** Moves group medians to a[lo..lo+g-1] and returns the median of them. */
    private int medianOfMedians(int[] a, int lo, int hi) {
        int groups = 0;
        for (int start = lo; start <= hi; start += 5) {
            int end = Math.min(start + 4, hi);
            insertionSort(a, start, end);
            int median = start + (end - start) / 2;
            swap(a, lo + groups, median);
            groups++;
        }
        int last = lo + groups - 1;
        return select(a, lo, last, lo + (groups - 1) / 2);
    }

    private void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int x = a[i];
            int j = i - 1;
            while (j >= lo) {
                metrics.addComparison();
                if (a[j] > x) {
                    a[j + 1] = a[j];
                    j--;
                } else {
                    break;
                }
            }
            a[j + 1] = x;
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
