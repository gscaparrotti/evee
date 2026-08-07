package evee.custom;

import ev3dev.hardware.EV3DevMotorDevice;

public class MotorUtils {

    public static boolean isOverloaded(EveeMotor regulatedMotor) {
        if (regulatedMotor instanceof OverloadableMotor) {
            return ((OverloadableMotor) regulatedMotor).isOverloaded();
        } else {
            return false;
        }
    }

}
