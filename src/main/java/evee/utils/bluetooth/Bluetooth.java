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
        BluetoothReceiver.receive(this::onCommand);
    }

    public void onCommand(final BluetoothCommands command, final Integer angle) {
        bluetoothCommands.add(Pair.of(command, angle));
        if (command.equals(BluetoothCommands.START)) {
            active = true;
        } else if (command.equals(BluetoothCommands.STOP)) {
            basicMovements.travel(0, 1);
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
                    basicMovements.travel(0);
                    break;
                case STOP:
                    break;
                case UP:
                    basicMovements.travel(0);
                    break;
                case DOWN:
                    basicMovements.travel(0, -100);
                    break;
                case LEFT:
                    basicMovements.travel(angle);
                    break;
                case RIGHT:
                    basicMovements.travel(-angle);
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
