package evee;

import evee.basicMovements.BasicMovements;
import evee.follower.Follower;
import evee.obstacles.ObstaclesAvoider;
import evee.utils.bluetooth.Bluetooth;
import evee.utils.bluetooth.BluetoothReceiver;
import lejos.robotics.subsumption.Arbitrator;
import lejos.robotics.subsumption.Behavior;
import lombok.Getter;

import static evee.utils.Utils.LOGGER;

@SuppressWarnings("FieldCanBeLocal")
@Getter
public class Robot {

    private final BasicMovements basicMovements;
    private final ObstaclesAvoider obstaclesAvoider;
    private final Follower follower;
    private final Bluetooth bluetooth;

    private final Arbitrator arbitrator;

    public Robot() {
        BluetoothReceiver.receive((command, angle) -> {
            if (this.getBluetooth() != null) {
                this.getBluetooth().onCommand(command, angle);
            }
        });
        this.basicMovements = new BasicMovements();
        this.obstaclesAvoider = new ObstaclesAvoider(this.basicMovements);
        this.follower = new Follower(this.basicMovements);
        this.bluetooth = new Bluetooth(this.basicMovements);
        this.arbitrator = new Arbitrator(new Behavior[]{this.basicMovements, this.follower, this.obstaclesAvoider, this.bluetooth});
    }

    private void runRobot() {
        LOGGER.info("Welcome!");
        this.arbitrator.go();
    }

    public static void main(final String[] args){
        new Robot().runRobot();
    }

}
