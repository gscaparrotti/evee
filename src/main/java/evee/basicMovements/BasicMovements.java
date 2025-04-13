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

import static evee.utils.Notifications.Beep.DOUBLE_BEEP;
import static evee.utils.Notifications.beep;
import static evee.utils.Utils.*;

public class BasicMovements implements Behavior {

    private static final int TURN_MOTOR_SPEED = 400;
    private static final int STRAIGHT_MOTOR_SPEED = 500;

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

    @Getter
    private BackwardsEV3LargeRegulatedMotor motorLeft;
    @Getter
    private BackwardsEV3LargeRegulatedMotor motorRight;
    private EV3MediumRegulatedMotor turn;

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

    public void moveAroundObstacle(final boolean invertedDirection) {
        setSpeedForBothMotors(TURN_MOTOR_SPEED);
        LOGGER.debug("Rotation started");
        LOGGER.debug("First part of rotation");
        final var direction = RANDOM.nextInt(100) > 20;
        var angle = direction ? 20 : -20;
        if (invertedDirection) {
            angle = -angle;
        }
        turn.rotate(angle);
        turn.stop();
        Delay.msDelay(1000);
        LOGGER.debug("Second part of rotation");
        turn.rotate(-angle);
        turn.stop();
        LOGGER.debug("Rotation completed");
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
        if (currentAngle < 0 || currentAngle != angle) {
            turn.rotateTo(angle);
            currentAngle = angle;
        }
    }

    @SuppressWarnings({"LoopConditionNotUpdatedInsideLoop", "StatementWithEmptyBody"})
    public void calibrate() {
        LOGGER.info("Waiting for calibration");
        beep(DOUBLE_BEEP);
        final boolean[] calibrated = {false};
        final var allKeys = new EV3Key(EV3Key.BUTTON_ALL);
        allKeys.addKeyListener(new CalibrationKeyListener(calibrated));
        while (!calibrated[0]) {}
    }

    private class CalibrationKeyListener implements KeyListener {

        private final boolean[] calibrated;

        public CalibrationKeyListener(boolean[] calibrated) {
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
                    calibrated[0] = true;
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
