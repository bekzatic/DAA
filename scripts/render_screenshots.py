"""Renders captured console output (results/*.txt) as terminal-style PNG screenshots."""
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.image as mpimg

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "screenshots"
OUT.mkdir(parents=True, exist_ok=True)


def terminal(lines, title, name, width=12.5):
    h = 0.55 + 0.152 * len(lines)
    fig = plt.figure(figsize=(width, h))
    fig.patch.set_facecolor("#1e1e1e")
    fig.text(0.01, 1 - 0.28 / h, "  " + title, color="#9cdcfe", family="monospace",
             fontsize=10, va="top", weight="bold")
    fig.text(0.01, 1 - 0.55 / h, "\n".join(lines), color="#d4d4d4", family="monospace",
             fontsize=8.6, va="top", linespacing=1.28)
    fig.savefig(OUT / name, facecolor=fig.get_facecolor(), dpi=120)
    plt.close(fig)
    print("wrote", OUT / name)


run = (ROOT / "results" / "program_output.txt").read_text().splitlines()
split = next(i for i, l in enumerate(run) if l.startswith("algorithm"))
terminal(["$ java -cp target/classes daa.Main"] + run[:split + 2] + run[split + 2:split + 20] + ["..."],
         "Program output: demo + experiment run", "program_output.png")

tests = (ROOT / "results" / "test_output.txt").read_text().splitlines()
terminal(["$ java -jar junit-platform-console-standalone.jar execute --scan-classpath ..."] + tests,
         "Test results: 16/16 passed", "test_results.png", width=9.5)

# large-n part of the summary table
big = [l for l in run[split + 2:] if any(f" {x} " in l for x in ["1,000,000", "500,000"])]
terminal(run[split:split + 2] + big, "Results: large inputs (n = 500k and 1M)", "results_large_n.png", width=9.5)

# plots overview
names = ["time_vs_n.png", "depth_vs_n.png", "time_by_input_type.png", "closest_pair_dc_vs_brute.png"]
fig, axes = plt.subplots(2, 2, figsize=(14, 10))
for ax, n in zip(axes.flat, names):
    ax.imshow(mpimg.imread(ROOT / "docs" / "plots" / n)); ax.axis("off")
fig.tight_layout()
fig.savefig(OUT / "plots_overview.png", dpi=110)
print("wrote", OUT / "plots_overview.png")
