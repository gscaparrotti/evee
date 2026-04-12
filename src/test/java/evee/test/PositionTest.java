package evee.test;

import evee.custom.SteeringPilot;
import org.junit.jupiter.api.Test;

public class PositionTest {

    @Test
    public void testPosition() {
        final var steeringPilot = new SteeringPilot(null, null, 42.0, 155.0);
        steeringPilot.addMovementListener(new SteeringPilot.MovementListener() {

            SteeringPilot.Movement movement;

            @Override
            public SteeringPilot.Movement getPreviousMovement() {
                return movement;
            }

            @Override
            public void movementEnded(SteeringPilot.Movement movement) {
                this.movement = movement;
                final var pose = movement.getOrientedPosition();
                System.out.println(System.currentTimeMillis() + "," + pose.getX() + "," + pose.getY());
            }
        });
        steeringPilot.move(SteeringPilot.Direction.RIGHT);
        steeringPilot.move(SteeringPilot.Direction.RIGHT);
        steeringPilot.move(SteeringPilot.Direction.LEFT);
        steeringPilot.move(SteeringPilot.Direction.LEFT);

    }
}
