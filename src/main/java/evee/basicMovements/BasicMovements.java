package evee.basicMovements;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import ev3dev.sensors.EV3Key;
import evee.custom.BackwardsEV3LargeRegulatedMotor;
import evee.custom.CombinedMotor;
import evee.custom.SteeringPilot;
import lejos.hardware.Key;
import lejos.hardware.KeyListener;
import lejos.hardware.port.MotorPort;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.subsumption.Behavior;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.mutable.MutableObject;

import static evee.utils.Notifications.Beep.DOUBLE_BEEP;
import static evee.utils.Notifications.beep;
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
        this.forward();
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
        RegulatedMotor driveMotor = new CombinedMotor(motorLeft, motorRight);
        this.steeringPilot = new SteeringPilot(42.0, driveMotor, turn, 140.0, 0, 0);
        this.steeringPilot.calibrateSteering();
        this.steeringPilot.arcForward(300.0);
    }

    public void forward() {
        this.steeringPilot.forward();
    }

    public void backOff() {
        this.steeringPilot.backward();
    }

    @SneakyThrows
    public void backOff(int time) {
        this.steeringPilot.backward();
        Thread.sleep(time);
        this.steeringPilot.stop();
    }

    public void setSpeedForBothMotors(final int speed) {
        this.steeringPilot.setLinearSpeed(speed);
    }

    public void stop() {
        this.steeringPilot.stop();
    }

    public void rotateToAngle(final int angle) {
        this.steeringPilot.travelArc(Math.max(angle * 140.0, 140.0), Double.POSITIVE_INFINITY, true);
    }

    @SuppressWarnings({"StatementWithEmptyBody"})
    public void calibrate() {
        LOGGER.info("Waiting for calibration");
        beep(DOUBLE_BEEP);
        final MutableObject<Boolean> calibrated = new MutableObject<>(false);
        final var allKeys = new EV3Key(EV3Key.BUTTON_ALL);
        allKeys.addKeyListener(new CalibrationKeyListener(turn, calibrated));
        while (!calibrated.getValue()) {}
    }

    private static class CalibrationKeyListener implements KeyListener {

        private final RegulatedMotor turn;
        private final MutableObject<Boolean> calibrated;

        public CalibrationKeyListener(final RegulatedMotor turnMotor, final MutableObject<Boolean> calibrated) {
            this.turn = turnMotor;
            this.calibrated = calibrated;
        }

        @Override
        public void keyReleased(Key k) { }

        @Override
        public void keyPressed(Key k) {
            LOGGER.debug("{} pressed", k);
            LOGGER.debug("Button ID: {}", k.getId());
            switch (k.getId()) {
                case EV3Key.BUTTON_ENTER:
                    calibrated.setValue(true);
                    LOGGER.debug("Calibration completed");
                    turn.resetTachoCount();
                    break;
                case EV3Key.BUTTON_LEFT:
                    turn.rotate(1);
                    turn.stop();
                    LOGGER.debug("Turn left");
                    break;
                case EV3Key.BUTTON_RIGHT:
                    turn.rotate(-1);
                    turn.stop();
                    LOGGER.debug("Turn right");
                    break;
            }
        }
    }

}
