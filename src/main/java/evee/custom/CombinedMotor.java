package evee.custom;

import lombok.AllArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
public class CombinedMotor implements EveeMotor {

    final EveeMotor motorLeft;
    final EveeMotor motorRight;

    @Override
    public void forward() {
        motorLeft.forward();
        motorRight.forward();
    }

    @Override
    public void backward() {
        motorLeft.backward();
        motorRight.backward();
    }

    @Override
    public void rotate(int angle) {
        motorLeft.rotate(angle);
        motorRight.rotate(angle);
    }

    @Override
    public void rotateTo(int limitAngle) {
        motorLeft.rotateTo(limitAngle);
        motorRight.rotateTo(limitAngle);
    }

    @Override
    public void setSpeed(int speed) {
        motorLeft.setSpeed(speed);
        motorRight.setSpeed(speed);
    }

    @Override
    public int getTachoCount() {
        final var leftTachoCount = motorLeft.getTachoCount();
        final var rightTachoCount = motorRight.getTachoCount();
        assert Objects.equals(leftTachoCount, rightTachoCount);
        return leftTachoCount;
    }

    @Override
    public void resetTachoCount() {
        motorLeft.resetTachoCount();
        motorRight.resetTachoCount();
    }

}