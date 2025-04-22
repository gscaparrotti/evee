package evee.basicMovements;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import ev3dev.sensors.EV3Key;
import evee.custom.BackwardsEV3LargeRegulatedMotor;
import lejos.hardware.Key;
import lejos.hardware.KeyListener;
import lejos.hardware.port.MotorPort;
import lejos.robotics.subsumption.Behavior;
import lejos.utility.Delay;
import lombok.Getter;
import org.apache.commons.lang3.mutable.MutableObject;

import static evee.utils.Notifications.Beep.DOUBLE_BEEP;
import static evee.utils.Notifications.beep;
import static evee.utils.Utils.*;

public class BasicMovements implements Behavior {

    public static final int TURN_MOTOR_SPEED = 400;
    public static final int STRAIGHT_MOTOR_SPEED = 500;

    @Getter
    private BackwardsEV3LargeRegulatedMotor motorLeft;
    @Getter
    private BackwardsEV3LargeRegulatedMotor motorRight;
    private EV3MediumRegulatedMotor turn;

    @Getter
    volatile boolean started = false;

    private int currentAngle = -1;

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

    }

    private void createMotorsAndSensors() {
        LOGGER.debug("Creating Motors");
        motorLeft = new BackwardsEV3LargeRegulatedMotor(MotorPort.A);
        motorRight = new BackwardsEV3LargeRegulatedMotor(MotorPort.D);
        turn = new EV3MediumRegulatedMotor(MotorPort.C);
        LOGGER.debug("Configuring motors");
        setSpeedForBothMotors(STRAIGHT_MOTOR_SPEED);
        turn.setSpeed(STRAIGHT_MOTOR_SPEED);
        calibrate();
    }

    public void forward() {
        LOGGER.debug("Moving forward");
        motorLeft.forward();
        motorRight.forward();
    }

    public void backOff() {
        backOff(2000);
    }

    public void backOff(int time) {
        setSpeedForBothMotors(TURN_MOTOR_SPEED);
        //fltBothMotors();
        motorRight.backward();
        motorLeft.backward();
        Delay.msDelay(time);
        //fltBothMotors();
        setSpeedForBothMotors(STRAIGHT_MOTOR_SPEED);
    }

    public void setSpeedForBothMotors(final int speed) {
        motorLeft.setSpeed(speed);
        motorRight.setSpeed(speed);
    }

    public void fltBothMotors() {
        motorLeft.flt(true);
        motorRight.flt(true);
    }

    public void stop() {
        motorLeft.stop();
        motorRight.stop();
    }

    public void rotateToAngle(final int angle) {
        LOGGER.info("Rotate to {}", angle);
        if (currentAngle < 0 || currentAngle != angle) {
            turn.rotateTo(angle);
            turn.stop();
            currentAngle = angle;
        }
    }

    @SuppressWarnings({"StatementWithEmptyBody"})
    public void calibrate() {
        LOGGER.info("Waiting for calibration");
        beep(DOUBLE_BEEP);
        final MutableObject<Boolean> calibrated = new MutableObject<>(false);
        final var allKeys = new EV3Key(EV3Key.BUTTON_ALL);
        allKeys.addKeyListener(new CalibrationKeyListener(calibrated));
        while (!calibrated.getValue()) {}
    }

    private class CalibrationKeyListener implements KeyListener {

        private final MutableObject<Boolean> calibrated;

        public CalibrationKeyListener(final MutableObject<Boolean> calibrated) {
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
