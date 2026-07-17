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

    public static final int TURN_MOTOR_SPEED = 400;
    public static final int STRAIGHT_MOTOR_SPEED = 500;
    public static final double TURN_RADIUS = 170.0;
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

    public void setSpeedForBothMotors(final int speed) {
        //this.steeringPilot.setLinearSpeed(speed);
    }

    public void stop() {
        //this.steeringPilot.stop();
    }

    public void travel(final int angle) {
        this.travel(angle, 80.0);
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
        final var bearing = distance < 0 ? Bearing.BACKWARD : Bearing.FORWARD;
        this.steeringPilot.move(direction, bearing);
    }
}
