"""Builds all plots for the README from results/results.csv.

Usage (from the repository root):
    python3 scripts/plot_results.py
Requires: pandas, matplotlib
"""
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parent.parent
CSV = ROOT / "results" / "results.csv"
OUT = ROOT / "docs" / "plots"
OUT.mkdir(parents=True, exist_ok=True)

df = pd.read_csv(CSV)
main = df[~df.algorithm.isin(["ClosestPairDC", "ClosestPairBrute"])]
ALGOS = ["MergeSort", "QuickSort", "Select", "ClosestPair"]
TYPES = ["RANDOM", "SORTED", "REVERSED", "DUPLICATES"]
MARK = {"MergeSort": "o", "QuickSort": "s", "Select": "^", "ClosestPair": "D", "ArraysSort": "x"}

plt.rcParams.update({"figure.dpi": 130, "axes.grid": True, "grid.alpha": 0.3})


def save(fig, name):
    fig.tight_layout()
    fig.savefig(OUT / name)
    plt.close(fig)
    print("wrote", OUT / name)


# 1. Time vs n (random input, log-log) ---------------------------------------
fig, ax = plt.subplots(figsize=(8, 5.5))
rnd = main[main.input_type == "RANDOM"]
for alg in ALGOS + ["ArraysSort"]:
    s = rnd[rnd.algorithm == alg].sort_values("n")
    ax.plot(s.n, s.time_ms, marker=MARK[alg], label=alg,
            linestyle="--" if alg == "ArraysSort" else "-")
n = np.array(sorted(rnd.n.unique()), dtype=float)
ax.plot(n, n * np.log2(n) / (n[-1] * np.log2(n[-1])) * 120, "k:", lw=1, label="c·n log n")
ax.plot(n, n / n[-1] * 45, "k-.", lw=1, label="c·n")
ax.set_xscale("log"); ax.set_yscale("log")
ax.set_xlabel("n"); ax.set_ylabel("time, ms (median of 7)")
ax.set_title("Execution time vs n (random input)")
ax.legend()
save(fig, "time_vs_n.png")

# 2. Recursion depth vs n ------------------------------------------------------
fig, ax = plt.subplots(figsize=(8, 5.5))
depth = main[main.algorithm.isin(ALGOS)].groupby(["algorithm", "n"]).max_depth.max().reset_index()
for alg in ALGOS:
    s = depth[depth.algorithm == alg].sort_values("n")
    ax.plot(s.n, s.max_depth, marker=MARK[alg], label=alg + " (max over input types)")
qd = main[(main.algorithm == "QuickSort") & (main.input_type == "DUPLICATES")].sort_values("n")
ax.plot(qd.n, qd.max_depth, marker="s", linestyle=":", label="QuickSort, DUPLICATES")
ax.plot(n, np.log2(n), "k--", lw=1, label="log₂ n")
ax.set_xscale("log")
ax.set_xlabel("n"); ax.set_ylabel("max recursion depth")
ax.set_title("Recursion depth vs n")
ax.legend()
save(fig, "depth_vs_n.png")

# 3. Time vs n per input type --------------------------------------------------
fig, axes = plt.subplots(2, 2, figsize=(11, 8), sharex=True)
for ax, alg in zip(axes.flat, ALGOS):
    for t in TYPES:
        s = main[(main.algorithm == alg) & (main.input_type == t)].sort_values("n")
        if len(s):
            ax.plot(s.n, s.time_ms, marker="o", label=t)
    ax.set_xscale("log"); ax.set_yscale("log")
    ax.set_title(alg); ax.set_xlabel("n"); ax.set_ylabel("time, ms")
    ax.legend(fontsize=8)
fig.suptitle("Effect of input structure on running time")
save(fig, "time_by_input_type.png")

# 4. Closest pair: D&C vs brute force -------------------------------------------
cp = df[df.algorithm.isin(["ClosestPairDC", "ClosestPairBrute"])]
fig, ax = plt.subplots(figsize=(8, 5.5))
for alg, lab in [("ClosestPairDC", "Divide & conquer Θ(n log n)"), ("ClosestPairBrute", "Brute force Θ(n²)")]:
    s = cp[cp.algorithm == alg].sort_values("n")
    ax.plot(s.n, s.time_ms, marker="o", label=lab)
ax.set_xscale("log"); ax.set_yscale("log")
ax.set_xlabel("n"); ax.set_ylabel("time, ms")
ax.set_title("Closest pair: divide-and-conquer vs brute force")
ax.legend()
save(fig, "closest_pair_dc_vs_brute.png")

# 5. Normalised cost: comparisons / (n log2 n) or / n ---------------------------
fig, axes = plt.subplots(1, 2, figsize=(12, 4.8))
ax = axes[0]
for alg in ["MergeSort", "QuickSort"]:
    s = rnd[rnd.algorithm == alg].sort_values("n")
    ax.plot(s.n, s.comparisons / (s.n * np.log2(s.n)), marker=MARK[alg], label=alg)
ax.set_xscale("log"); ax.set_xlabel("n"); ax.set_ylabel("comparisons / (n log₂ n)")
ax.set_title("Sorting: comparisons normalised by n log n (random)")
ax.legend()
ax = axes[1]
for t in TYPES:
    s = main[(main.algorithm == "Select") & (main.input_type == t)].sort_values("n")
    ax.plot(s.n, s.comparisons / s.n, marker="^", label=t)
ax.set_xscale("log"); ax.set_xlabel("n"); ax.set_ylabel("comparisons / n")
ax.set_title("Deterministic Select: comparisons normalised by n")
ax.legend()
save(fig, "comparisons_normalized.png")
