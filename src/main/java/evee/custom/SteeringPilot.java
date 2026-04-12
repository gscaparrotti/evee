package evee.custom;

import lejos.robotics.RegulatedMotor;
import lombok.RequiredArgsConstructor;
import lombok.Value;

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

    public void move(final Direction direction) {
        final var startTimestamp = System.nanoTime();
        if (steerMotor != null && driveMotor != null) {
            steer(direction);
            driveMotor.rotate(360);
        }
        final var endTimestamp = System.nanoTime();
        final var elapsedTime = endTimestamp - startTimestamp;
        this.logMovement(direction, elapsedTime);
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

    @SuppressWarnings({"IfStatementWithIdenticalBranches", "DuplicateExpressions"})
    private void logMovement(final Direction direction, final long elapsedTime) {

        final var length = (wheelDiameter / 2) * Math.PI * 2;

        // Recupera posizione e orientamento precedenti
        OrientedPosition previousOrientedPosition = null;
        for (final var movementListener : movementListeners) {
            final var previousMovement = movementListener.getPreviousMovement();
            if (previousMovement != null) {
                previousOrientedPosition = previousMovement.orientedPosition;
            }
        }

        // Posizione di partenza: origine se è il primo movimento
        final var startX = previousOrientedPosition != null ? previousOrientedPosition.x : 0.0;
        final var startY = previousOrientedPosition != null ? previousOrientedPosition.y : 0.0;
        // L'orientamento è l'angolo perpendicolare salvato nel movimento precedente.
        // Il valore iniziale (90°) indica che il robot è parallelo all'asse Y (guarda verso l'alto).
        final var previousAngle = previousOrientedPosition != null ? previousOrientedPosition.orientation : 90.0;

        final double sweepAngle;
        if (direction != Direction.STRAIGHT) {
            final var circle = turnRadius * 2 * Math.PI;
            // Angolo spazzato lungo l'arco (relativo)
            sweepAngle = 360.0 * length / circle;
        } else {
            sweepAngle = 90.0;
        }

        // Lo spostamento relativo viene calcolato nel sistema di riferimento locale del robot
        // (robot orientato verso l'alto, cioè asse Y), poi ruotato dell'orientamento corrente.
        final double localX;
        final double localY;
        if (direction == Direction.LEFT) {
            localX = -(turnRadius * Math.cos(Math.toRadians(sweepAngle)));
            localY = turnRadius * Math.sin(Math.toRadians(sweepAngle));
        } else { // RIGHT
            localX = turnRadius * Math.cos(Math.toRadians(sweepAngle));
            localY = turnRadius * Math.sin(Math.toRadians(sweepAngle));
        }

        // Rotazione del vettore locale nell'orientamento assoluto del robot.
        // startAngle è l'angolo del robot rispetto all'asse X (est = 0°, nord = 90°).
        final var headingRad = Math.toRadians((previousAngle - 90.0) % 360.0);
        final var deltaX = localX * Math.cos(headingRad) - localY * Math.sin(headingRad);
        final var deltaY = localX * Math.sin(headingRad) + localY * Math.cos(headingRad);

        final var newAngle = (previousAngle + (direction == Direction.LEFT ? sweepAngle : -sweepAngle)) % 360.0;

        final var newX = startX + deltaX;
        final var newY = startY + deltaY;

        for (final var movementListener : movementListeners) {
            final var orientedPosition = new OrientedPosition(newX, newY, newAngle);
            final var movement = new Movement(orientedPosition, direction, elapsedTime, length);
            movementListener.movementEnded(movement);
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

    public enum Direction {
        STRAIGHT, LEFT, RIGHT
    }

    public interface MovementListener {

        Movement getPreviousMovement();

        void movementEnded(final Movement movement);

    }

}
