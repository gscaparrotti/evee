package evee.test;

import evee.basicMovements.BasicMovements;
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
                System.out.println(movement.getPosition());
            }
        });
        steeringPilot.move(SteeringPilot.Directions.LEFT);
        steeringPilot.move(SteeringPilot.Directions.LEFT);
    }

}
