package evee.basicMovements;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import evee.custom.BackwardsEV3LargeRegulatedMotor;
import evee.custom.SteeringPilot;
import evee.custom.SteeringPilot.Bearing;
import evee.custom.SteeringPilot.Direction;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.subsumption.Behavior;
import lombok.Getter;

import static evee.utils.Utils.*;

@SuppressWarnings("UnnecessaryReturnStatement")
@Getter
public class BasicMovements implements Behavior {

    public static final double TURN_RADIUS = 168.0;
    public static final double WHEEL_DIAMETER = 43.2;

    volatile boolean started = false;

    private RegulatedMotor driveMotor;
    private RegulatedMotor steerMotor;

    private SteeringPilot steeringPilot;
    private BasicMovementListener movementListener;

    public BasicMovements() {
        this.createMotorsAndSensors();
    }

    @Override
    public boolean takeControl() {
        return true;
    }

    @Override
    public void action() {
        started = true;
    }

    @Override
    public void suppress() {
        return;
    }

    private void createMotorsAndSensors() {
        LOGGER.debug("Creating Motors");
        this.driveMotor = new BackwardsEV3LargeRegulatedMotor(MotorPort.D);
        this.steerMotor = new EV3MediumRegulatedMotor(MotorPort.C);
        this.steeringPilot = new SteeringPilot(driveMotor, steerMotor, WHEEL_DIAMETER, TURN_RADIUS);
        this.steeringPilot.calibrateSteering();
        this.movementListener = new BasicMovementListener();
        this.steeringPilot.addMovementListener(this.movementListener);
    }

    public void travel(final int angle) {
        this.travel(angle, 100.0);
    }

    public void travel(final int angle, final double distance) {
        final Direction direction;
        if (angle < 0) {
            direction = Direction.LEFT;
        } else if (angle > 0) {
            direction = Direction.RIGHT;
        } else {
            direction = Direction.STRAIGHT;
        }
        final int iterations = (int) (distance / 100);
        final var bearing = distance < 0 ? Bearing.BACKWARD : Bearing.FORWARD;
        for (int i = 0; i < Math.max(iterations, 1); i++) {
            this.steeringPilot.move(direction, bearing);
        }
    }
}
