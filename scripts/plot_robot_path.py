#!/usr/bin/env python3
"""Plot the path logged by BasicMovements.BasicMovementListener.

Each line of the input file is either a position, "timestampMillis,x,y", appended
once per SteeringPilot.Movement, or an obstacle marker, "OBSTACLE,timestampMillis,x,y",
appended by ObstaclesAvoider whenever it actually detects an obstacle (not on every
poll). This script parses the file and plots the (x, y) path with matplotlib, using
the millisecond timestamps to color the path by time and to compute the elapsed time
between points, and overlays a marker for every obstacle found along the way.

Usage:
    python3 plot_robot_path.py first_correct_output.txt
    python3 plot_robot_path.py first_correct_output.txt --save path.png
"""
import argparse
import csv
import os
import sys

import matplotlib.pyplot as plt
from matplotlib.collections import LineCollection


def parse_log(path):
    timestamps, xs, ys = [], [], []
    obstacle_xs, obstacle_ys = [], []
    with open(path, newline="") as f:
        reader = csv.reader(f)
        for line_number, row in enumerate(reader, start=1):
            if not row:
                continue
            if row[0] == "OBSTACLE":
                if len(row) != 4:
                    print(f"Skipping malformed line {line_number}: {row}", file=sys.stderr)
                    continue
                try:
                    x, y = float(row[2]), float(row[3])
                except ValueError:
                    print(f"Skipping malformed line {line_number}: {row}", file=sys.stderr)
                    continue
                obstacle_xs.append(x)
                obstacle_ys.append(y)
                continue
            if len(row) != 3:
                print(f"Skipping malformed line {line_number}: {row}", file=sys.stderr)
                continue
            try:
                timestamp, x, y = int(row[0]), float(row[1]), float(row[2])
            except ValueError:
                print(f"Skipping malformed line {line_number}: {row}", file=sys.stderr)
                continue
            timestamps.append(timestamp)
            xs.append(x)
            ys.append(y)
    if not xs:
        raise ValueError(f"No valid position data found in {path}")
    return timestamps, xs, ys, obstacle_xs, obstacle_ys


def plot_path(timestamps, xs, ys, obstacle_xs, obstacle_ys, title):
    fig, ax = plt.subplots()

    points = list(zip(xs, ys))
    segments = [[points[i], points[i + 1]] for i in range(len(points) - 1)]
    elapsed_seconds = [(t - timestamps[0]) / 1000 for t in timestamps]

    # BasicMovementListener only logs a position once the first Movement completes, so
    # the very first move (from the robot's true starting pose at the origin) is never
    # itself in the file. Draw it in explicitly.
    ax.plot([0, xs[0]], [0, ys[0]], color="gray", linestyle="--", linewidth=1,
            zorder=2, label="First move (unlogged)")

    if segments:
        line_collection = LineCollection(segments, cmap="viridis")
        line_collection.set_array(elapsed_seconds[:-1])
        ax.add_collection(line_collection)
        fig.colorbar(line_collection, ax=ax, label="Time since start (s)")

    ax.scatter(xs, ys, c=elapsed_seconds, cmap="viridis", s=15, zorder=3)
    ax.scatter(xs[0], ys[0], c="green", marker="o", s=80, label="Start", zorder=4)
    ax.scatter(xs[-1], ys[-1], c="red", marker="X", s=80, label="End", zorder=4)

    if obstacle_xs:
        ax.scatter(obstacle_xs, obstacle_ys, c="orangered", marker="^", s=120,
                    label="Obstacle", zorder=5, edgecolors="black", linewidths=0.5)

    ax.set_xlabel("x")
    ax.set_ylabel("y")
    ax.set_title(title)
    ax.set_aspect("equal", adjustable="datalim")
    ax.legend()
    ax.grid(True, linestyle="--", alpha=0.4)
    fig.tight_layout()
    return fig


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("logfile", help="Path to the log file written by BasicMovementListener (e.g. first_correct_output.txt)")
    parser.add_argument("--save", metavar="IMAGE_PATH", help="Save the plot to this file instead of (or in addition to) showing it")
    parser.add_argument("--no-show", action="store_true", help="Don't open an interactive window (useful with --save)")
    args = parser.parse_args()

    timestamps, xs, ys, obstacle_xs, obstacle_ys = parse_log(args.logfile)
    fig = plot_path(timestamps, xs, ys, obstacle_xs, obstacle_ys, title=f"Robot path ({os.path.basename(args.logfile)})")

    if args.save:
        fig.savefig(args.save, dpi=150)
        print(f"Saved plot to {args.save}")
    if not args.no_show:
        plt.show()


if __name__ == "__main__":
    main()