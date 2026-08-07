package evee.custom;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Pilots a car-like (Ackermann/bicycle-model) robot: one {@link #driveMotor} pushes the
 * robot forward by a fixed amount, and one {@link #steerMotor} points the steerable wheel
 * at one of three fixed positions ({@link Direction#STRAIGHT}, {@link Direction#LEFT} or
 * {@link Direction#RIGHT}) rather than at an arbitrary continuous angle.
 *
 * <p>{@link #calibrateSteering()} must be called once before driving: it sweeps
 * {@link #steerMotor} to its physical left/right limits to find their tacho-count
 * positions, resets the encoder so 0 is the center/straight position, and stores the two
 * limits in {@link #minLeft} and {@link #minRight}.
 *
 * <p>Each call to {@link #move(Direction, Bearing)} first steers into position via
 * {@link #steer(Direction)} (rotating {@link #steerMotor} to {@link #minLeft},
 * {@link #minRight} or 0), then drives {@link #driveMotor} through one full rotation,
 * forward or backward depending on {@link Bearing} — i.e. every move covers the same
 * ground distance, one wheel circumference ({@link #wheelDiameter} &times; &pi;), just
 * signed by {@link Bearing}. It then hands off to {@link #logMovement} to update the
 * tracked pose and notify listeners.
 *
 * <p>{@link #logMovement} maintains the robot's pose relative to where it started moving
 * in the fields {@link #x}, {@link #y} and {@link #heading} (all implicitly zero at
 * construction, i.e. the origin is the robot's own starting point and pose, not any
 * absolute/world frame). For {@link Direction#STRAIGHT} it just projects the travelled
 * distance along the current {@link #heading}. For {@link Direction#LEFT}/{@link
 * Direction#RIGHT} it applies the standard instantaneous-center-of-curvature (ICC)
 * construction: since the vehicle can only steer to the fixed, calibrated
 * {@link #turnRadius}, the signed curvature radius {@code R} is just {@code turnRadius}
 * with the sign of {@link Direction#steeringAngle}, the heading change {@code dTheta} is
 * the arc length divided by {@code R}, and the new (x, y, heading) is obtained by
 * rotating the old pose by {@code dTheta} around the ICC. Once updated, the new pose is
 * wrapped in a {@link Movement} and broadcast to every registered
 * {@link MovementListener} (see {@link #addMovementListener}).
 */
@RequiredArgsConstructor
public class SteeringPilot {

    final EveeMotor driveMotor;
    final EveeMotor steerMotor;
    final double wheelDiameter;
    final double turnRadius;
    final List<MovementListener> movementListeners = new ArrayList<>();

    int minRight, minLeft;

    double x, y, heading;

    public void addMovementListener(final MovementListener listener) {
        movementListeners.add(listener);
    }

    public void move(final Direction direction, final Bearing bearing) {
        final var startTimestamp = System.nanoTime();
        if (steerMotor != null && driveMotor != null) {
            steer(direction);
            driveMotor.rotate(bearing == Bearing.FORWARD ? 360 : -360);
        }
        final var endTimestamp = System.nanoTime();
        final var elapsedTime = endTimestamp - startTimestamp;
        this.logMovement(direction, bearing, elapsedTime);
    }

    private void steer(final Direction direction) {
        switch (direction) {
            case LEFT:
                steerMotor.rotateTo(minLeft);
                break;
            case RIGHT:
                steerMotor.rotateTo(minRight);
                break;
            case STRAIGHT:
                steerMotor.rotateTo(0);
                break;
        }
    }

    /**
     * Updates {@link #x}, {@link #y} and {@link #heading} to reflect the move just made in
     * {@code direction}, then wraps the resulting pose in a {@link Movement} and passes it
     * to every {@link MovementListener}.
     *
     * <p>The distance travelled is always one wheel circumference ({@link #wheelDiameter}
     * &times; &pi;), signed by {@link Bearing}, since {@link #move(Direction, Bearing)}
     * always drives {@link #driveMotor} through exactly one rotation, forward or backward.
     * For {@link Direction#STRAIGHT} that (signed) distance is simply projected along the
     * current {@link #heading}. For {@link Direction#LEFT}/{@link Direction#RIGHT}, the
     * vehicle only ever steers to the fixed, calibrated {@link #turnRadius}, so the signed
     * curvature radius {@code R} is {@code turnRadius} with the sign of {@link
     * Direction#steeringAngle}; the heading change {@code dTheta} is the signed distance
     * divided by {@code R}, and the new pose is obtained by rotating the old one by
     * {@code dTheta} around the instantaneous center of curvature (ICC).
     *
     * @param direction   the steering direction used for this move
     * @param bearing     whether {@link #driveMotor} was driven forward or backward for this
     *                    move; the travelled distance is negated for {@link Bearing#BACKWARD}
     *                    so the ICC construction below still yields the correct pose
     * @param elapsedTime how long the move took, in nanoseconds, recorded on the resulting
     *                    {@link Movement} but not used in the pose calculation
     */
    private void logMovement(final Direction direction, final Bearing bearing, final long elapsedTime) {

        final var distance = (wheelDiameter / 2) * Math.PI * 2 * bearing.getSign();

        final double EPS = 1e-9;

        final var steeringAngleRad = Math.toRadians(direction.steeringAngle);
        if (Math.abs(steeringAngleRad) < EPS) {
            // Straight-line motion
            x += distance * Math.cos(heading);
            y += distance * Math.sin(heading);
        } else {
            // Curvature radius: turnRadius is the calibrated steering radius (constant,
            // since there is only one steering motor with fixed positions); the sign
            // follows the steering direction.
            double R = Math.signum(steeringAngleRad) * turnRadius;

            // Heading change: arc-angle relation s = R * dTheta on a circle of radius R,
            // so dTheta = s / R (s = distance).
            double dTheta = distance / R;

            // Instantaneous center of curvature (ICC): it lies at distance R from the
            // current position, along the normal to the current heading, i.e.
            // ICC = (x, y) + R * (-sin(heading), cos(heading)).
            double iccX = x - R * Math.sin(heading);
            double iccY = y + R * Math.cos(heading);

            // New coordinates: (x, y) is the point on the circle centered at ICC with
            // radius R corresponding to angle heading, i.e. (x,y) = ICC + R *
            // (sin(heading), -cos(heading)) (verifiable by substituting the two lines
            // above). Evaluating the same parametrization at heading + dTheta gives the
            // point rotated by dTheta around the ICC, i.e. the new position after the arc.
            x = iccX + R * Math.sin(heading + dTheta);
            y = iccY - R * Math.cos(heading + dTheta);
            heading = normalizeAngle(heading + dTheta);
        }

        for (final var movementListener : movementListeners) {
            final var orientedPosition = new OrientedPosition(x, y, heading);
            final var movement = new Movement(orientedPosition, direction, elapsedTime, distance);
            movementListener.movementEnded(movement);
        }

    }

    /** Normalizes an angle into the range (-π, π]. */
    private static double normalizeAngle(double angle) {
        while (angle >  Math.PI) angle -= 2 * Math.PI;
        while (angle <= -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    public void calibrateSteering() {

        steerMotor.setSpeed(100);
        steerMotor.forward();

        long timestamp;
        timestamp = System.currentTimeMillis();
        while (System.currentTimeMillis() - timestamp < 2000) {
            Thread.yield();
        }

        int r = steerMotor.getTachoCount();

        steerMotor.backward();
        timestamp = System.currentTimeMillis();
        while (System.currentTimeMillis() - timestamp < 2000) {
            Thread.yield();
        }
        int l = steerMotor.getTachoCount();

        int center = (l + r) / 2;

        // Adjust values so they are still meaningful when tachocount is reset to zero below (0 = center):
        r -= center;
        l -= center;

        minRight = (int) ((double) r / 2.5);
        minLeft = (int) ((double) l / 2.5);

        steerMotor.rotateTo(center);
        steerMotor.resetTachoCount();
        steerMotor.setSpeed(250);
    }

    @Value
    public static class Movement {
        OrientedPosition orientedPosition;
        Direction direction;
        double length;
        double elapsedTime;
    }

    @Value
    public static class OrientedPosition {
        double x;
        double y;
        double orientation;
    }

    @AllArgsConstructor
    @Getter
    public enum Direction {
        STRAIGHT(0.0), LEFT(-30.0), RIGHT(30.0);
        private final double steeringAngle;
    }

    /** Whether {@link #driveMotor} is driven forward or backward for a given {@link #move}. */
    @AllArgsConstructor
    @Getter
    public enum Bearing {
        FORWARD(1), BACKWARD(-1);
        private final int sign;
    }

    public interface MovementListener {

        Movement getPreviousMovement();

        void movementEnded(final Movement movement);

        /** Marks the position of {@link #getPreviousMovement()} as an obstacle sighting. */
        void markObstacleFound();

    }

}
