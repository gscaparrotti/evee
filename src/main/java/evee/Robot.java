package evee;

import ev3dev.actuators.lego.motors.EV3MediumRegulatedMotor;
import ev3dev.sensors.EV3Key;
import ev3dev.sensors.ev3.EV3ColorSensor;
import ev3dev.sensors.ev3.EV3IRSensor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import evee.custom.BackwardsEV3LargeRegulatedMotor;
import lejos.hardware.Key;
import lejos.hardware.KeyListener;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.robotics.Color;
import lejos.utility.Delay;

@SuppressWarnings("FieldCanBeLocal")
public class Robot {

    private static final int TURN_MOTOR_SPEED = 400;
    private static final int STRAIGHT_MOTOR_SPEED = 500;

    private BackwardsEV3LargeRegulatedMotor motorLeft;
    private BackwardsEV3LargeRegulatedMotor motorRight;
    private EV3MediumRegulatedMotor turn;
    private EV3IRSensor irSensor;
    private EV3TouchSensor touchSensor;
    private EV3ColorSensor colorSensor;

    private void runRobot() {
        System.out.println("Creating Motors");
        motorLeft = new BackwardsEV3LargeRegulatedMotor(MotorPort.A);
        motorRight = new BackwardsEV3LargeRegulatedMotor(MotorPort.D);
        turn = new EV3MediumRegulatedMotor(MotorPort.C);

        System.out.println("Configuring motors");
        setSpeedForBothMotors(STRAIGHT_MOTOR_SPEED);

        System.out.println("Creating Sensors");
        irSensor = new EV3IRSensor(SensorPort.S1);
        touchSensor = new EV3TouchSensor(SensorPort.S2);
        colorSensor = new EV3ColorSensor(SensorPort.S3);

        System.out.println("Waiting for calibration");
        final boolean[] calibrated = {false};
        final var allKeys = new EV3Key(EV3Key.BUTTON_ALL);
        allKeys.addKeyListener(new KeyListener() {
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
        });
        while (!calibrated[0]) {}

        boolean doContinue = true;
        int[] currentRotation = new int[] {0, 0, 0};
        while (doContinue) {
            forward();
            currentRotation = follow(currentRotation);
            final var distanceMode = irSensor.getDistanceMode();
            float[] distanceSample = new float[distanceMode.sampleSize()];
            distanceMode.fetchSample(distanceSample, 0);
            final var distance = (int) distanceSample[0];
            if (distance < 50) {
                System.out.println("Obstacle found at a distance of " + distance + " cm");
                moveAroundObstacle();
            }
            final var touchMode = touchSensor.getTouchMode();
            float[] touchSample = new float[distanceMode.sampleSize()];
            touchMode.fetchSample(touchSample, 0);
            final var touch = (int) touchSample[0] == 1;
            if (touch) {
                System.out.println("Obstacle found by touch");
                backOff();
                forward();
                moveAroundObstacle();
            }
        }

        System.exit(0);
    }

    private int[] follow(final int[] currentRotationAndCount) {
        final int currentRotation = currentRotationAndCount[0];
        final int currentCount = currentRotationAndCount[1];
        final boolean change = currentRotationAndCount[2] != 0;
        final var colorID = colorSensor.getColorID();
        switch (colorID) {
            case Color.NONE:
                System.out.println("No color detected");
                break;
            case Color.BLACK:
                System.out.println("BLACK detected");
                break;
            case Color.BLUE:
                System.out.println("BLUE detected");
                break;
            case Color.GREEN:
                System.out.println("GREEN detected");
                break;
            case Color.YELLOW:
                System.out.println("YELLOW detected");
                break;
            case Color.RED:
                System.out.println("RED detected");
                break;
            case Color.WHITE:
                System.out.println("WHITE detected");
                break;
            case Color.BROWN:
                System.out.println("BROWN detected");
                break;
        }
        if (colorID == Color.BLACK) {
            if (currentRotation != 1) {
                turn.rotate((Math.abs(currentRotation) + 1) * 15);
            }
            return new int[] {1, 0, 0};
        } else {
            if (currentRotation != -1) {
                final int direction = ((currentCount / 50) % 2) == 0 ? 1 : -1;
                final int times = (change ? 2 : 1);
                turn.rotate((Math.abs(currentRotation) + 1) * -15);
            }
            final boolean newChange = currentCount % 50 == 0 && currentCount != 0;
            return new int[] {newChange ? 0 : -1, currentCount + 1, newChange ? 1 : 0};
        }
    }

    private void forward() {
        motorLeft.forward();
        motorRight.forward();
    }

    private void moveAroundObstacle() {
        moveAroundObstacle(true);
    }

    private void moveAroundObstacle(boolean left) {
        setSpeedForBothMotors(TURN_MOTOR_SPEED);
        System.out.println("Rotation started");
        System.out.println("First part of rotation");
        final var angle = left ? 15 : -15;
        //turn.resetTachoCount();
        turn.rotate(angle);
        turn.stop();
        Delay.msDelay(1000);
        System.out.println("Second part of rotation");
        turn.rotate(-angle);
        turn.stop();
        System.out.println("Rotation completed");
        setSpeedForBothMotors(STRAIGHT_MOTOR_SPEED);
    }

    private void backOff() {
        setSpeedForBothMotors(TURN_MOTOR_SPEED);
        fltBothMotors();
        motorRight.backward();
        motorLeft.backward();
        Delay.msDelay(2000);
        fltBothMotors();
        setSpeedForBothMotors(STRAIGHT_MOTOR_SPEED);
    }

    private void setSpeedForBothMotors(final int speed) {
        motorLeft.setSpeed(speed);
        motorRight.setSpeed(speed);
    }

    private void fltBothMotors() {
        motorLeft.flt(true);
        motorRight.flt(true);
    }

    public static void main(final String[] args){
        new Robot().runRobot();
    }

}
