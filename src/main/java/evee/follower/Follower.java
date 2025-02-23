package evee.follower;

import ev3dev.actuators.ev3.EV3Led;
import ev3dev.sensors.ev3.EV3ColorSensor;
import evee.basicMovements.BasicMovements;
import evee.utils.Utils;
import lejos.hardware.port.SensorPort;
import lejos.robotics.Color;
import lejos.robotics.filter.MedianFilter;
import lejos.robotics.subsumption.Behavior;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jeasy.states.api.*;
import org.jeasy.states.api.Transition.PeriodicEvent;
import org.jeasy.states.core.FiniteStateMachineBuilder;
import org.jeasy.states.core.TransitionBuilder;

import java.util.Random;
import java.util.Set;

import static evee.follower.Follower.RotationKind.LEFT;
import static evee.follower.Follower.RotationKind.RIGHT;

public class Follower implements Behavior {

    final BasicMovements basicMovements;
    final EV3ColorSensor colorSensor = new EV3ColorSensor(SensorPort.S3);

    final MedianFilter filter = new MedianFilter(colorSensor, 3);

    final FollowerStateMachine followerStateMachine;

    public Follower(BasicMovements basicMovements) {
        this.colorSensor.setFloodlight(Color.WHITE);
        this.basicMovements = basicMovements;
        this.followerStateMachine = new FollowerStateMachine(this.basicMovements);
    }

    @Override
    public boolean takeControl() {
        System.out.println("Evaluating Follower");
        return basicMovements.isStarted();
    }

    @Override
    @SneakyThrows
    public void action() {
        this.followerStateMachine.update(getColorID());
    }

    @Override
    public void suppress() {

    }

    private int getColorID() {
        final float[] buffer = new float[1];
        filter.fetchSample(buffer, 0);
        final var colorID = (int) buffer[0];
        Utils.logColorID(colorID);
        return colorID;
    }

    private static class FollowerStateMachine {

        private static final Random RANDOM = new Random();

        static {
            RANDOM.nextInt();
        }

        private static class BlackDetectedEvent extends AbstractEvent { }
        private static class NotBlackDetectedEvent extends AbstractEvent { }

        private static final State BLACK_NOT_FOUND = new State("BLACK_NOT_FOUND");
        private static final State BLACK_FOUND = new State("BLACK_FOUND");
        private static final State BLACK_LOST = new State("BLACK_LOST");

        private static final Set<State> STATES = Set.of(
            BLACK_NOT_FOUND,
            BLACK_FOUND,
            BLACK_LOST
        );

        private BasicMovements basicMovements;
        private final EV3Led led = new EV3Led(EV3Led.Direction.LEFT);

        private final Transition BLACK_DETECTED_TRANSITION = new TransitionBuilder()
            .name("BLACK_DETECTED")
            .sourceState(BLACK_NOT_FOUND)
            .targetState(BLACK_FOUND)
            .eventType(BlackDetectedEvent.class)
            .eventHandler((event) -> {
                basicMovements.rotateToAngle(LEFT.angle);
                led.setPattern(3);
                System.out.println("BLACK_DETECTED");
            })
            .build();
        private final Transition BLACK_DETECTED_AGAIN_TRANSITION = new TransitionBuilder()
            .name("BLACK_DETECTED_AGAIN_TRANSITION")
            .sourceState(BLACK_LOST)
            .targetState(BLACK_FOUND)
            .eventType(BlackDetectedEvent.class)
            .eventHandler((event) -> {
                basicMovements.rotateToAngle(LEFT.angle);
                led.setPattern(3);
                System.out.println("BLACK_DETECTED_AGAIN_TRANSITION");
            })
            .build();
        private final Transition NOT_BLACK_DETECTED_TRANSITION = new TransitionBuilder()
            .name("NOT_BLACK_DETECTED")
            .sourceState(BLACK_FOUND)
            .targetState(BLACK_LOST)
            .eventType(NotBlackDetectedEvent.class)
            .eventHandler((event -> {
                basicMovements.rotateToAngle(RIGHT.angle);
                led.setPattern(2);
                System.out.println("NOT_BLACK_DETECTED");
            }))
            .build();
        private final Transition BLACK_DETECTION_TOO_OLD_TRANSITION = new TransitionBuilder()
            .name("BLACK_DETECTION_TOO_OLD")
            .sourceState(BLACK_LOST)
            .targetState(BLACK_NOT_FOUND)
            .period(2000)
            .eventType(PeriodicEvent.class)
            .eventHandler((event -> {
                basicMovements.rotateToAngle(0);
                basicMovements.backOff();
                basicMovements.forward();
                led.setPattern(0);
                System.out.println("BLACK_DETECTION_TOO_OLD");
            }))
            .build();

        private final Transition BLACK_DETECTION_STILL_TOO_OLD_TRANSITION = new TransitionBuilder()
            .name("BLACK_DETECTION_TOO_OLD")
            .sourceState(BLACK_NOT_FOUND)
            .targetState(BLACK_NOT_FOUND)
            .period(2000)
            .eventType(PeriodicEvent.class)
            .eventHandler((event -> {
                final var randomAngle = RANDOM.nextInt(15);
                final var sign = RANDOM.nextBoolean() ? 1 : -1;
                System.out.println("New random angle: " + randomAngle + ", sign: " + sign);
                basicMovements.rotateToAngle(randomAngle * sign);
                led.setPattern(0);
                System.out.println("BLACK_DETECTION_TOO_OLD");
            }))
            .build();

        private final Set<Transition> TRANSITIONS = Set.of(
            BLACK_DETECTED_TRANSITION,
            BLACK_DETECTED_AGAIN_TRANSITION,
            NOT_BLACK_DETECTED_TRANSITION,
            BLACK_DETECTION_TOO_OLD_TRANSITION,
            BLACK_DETECTION_STILL_TOO_OLD_TRANSITION
        );

        private final FiniteStateMachine fsm = new FiniteStateMachineBuilder(STATES, BLACK_NOT_FOUND)
            .registerTransitions(TRANSITIONS)
            .build();

        private FollowerStateMachine(final BasicMovements basicMovements) {
            this.basicMovements = basicMovements;
        }

        public void update(final int colorID) throws FiniteStateMachineException {
            fsm.fire(colorID == Color.BLACK ? new BlackDetectedEvent() : new NotBlackDetectedEvent());
            fsm.evaluatePeriodic();
        }

    }

    @AllArgsConstructor
    enum RotationKind {
        LEFT(-8), RIGHT(8);
        final int angle;
    }

}
