package evee.basicMovements;

import evee.custom.SteeringPilot.Movement;
import evee.custom.SteeringPilot.MovementListener;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Appends every {@link Movement} to {@link #OUTPUT_FILE} for later inspection with
 * {@code scripts/plot_robot_path.py}, and lets {@link evee.obstacles.ObstaclesAvoider} mark
 * the robot's last known position as an obstacle sighting via {@link #markObstacleFound()}.
 */
public class BasicMovementListener implements MovementListener {

    private static final File OUTPUT_FILE = new File("output.txt");

    private Movement previousMovement;

    @SneakyThrows
    public BasicMovementListener() {
        FileUtils.write(OUTPUT_FILE, LocalDateTime.now() + "\n", true);
    }

    @Override
    public Movement getPreviousMovement() {
        return this.previousMovement;
    }

    @SneakyThrows
    @Override
    public void movementEnded(Movement movement) {
        this.previousMovement = movement;
        final var pose = movement.getOrientedPosition();
        FileUtils.write(OUTPUT_FILE, System.currentTimeMillis() + "," + pose.getX() + "," + pose.getY() + "\n", true);
    }

    /**
     * Appends a marker to the movement log at the position of the last recorded
     * {@link Movement}, so {@code scripts/plot_robot_path.py} can show where obstacles were
     * found along the path. A no-op before the robot has made its first move.
     */
    @SneakyThrows
    @Override
    public void markObstacleFound() {
        if (this.previousMovement == null) {
            return;
        }
        final var pose = this.previousMovement.getOrientedPosition();
        FileUtils.write(OUTPUT_FILE, "OBSTACLE," + System.currentTimeMillis() + "," + pose.getX() + "," + pose.getY() + "\n", true);
    }
}