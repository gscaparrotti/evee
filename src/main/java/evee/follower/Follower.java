package evee.follower;

import ev3dev.actuators.ev3.EV3Led;
import ev3dev.actuators.ev3.EV3Led.Direction;
import ev3dev.sensors.ev3.EV3ColorSensor;
import evee.basicMovements.BasicMovements;
import evee.utils.Utils;
import lejos.hardware.port.SensorPort;
import lejos.robotics.Color;
import lejos.robotics.subsumption.Behavior;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jeasy.states.api.*;
import org.jeasy.states.api.Transition.PeriodicEvent;
import org.jeasy.states.core.FiniteStateMachineBuilder;
import org.jeasy.states.core.TransitionBuilder;

import java.util.Set;

import static evee.follower.Follower.RotationKind.LEFT;
import static evee.follower.Follower.RotationKind.RIGHT;
import static evee.utils.Utils.LOGGER;
import static evee.utils.Utils.RANDOM;
import static lejos.robotics.Color.BLACK;

public class Follower implements Behavior {

    final BasicMovements basicMovements;
    final EV3ColorSensor colorSensor = new EV3ColorSensor(SensorPort.S3);

    final float[] buffer = new float[1];

    final FollowerStateMachine followerStateMachine;

    public Follower(BasicMovements basicMovements) {
        this.colorSensor.setFloodlight(Color.WHITE);
        this.basicMovements = basicMovements;
        this.followerStateMachine = new FollowerStateMachine(this.basicMovements);
    }

    @Override
    public boolean takeControl() {
        LOGGER.debug("Evaluating Follower");
        return basicMovements.isStarted();
    }

    @Override
    @SneakyThrows
    public void action() {
        this.followerStateMachine.update(getColorID());
    }

    @Override
    public void suppress() { }

    private int getColorID() {
        colorSensor.fetchSample(buffer, 0);
        final var colorID = (int) buffer[0];
        Utils.logColorID(colorID);
        return colorID;
    }

    private static class FollowerStateMachine {

        private final BlackDetectedEvent blackDetectedEvent;
        private final NotBlackDetectedEvent notBlackDetectedEvent;

        private static abstract class AbstractEventWithUpdatableTimestamp extends AbstractEvent {
            public AbstractEventWithUpdatableTimestamp withUpdatedTimestamp() {
                this.timestamp = System.currentTimeMillis();
                return this;
            }
        }

        private static class BlackDetectedEvent extends AbstractEventWithUpdatableTimestamp { }
        private static class NotBlackDetectedEvent extends AbstractEventWithUpdatableTimestamp { }

        private static final State BLACK_NOT_FOUND = new State("BLACK_NOT_FOUND");
        private static final State BLACK_FOUND = new State("BLACK_FOUND");
        private static final State BLACK_LOST = new State("BLACK_LOST");

        private static final Set<State> STATES = Set.of(
            BLACK_NOT_FOUND,
            BLACK_FOUND,
            BLACK_LOST
        );

        private BasicMovements basicMovements;
        private final EV3Led led = new EV3Led(Direction.LEFT);

        private final Transition BLACK_DETECTED_TRANSITION = new TransitionBuilder()
            .name("BLACK_DETECTED")
            .sourceState(BLACK_NOT_FOUND)
            .targetState(BLACK_FOUND)
            .eventType(BlackDetectedEvent.class)
            .eventHandler(event -> {
                basicMovements.rotateToAngle(LEFT.angle);
                led.setPattern(3);
                LOGGER.debug("BLACK_DETECTED");
            })
            .build();

        private final Transition BLACK_DETECTED_AGAIN_TRANSITION = new TransitionBuilder()
            .name("BLACK_DETECTED_AGAIN")
            .sourceState(BLACK_LOST)
            .targetState(BLACK_FOUND)
            .eventType(BlackDetectedEvent.class)
            .eventHandler(event -> {
                basicMovements.rotateToAngle(LEFT.angle);
                led.setPattern(3);
                LOGGER.debug("BLACK_DETECTED_AGAIN");
            })
            .build();

        private final Transition NOT_BLACK_DETECTED_TRANSITION = new TransitionBuilder()
            .name("NOT_BLACK_DETECTED")
            .sourceState(BLACK_FOUND)
            .targetState(BLACK_LOST)
            .eventType(NotBlackDetectedEvent.class)
            .eventHandler(event -> {
                basicMovements.rotateToAngle(RIGHT.angle);
                led.setPattern(2);
                LOGGER.debug("NOT_BLACK_DETECTED");
            })
            .build();

        private final Transition BLACK_DETECTION_TOO_OLD_TRANSITION = new TransitionBuilder()
            .name("BLACK_DETECTION_TOO_OLD")
            .sourceState(BLACK_LOST)
            .targetState(BLACK_NOT_FOUND)
            .period(2000)
            .eventType(PeriodicEvent.class)
            .eventHandler(event -> {
                basicMovements.rotateToAngle(0);
                basicMovements.backOff();
                basicMovements.forward();
                led.setPattern(0);
                LOGGER.debug("BLACK_DETECTION_TOO_OLD");
            })
            .build();

        private final Transition BLACK_NOT_FOUND_TRANSITION = new TransitionBuilder()
            .name("BLACK_NOT_FOUND")
            .sourceState(BLACK_NOT_FOUND)
            .targetState(BLACK_NOT_FOUND)
            .period(2000)
            .eventType(PeriodicEvent.class)
            .eventHandler(event -> {
                final var randomAngle = RANDOM.nextInt(15);
                final var sign = RANDOM.nextBoolean() ? 1 : -1;
                LOGGER.debug("New random angle: " + randomAngle + ", sign: " + sign);
                basicMovements.rotateToAngle(randomAngle * sign);
                led.setPattern(0);
                LOGGER.debug("BLACK_NOT_FOUND");
            })
            .build();

        private final Set<Transition> TRANSITIONS = Set.of(
            BLACK_DETECTED_TRANSITION,
            BLACK_DETECTED_AGAIN_TRANSITION,
            NOT_BLACK_DETECTED_TRANSITION,
            BLACK_DETECTION_TOO_OLD_TRANSITION,
            BLACK_NOT_FOUND_TRANSITION
        );

        private final FiniteStateMachine fsm = new FiniteStateMachineBuilder(STATES, BLACK_NOT_FOUND)
            .registerTransitions(TRANSITIONS)
            .build();

        private FollowerStateMachine(final BasicMovements basicMovements) {
            this.basicMovements = basicMovements;
            this.blackDetectedEvent = new BlackDetectedEvent();
            this.notBlackDetectedEvent = new NotBlackDetectedEvent();
        }

        public void update(final int colorID) throws FiniteStateMachineException {
            fsm.fire(getEvent(colorID));
            fsm.evaluatePeriodic();
        }

        private Event getEvent(int colorID) {
            return colorID == BLACK
                ? this.blackDetectedEvent.withUpdatedTimestamp()
                : this.notBlackDetectedEvent.withUpdatedTimestamp();
        }

    }

    @AllArgsConstructor
    enum RotationKind {
        LEFT(-32), RIGHT(32);
        final int angle;
    }

}
