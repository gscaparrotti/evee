package evee.custom;

public interface EveeMotor {

    void forward();

    void backward();

    void rotate(int angle);

    void rotateTo(int limitAngle);

    void setSpeed(int speed);

    int getTachoCount();

    void resetTachoCount();

}