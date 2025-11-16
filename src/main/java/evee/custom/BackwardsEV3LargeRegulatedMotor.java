package evee.custom;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;

import java.util.Objects;

public class BackwardsEV3LargeRegulatedMotor extends EV3LargeRegulatedMotor {

    private final String motorPort;

    /**
     * Constructor
     *
     * @param motorPort motor port
     */
    public BackwardsEV3LargeRegulatedMotor(Port motorPort) {
        super(motorPort);
        this.motorPort = motorPort.getName();
    }

    @Override
    public void forward() {
        super.backward();
    }

    @Override
    public void backward() {
        super.forward();
    }

    @Override
    public void rotateTo(int limitAngle, boolean immediateReturn) {
        super.rotateTo(-limitAngle, immediateReturn);
    }

    @Override
    public void rotate(int angle, boolean immediateReturn) {
        Objects.requireNonNull(this.motorPort);
        super.rotate(-angle, immediateReturn);
    }

    public boolean isOverloaded() {
        return (this.getStringAttribute(STATE).contains("overloaded"));
    }

}
