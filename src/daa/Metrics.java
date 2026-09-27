package daa;

/**
 * Collects operation counters for one algorithm run.
 * <p>
 * Recursion depth is tracked with {@link #enter()} / {@link #exit()} which every
 * recursive method calls on entry and in a {@code finally} block on exit.
 */
public final class Metrics {

    private long comparisons;
    private long swaps;
    private long allocations;
    private long recursiveCalls;
    private int depth;
    private int maxDepth;

    public void enter() {
        recursiveCalls++;
        depth++;
        if (depth > maxDepth) {
            maxDepth = depth;
        }
    }

    public void exit() {
        depth--;
    }

    public void addComparison() { comparisons++; }
    public void addComparisons(long c) { comparisons += c; }
    public void addSwap() { swaps++; }
    public void addAllocation() { allocations++; }

    public long comparisons() { return comparisons; }
    public long swaps() { return swaps; }
    public long allocations() { return allocations; }
    public long recursiveCalls() { return recursiveCalls; }
    public int maxDepth() { return maxDepth; }

    public void reset() {
        comparisons = swaps = allocations = recursiveCalls = 0;
        depth = maxDepth = 0;
    }

    @Override
    public String toString() {
        return String.format("comparisons=%d, swaps=%d, allocations=%d, calls=%d, maxDepth=%d",
                comparisons, swaps, allocations, recursiveCalls, maxDepth);
    }
}
