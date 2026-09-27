package daa;

/**
 * Top-down MergeSort.
 * <ul>
 *   <li>Linear merge of two sorted halves.</li>
 *   <li>One auxiliary buffer allocated once per {@link #sort(int[])} call and reused
 *       by every merge (no allocations inside the recursion).</li>
 *   <li>Insertion sort cut-off for sub-arrays of size &lt;= {@link #CUTOFF}.</li>
 *   <li>Skips the merge when the two halves are already in order
 *       ({@code a[mid-1] <= a[mid]}), which makes sorted input linear-ish.</li>
 * </ul>
 * Recurrence: T(n) = 2T(n/2) + Θ(n) = Θ(n log n). Extra space: Θ(n) buffer + Θ(log n) stack.
 */
public final class MergeSorter {

    public static final int CUTOFF = 16;

    private final Metrics metrics;

    public MergeSorter() {
        this(new Metrics());
    }

    public MergeSorter(Metrics metrics) {
        this.metrics = metrics;
    }

    public Metrics metrics() {
        return metrics;
    }

    public void sort(int[] a) {
        if (a == null || a.length < 2) {
            return;
        }
        int[] buffer = new int[a.length];   // allocated exactly once, reused by all merges
        metrics.addAllocation();
        sort(a, buffer, 0, a.length);
    }

    /** Sorts the half-open range a[lo, hi). */
    private void sort(int[] a, int[] buf, int lo, int hi) {
        metrics.enter();
        try {
            if (hi - lo <= CUTOFF) {
                insertionSort(a, lo, hi);
                return;
            }
            int mid = (lo + hi) >>> 1;
            sort(a, buf, lo, mid);
            sort(a, buf, mid, hi);

            metrics.addComparison();
            if (a[mid - 1] <= a[mid]) {
                return; // halves already in order
            }
            merge(a, buf, lo, mid, hi);
        } finally {
            metrics.exit();
        }
    }

    /**
     * Linear merge. Only the left half is copied into the buffer; the right half is
     * read in place, so every merge does at most (hi - lo) writes.
     */
    private void merge(int[] a, int[] buf, int lo, int mid, int hi) {
        int leftLen = mid - lo;
        System.arraycopy(a, lo, buf, lo, leftLen);
        int i = lo;       // index in buf (left half)
        int j = mid;      // index in a   (right half)
        int k = lo;       // write index
        int leftEnd = mid;
        while (i < leftEnd && j < hi) {
            metrics.addComparison();
            if (buf[i] <= a[j]) {        // <= keeps the sort stable
                a[k++] = buf[i++];
            } else {
                a[k++] = a[j++];
            }
        }
        while (i < leftEnd) {
            a[k++] = buf[i++];
        }
        // any remaining right elements are already in place
    }

    private void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i < hi; i++) {
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
}
