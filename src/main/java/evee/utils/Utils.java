package evee.utils;

import lejos.robotics.Color;

public class Utils {

    public static void logColorID(int colorID) {
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
