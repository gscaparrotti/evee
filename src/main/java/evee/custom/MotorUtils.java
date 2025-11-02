package evee.custom;

import ev3dev.hardware.EV3DevMotorDevice;
import lejos.robotics.RegulatedMotor;

public class MotorUtils {

    public static boolean isOverloaded(RegulatedMotor regulatedMotor) {
        if (regulatedMotor instanceof BackwardsEV3LargeRegulatedMotor) {
            return ((BackwardsEV3LargeRegulatedMotor) regulatedMotor).isOverloaded();
        } else {
            return false;
        }
    }

}
