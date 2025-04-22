package evee.utils.bluetooth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.math.NumberUtils;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import java.io.*;
import java.util.Arrays;
import java.util.function.BiConsumer;

import static evee.utils.Utils.LOGGER;

public class BluetoothReceiver {

    public static void receive(final BiConsumer<BluetoothCommands, Integer> commandsConsumer) {
        final var bluetooththread = new Thread(() -> {
            try {
                UUID uuid = new UUID("446118f08b1e11e29e960800200c9a66", false);
                //Create the servicve url
                String connectionString = "btspp://localhost:" + uuid +";name=Sample SPP Server";
                StreamConnectionNotifier streamConnNotifier = (StreamConnectionNotifier) Connector.open(connectionString);
                //Wait for client connection
                LOGGER.info("\nServer Started. Waiting for clients to connect...");
                StreamConnection connection=streamConnNotifier.acceptAndOpen();
                RemoteDevice dev = RemoteDevice.getRemoteDevice(connection);
                LOGGER.info("Remote device address: {}", dev.getBluetoothAddress());
                LOGGER.info("Remote device name: {}", dev.getFriendlyName(true));
                //read string from spp client
                InputStream inStream=connection.openInputStream();
                BufferedReader bReader=new BufferedReader(new InputStreamReader(inStream));
                OutputStream outputStream = connection.openOutputStream();
                PrintWriter pWriter=new PrintWriter(new OutputStreamWriter(outputStream));
                boolean close = false;
                while (!close) {
                    String lineRead=bReader.readLine();
                    LOGGER.info(lineRead);
                    //send response to spp client
                    pWriter.write("Received message: " + lineRead + "\r\n");
                    final var commandOpt = Arrays.stream(BluetoothCommands.values())
                        .filter(it -> it != null && lineRead != null && lineRead.contains(it.name()))
                        .findFirst();
                    if (commandOpt.isPresent()) {
                        final var command = commandOpt.get();
                        final Integer angle;
                        if (command.isAngle()) {
                            angle = Arrays.stream(lineRead.split("-"))
                                .skip(1)
                                .map(NumberUtils::createInteger)
                                .findFirst()
                                .orElseThrow();
                        } else {
                            angle = null;
                        }
                        commandsConsumer.accept(command, angle);
                    }
                    pWriter.flush();
                    if (lineRead == null || lineRead.contains("exit")) {
                        close = true;
                    }
                }
                bReader.close();
                pWriter.close();
                streamConnNotifier.close();
            }
            catch (Exception e) {
                LOGGER.error("Bluetooth error", e);
            }
        });
        bluetooththread.setDaemon(true);
        bluetooththread.start();
    }

    @Getter
    @AllArgsConstructor
    public enum BluetoothCommands {
        START(false), STOP(false), UP(false), DOWN(false), LEFT(true), RIGHT(true);
        final boolean angle;
    }

}
