package evee.follower;

import ev3dev.sensors.ev3.EV3ColorSensor;
import evee.basicMovements.BasicMovements;
import lejos.hardware.port.SensorPort;
import lejos.robotics.Color;
import lejos.robotics.filter.MedianFilter;
import lejos.robotics.subsumption.Behavior;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Random;

import static evee.follower.Follower.Rotation.RotationKind.LEFT;
import static evee.follower.Follower.Rotation.RotationKind.RIGHT;

public class Follower implements Behavior {

    final Random rand = new Random();

    final BasicMovements basicMovements;
    final EV3ColorSensor colorSensor = new EV3ColorSensor(SensorPort.S3);

    final MedianFilter filter = new MedianFilter(colorSensor, 3);
    long lastBlackFoundTimestamp = -1;
    long lastChangeTimestamp = -1;

    Rotation currentRotation = new Rotation(LEFT);

    public Follower(BasicMovements basicMovements) {
        this.basicMovements = basicMovements;
    }

    @Override
    public boolean takeControl() {
        System.out.println("Evaluating Follower");
        return basicMovements.isStarted();
    }

    @Override
    public void action() {
        this.updateRotation();
    }

    @Override
    public void suppress() {

    }

    public void updateRotation() {
        final float[] buffer = new float[1];
        filter.fetchSample(buffer, 0);
        final var colorID = (int) buffer[0];
        logColorID(colorID);
        if (colorID == Color.BLACK) {
            basicMovements.rotateToAngle(LEFT.angle);
            this.currentRotation = new Rotation(LEFT);
            this.lastBlackFoundTimestamp = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - lastBlackFoundTimestamp < 2000) {
                basicMovements.rotateToAngle(RIGHT.angle);
            } else if (System.currentTimeMillis() - lastChangeTimestamp > 2000) {
                basicMovements.rotateToAngle(0);
                if (System.currentTimeMillis() - lastBlackFoundTimestamp < 10000) {
                    basicMovements.backOff();
                }
                final var randomAngle = rand.nextInt(15);
                final var sign = rand.nextBoolean() ? 1 : -1;
                System.out.println("New random angle: " + randomAngle + ", sign: " + sign);
                basicMovements.rotateToAngle(randomAngle * sign);
                basicMovements.forward();
                this.lastChangeTimestamp = System.currentTimeMillis();
            }
            this.currentRotation = new Rotation(RIGHT);
        }
    }

    @Value
    static class Rotation {
        RotationKind rotation;

        @AllArgsConstructor
        enum RotationKind {
            LEFT(-20), RIGHT(20);
            final int angle;
        }

    }

    private static void logColorID(int colorID) {
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
    }

}
