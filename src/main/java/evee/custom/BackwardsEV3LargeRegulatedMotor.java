package evee.custom;

import ev3dev.actuators.lego.motors.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;

public class BackwardsEV3LargeRegulatedMotor extends EV3LargeRegulatedMotor {
    /**
     * Constructor
     *
     * @param motorPort motor port
     */
    public BackwardsEV3LargeRegulatedMotor(Port motorPort) {
        super(motorPort);
    }

    @Override
    public void forward() {
        super.backward();
    }

    @Override
    public void backward() {
        super.forward();
    }

    public boolean isOverloaded() {
        return (this.getStringAttribute(STATE).contains("overloaded"));
    }

}
