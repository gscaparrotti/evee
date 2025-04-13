package evee.utils;

import lejos.robotics.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class Utils {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    public static final Random RANDOM;

    static {
        RANDOM = new Random();
        LOGGER.debug("Rand: {}", RANDOM.nextInt());
    }

    private Utils() { }

    public static void logColorID(int colorID) {
        if (LOGGER.isDebugEnabled()) {
            switch (colorID) {
                case Color.NONE:
                    LOGGER.debug("No color detected");
                    break;
                case Color.BLACK:
                    LOGGER.debug("BLACK detected");
                    break;
                case Color.BLUE:
                    LOGGER.debug("BLUE detected");
                    break;
                case Color.GREEN:
                    LOGGER.debug("GREEN detected");
                    break;
                case Color.YELLOW:
                    LOGGER.debug("YELLOW detected");
                    break;
                case Color.RED:
                    LOGGER.debug("RED detected");
                    break;
                case Color.WHITE:
                    LOGGER.debug("WHITE detected");
                    break;
                case Color.BROWN:
                    LOGGER.debug("BROWN detected");
                    break;
            }
        }
    }

}
