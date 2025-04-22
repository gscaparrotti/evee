package evee.utils.bluetooth;

import evee.basicMovements.BasicMovements;
import evee.utils.bluetooth.BluetoothReceiver.BluetoothCommands;
import lejos.robotics.subsumption.Behavior;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Bluetooth implements Behavior {

    private final BasicMovements basicMovements;
    private final BlockingQueue<Pair<BluetoothCommands, Integer>> bluetoothCommands = new LinkedBlockingQueue<>();
    private volatile boolean active = false;

    public Bluetooth(final BasicMovements basicMovements) {
        this.basicMovements = basicMovements;
    }

    public void onCommand(final BluetoothCommands command, final Integer angle) {
        bluetoothCommands.add(Pair.of(command, angle));
        if (command.equals(BluetoothCommands.START)) {
            active = true;
        } else if (command.equals(BluetoothCommands.STOP)) {
            basicMovements.rotateToAngle(0);
            basicMovements.forward();
            active = false;
        }
    }

    @Override
    public boolean takeControl() {
        return this.active;
    }

    @Override
    public void action() {
        while (!bluetoothCommands.isEmpty()) {
            final var commandAndAngle = bluetoothCommands.poll();
            final var command = commandAndAngle.getKey();
            final var angle = commandAndAngle.getValue();
            switch (command) {
                case START:
                    basicMovements.rotateToAngle(0);
                    basicMovements.stop();
                    break;
                case STOP:
                    break;
                case UP:
                    basicMovements.forward();
                    break;
                case DOWN:
                    basicMovements.backOff(10);
                    break;
                case LEFT:
                    basicMovements.rotateToAngle(angle);
                    break;
                case RIGHT:
                    basicMovements.rotateToAngle(-angle);
                    break;
            }
        }
    }


    @Override
    public void suppress() {
        active = false;
        bluetoothCommands.clear();
    }
}
