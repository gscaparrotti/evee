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

public class BasicMovements implements Behavior {

    private static final int TURN_MOTOR_SPEED = 400;
    private static final int STRAIGHT_MOTOR_SPEED = 500;

    @Getter
    volatile boolean started = false;

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
        System.out.println("Creating Motors");
        motorLeft = new BackwardsEV3LargeRegulatedMotor(MotorPort.A);
        motorRight = new BackwardsEV3LargeRegulatedMotor(MotorPort.D);
        turn = new EV3MediumRegulatedMotor(MotorPort.C);
        System.out.println("Configuring motors");
        setSpeedForBothMotors(STRAIGHT_MOTOR_SPEED);
        calibrate();
    }

    private BackwardsEV3LargeRegulatedMotor motorLeft;
    private BackwardsEV3LargeRegulatedMotor motorRight;
    private EV3MediumRegulatedMotor turn;

    public void forward() {
        System.out.println("Moving forward");
        motorLeft.forward();
        motorRight.forward();
    }

    public void backOff() {
        setSpeedForBothMotors(TURN_MOTOR_SPEED);
        fltBothMotors();
        motorRight.backward();
        motorLeft.backward();
        Delay.msDelay(2000);
        fltBothMotors();
        setSpeedForBothMotors(STRAIGHT_MOTOR_SPEED);
    }

    public void moveAroundObstacle() {
        setSpeedForBothMotors(TURN_MOTOR_SPEED);
        System.out.println("Rotation started");
        System.out.println("First part of rotation");
        final var angle = 15;
        turn.rotate(angle);
        turn.stop();
        Delay.msDelay(1000);
        System.out.println("Second part of rotation");
        turn.rotate(-angle);
        turn.stop();
        System.out.println("Rotation completed");
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

    public void rotateToAngle(final int angle) {
        turn.rotateTo(angle);
    }

    @SuppressWarnings({"LoopConditionNotUpdatedInsideLoop", "StatementWithEmptyBody"})
    public void calibrate() {
        System.out.println("Waiting for calibration");
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
            System.out.println(k + " pressed");
            System.out.println("Button ID: " + k.getId());
            switch (k.getId()) {
                case EV3Key.BUTTON_ENTER:
                    calibrated[0] = true;
                    System.out.println("Calibration completed");
                    turn.resetTachoCount();
                    break;
                case EV3Key.BUTTON_LEFT:
                    turn.rotate(1);
                    turn.stop();
                    System.out.println("Turn left");
                    break;
                case EV3Key.BUTTON_RIGHT:
                    turn.rotate(-1);
                    turn.stop();
                    System.out.println("Turn right");
                    break;
            }
        }
    }

}
