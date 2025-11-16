package evee.basicMovements;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import evee.custom.BackwardsEV3LargeRegulatedMotor;
import evee.custom.CombinedMotor;
import evee.custom.SteeringPilot;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.localization.OdometryPoseProvider;
import lejos.robotics.navigation.Move;
import lejos.robotics.navigation.MoveProvider;
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

    volatile boolean started = false;

    private RegulatedMotor motorLeft;
    private RegulatedMotor motorRight;
    private RegulatedMotor turn;

    private SteeringPilot steeringPilot;
    private OdometryPoseProvider poseProvider;

    @SneakyThrows
    public BasicMovements() {
        this.createMotorsAndSensors();
        FileUtils.write(new File("output.txt"), LocalDateTime.now().toString() + "\n", true);
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
        this.motorLeft = new BackwardsEV3LargeRegulatedMotor(MotorPort.A);
        this.motorRight = new BackwardsEV3LargeRegulatedMotor(MotorPort.D);
        this.turn = new EV3MediumRegulatedMotor(MotorPort.C);
        LOGGER.debug("Configuring motors");
        //RegulatedMotor driveMotor = new CombinedMotor(motorLeft, motorRight);
        this.steeringPilot = new SteeringPilot(42.0, motorLeft, turn, 140.0, 0, 0);
        this.steeringPilot.calibrateSteering();
        this.poseProvider = new MyOdometryPoseProvider(this.steeringPilot);
        //this.steeringPilot.arcForward(300.0);
    }

    public void setSpeedForBothMotors(final int speed) {
        this.steeringPilot.setLinearSpeed(speed);
    }

    public void stop() {
        this.steeringPilot.stop();
    }

    public void travel(final int angle) {
        this.travel(angle, 80.0);
    }

    public void travel(final int angle, final double distance) {
        var actualAngle = angle != 0.0 ? angle * 140.0 : Double.POSITIVE_INFINITY;
        //this.steeringPilot.stop();
        this.steeringPilot.travelArc(actualAngle, distance, false);
    }

    private static class MyOdometryPoseProvider extends OdometryPoseProvider {

        public MyOdometryPoseProvider(SteeringPilot steeringPilot) {
            super(steeringPilot);
        }

        @SneakyThrows
        @Override
        public void moveStopped(Move move, MoveProvider mp) {
            super.moveStopped(move, mp);
            final var pose = this.getPose();
            FileUtils.write(new File("output.txt"), pose.getX() + "," + pose.getY() + "\n", true);
        }
    }
}
