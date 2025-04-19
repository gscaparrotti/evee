package evee.utils;

import ev3dev.actuators.Sound;
import lombok.SneakyThrows;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Notifications {

    public static final Sound SOUND = Sound.getInstance();
    private static final BlockingQueue<Beep> BEEPS = new LinkedBlockingQueue<>();

    public static void beep(final Beep beep) {
        BEEPS.offer(beep);
    }

    static  {
        @SuppressWarnings("Convert2Lambda") // because of @SneakyThrows
        final var notificationThread = new Thread(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
            while (true) {
                    final var nextBeep = BEEPS.take();
                    switch (nextBeep) {
                        case SINGLE_LOW_BEEP:
                            SOUND.beep();
                            break;
                        case SINGLE_MEDIUM_BEEP:
                        case SINGLE_HIGH_BEEP:
                            SOUND.playTone(nextBeep.frequency, 100);
                            break;
                        case DOUBLE_BEEP:
                            SOUND.twoBeeps();
                            break;
                    }
                }
            }
        });
        notificationThread.setDaemon(true);
        notificationThread.start();
    }

    public enum Beep {
        SINGLE_LOW_BEEP(null),
        SINGLE_MEDIUM_BEEP(500),
        SINGLE_HIGH_BEEP(1000),
        SINGLE_VERY_HIGH_BEEP(1300),
        DOUBLE_BEEP(null);

        final Integer frequency;

        Beep(Integer frequency) {
            this.frequency = frequency;
        }
    }
}
