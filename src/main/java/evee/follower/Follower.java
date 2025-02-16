package evee.follower;

import ev3dev.actuators.lego.motors.BaseRegulatedMotor;
import ev3dev.sensors.ev3.EV3ColorSensor;
import ev3dev.sensors.ev3.EV3IRSensor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import evee.basicMovements.BasicMovements;
import lejos.hardware.port.SensorPort;
import lejos.robotics.Color;
import lombok.AllArgsConstructor;
import lombok.Value;

import static evee.follower.Follower.Rotation.RotationKind.LEFT;
import static evee.follower.Follower.Rotation.RotationKind.RIGHT;

public class Follower {

    final BasicMovements basicMovements;
    final EV3ColorSensor colorSensor;

    Rotation currentRotation = new Rotation(LEFT);

    public Follower(BasicMovements basicMovements) {
        this.basicMovements = basicMovements;
        this.colorSensor = new EV3ColorSensor(SensorPort.S3);
    }

    public void updateRotation() {
        final var colorID = colorSensor.getColorID();
        logColorID(colorID);
        if (colorID == Color.BLACK) {
            if (this.currentRotation.rotation != LEFT) {
                basicMovements.rotateToAngle(LEFT.angle);
            }
            this.currentRotation = new Rotation(LEFT);
        } else {
            if (this.currentRotation.rotation != RIGHT) {
                basicMovements.rotateToAngle(RIGHT.angle);
            }
            this.currentRotation = new Rotation(RIGHT);
        }
    }

    @Value
    static class Rotation {
        RotationKind rotation;

        @AllArgsConstructor
        enum RotationKind {
            LEFT(-15), RIGHT(15);
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
