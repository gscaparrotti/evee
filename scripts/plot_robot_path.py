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


def catmull_rom_path(xs, ys, values, samples_per_segment=15):
    """Interpolate a smooth Catmull-Rom spline through (xs, ys), so consecutive logged
    positions are joined by a curve instead of a straight segment - closer to how the
    robot actually moves when steering left or right instead of teleporting in a
    straight hop between two points. `values` (e.g. elapsed time) is linearly
    interpolated alongside the curve so callers can still color it per-point.

    Falls back to the original points when there aren't enough of them to curve.
    """
    n = len(xs)
    if n < 3:
        return list(xs), list(ys), list(values)

    pts = list(zip(xs, ys))
    extended_pts = [pts[0]] + pts + [pts[-1]]
    extended_values = [values[0]] + list(values) + [values[-1]]

    curve_xs, curve_ys, curve_values = [], [], []
    segment_count = len(extended_pts) - 3
    for i in range(1, segment_count + 1):
        p0, p1, p2, p3 = extended_pts[i - 1], extended_pts[i], extended_pts[i + 1], extended_pts[i + 2]
        v0, v1 = extended_values[i], extended_values[i + 1]
        is_last_segment = i == segment_count
        steps = samples_per_segment + (1 if is_last_segment else 0)
        for step in range(steps):
            t = step / samples_per_segment
            t2, t3 = t * t, t * t * t
            x = 0.5 * ((2 * p1[0]) + (-p0[0] + p2[0]) * t +
                       (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 +
                       (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3)
            y = 0.5 * ((2 * p1[1]) + (-p0[1] + p2[1]) * t +
                       (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 +
                       (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3)
            curve_xs.append(x)
            curve_ys.append(y)
            curve_values.append(v0 + (v1 - v0) * t)
    return curve_xs, curve_ys, curve_values


def plot_path(timestamps, xs, ys, obstacle_xs, obstacle_ys, title):
    fig, ax = plt.subplots()

    elapsed_seconds = [(t - timestamps[0]) / 1000 for t in timestamps]
    curve_xs, curve_ys, curve_elapsed = catmull_rom_path(xs, ys, elapsed_seconds)
    curve_points = list(zip(curve_xs, curve_ys))
    segments = [[curve_points[i], curve_points[i + 1]] for i in range(len(curve_points) - 1)]

    # BasicMovementListener only logs a position once the first Movement completes, so
    # the very first move (from the robot's true starting pose at the origin) is never
    # itself in the file. Draw it in explicitly.
    ax.plot([0, xs[0]], [0, ys[0]], color="gray", linestyle="--", linewidth=1,
            zorder=2, label="First move (unlogged)")

    if segments:
        line_collection = LineCollection(segments, cmap="viridis")
        line_collection.set_array(curve_elapsed[:-1])
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