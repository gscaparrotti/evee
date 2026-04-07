package evee.basicMovements;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import evee.custom.BackwardsEV3LargeRegulatedMotor;
import evee.custom.SteeringPilot;
import evee.custom.SteeringPilot.Directions;
import evee.custom.SteeringPilot.Movement;
import evee.custom.SteeringPilot.MovementListener;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.subsumption.Behavior;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.time.LocalDateTime;

import static evee.utils.Utils.*;

@SuppressWarnings("UnnecessaryReturnStatement")
@Getter
public class BasicMovements implements Behavior {

    public static final int TURN_MOTOR_SPEED = 400;
    public static final int STRAIGHT_MOTOR_SPEED = 500;
    public static final double TURN_RADIUS = 155.0;

    volatile boolean started = false;

    private RegulatedMotor motorRight;
    private RegulatedMotor turn;

    private SteeringPilot steeringPilot;
    private MovementListener movementListener;

    @SneakyThrows
    public BasicMovements() {
        this.createMotorsAndSensors();
        FileUtils.write(new File("output.txt"), LocalDateTime.now() + "\n", true);
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
        this.motorRight = new BackwardsEV3LargeRegulatedMotor(MotorPort.D);
        this.turn = new EV3MediumRegulatedMotor(MotorPort.C);
        LOGGER.debug("Configuring motors");
        this.steeringPilot = new SteeringPilot(motorRight, turn, 42.0, 155.0);;
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
        final Directions direction;
        if (angle < 0) {
            direction = Directions.LEFT;
        } else if (angle > 0) {
            direction = Directions.RIGHT;
        } else {
            direction = Directions.STRAIGHT;
        }
        this.steeringPilot.move(direction);
    }

    private static class BasicMovementListener implements MovementListener {

        private Movement previousMovement;

        @Override
        public Movement getPreviousMovement() {
            return this.previousMovement;
        }

        @SneakyThrows
        @Override
        public void movementEnded(Movement movement) {
            this.previousMovement = movement;
            final var pose = movement.getPosition();
            FileUtils.write(new File("output.txt"), System.currentTimeMillis() + "," + pose.getX() + "," + pose.getY() + "\n", true);
        }
    }
}
