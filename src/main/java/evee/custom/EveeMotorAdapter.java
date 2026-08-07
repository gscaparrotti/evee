package evee.custom;

import lejos.robotics.RegulatedMotor;
import lombok.AllArgsConstructor;

/**
 * Bridges a {@link lejos.robotics.RegulatedMotor} from the underlying ev3dev/lejos
 * libraries (which the project cannot make implement {@link EveeMotor} directly)
 * to the {@link EveeMotor} surface used throughout the rest of the project.
 */
@AllArgsConstructor
public class EveeMotorAdapter implements EveeMotor {

    private final RegulatedMotor delegate;

    @Override
    public void forward() {
        delegate.forward();
    }

    @Override
    public void backward() {
        delegate.backward();
    }

    @Override
    public void rotate(int angle) {
        delegate.rotate(angle);
    }

    @Override
    public void rotateTo(int limitAngle) {
        delegate.rotateTo(limitAngle);
    }

    @Override
    public void setSpeed(int speed) {
        delegate.setSpeed(speed);
    }

    @Override
    public int getTachoCount() {
        return delegate.getTachoCount();
    }

    @Override
    public void resetTachoCount() {
        delegate.resetTachoCount();
    }

}