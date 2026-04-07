package evee.custom;

import lejos.robotics.RegulatedMotor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class SteeringPilot {

    final RegulatedMotor driveMotor;
    final RegulatedMotor steerMotor;
    final double wheelDiameter;
    final double turnRadius;
    final List<MovementListener> movementListeners = new ArrayList<>();

    int minRight, minLeft;

    public void addMovementListener(final MovementListener listener) {
        movementListeners.add(listener);
    }

    public void move(final Directions direction) {
        final var startTimestamp = System.nanoTime();
        if (steerMotor != null && driveMotor != null) {
            steer(direction);
            driveMotor.rotate(360);
        }
        final var endTimestamp = System.nanoTime();

        // distanza percorsa, pari al diametro della ruota (ha ruotato esattamente di 360 gradi)
        final var length = (wheelDiameter / 2) * Math.PI * 2;

        final double[] point;
        double angle;
        final double perpendicularAngle;
        if (direction != Directions.STRAIGHT) {
            // la lunghezza della circonferenza data dal raggio di sterzata
            final var circle = turnRadius * 2 * Math.PI;
            // l'angolo spazzato su questa circonferenza dal robot, ossia l'angolo sotteso dall'arco di circonferenza di lunghezza 'length'
            angle = 360 * length / circle;
            // la nuova posizione del robot assumendo che parta da (0, 0)
            point = new double[] {turnRadius * Math.cos(Math.toRadians(angle)), turnRadius * Math.sin(Math.toRadians(angle))};
            point[0] -= turnRadius;
            if (direction == Directions.RIGHT) {
                point[0] = -point[0];
            }
            if (direction == Directions.RIGHT) {
                angle = -angle;
            }
            // il nuovo orientamento del robot, assumendo che in precedenza fosse 90 gradi, ossia parallelo all'asse Y
            perpendicularAngle = (angle + Math.toDegrees(Math.PI / 2)) % 360.0;
        } else {
            point = new double[] {0, length};
            angle = 90.0;
            perpendicularAngle = 180.0;
        }

        final var elapsedTime = endTimestamp - startTimestamp;
        for (MovementListener l : movementListeners) {
            final var previousMovement = l.getPreviousMovement();
            double newAngle = Double.NaN;
            if (previousMovement != null) {
                final var previousAngle = previousMovement.position.orientation;
                final var rotation = Math.toRadians(previousAngle + angle);
                newAngle = Math.toDegrees(rotation);
                final var affineTransform = new AffineTransform();
                affineTransform.translate(previousMovement.position.x, previousMovement.position.y);
                affineTransform.rotate(rotation);
                affineTransform.transform(point, 0, point, 0, 1);
            }
            final var movement = new Movement(direction, elapsedTime, length, new Position(point[0], point[1], !Double.isNaN(newAngle) ? newAngle : perpendicularAngle));
            l.movementEnded(movement);
        }
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

    private void steer(Directions direction) {
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

    @Data
    public static class Movement {

        final Directions direction;
        final double length;
        final double elapsedTime;
        final Position position;

    }

    @Data
    public static class Position {
        final double x;
        final double y;
        final double orientation;
    }

    public interface MovementListener {

        Movement getPreviousMovement();

        void movementEnded(final Movement movement);

    }

    public enum Directions {
        STRAIGHT, LEFT, RIGHT
    }

}
