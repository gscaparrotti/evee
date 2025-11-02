package evee.obstacles;

import ev3dev.sensors.ev3.EV3IRSensor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import evee.basicMovements.BasicMovements;
import evee.custom.MotorUtils;
import evee.utils.Notifications;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.SensorMode;
import lejos.robotics.subsumption.Behavior;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import static evee.basicMovements.BasicMovements.STRAIGHT_MOTOR_SPEED;
import static evee.basicMovements.BasicMovements.TURN_MOTOR_SPEED;
import static evee.utils.Notifications.Beep.*;
import static evee.utils.Notifications.beep;
import static evee.utils.Utils.LOGGER;
import static evee.utils.Utils.RANDOM;

public class ObstaclesAvoider implements Behavior {

    final BasicMovements basicMovements;

    private final CircularFifoQueue<Long> obstacles = new CircularFifoQueue<>(3);
    private boolean invertedDirection = false;

    private volatile boolean interrupted = false;
    private volatile Thread sleepingThread = null;

    private final SensorMode touchMode;
    private final float[] touchSample;
    private final SensorMode distanceMode;
    private final float[] distanceSample;

    private final boolean[] obstaclesDetected = new boolean[] {false, false};

    public ObstaclesAvoider(BasicMovements basicMovements) {
        this.basicMovements = basicMovements;
        final var irSensor = new EV3IRSensor(SensorPort.S1);
        final var touchSensor = new EV3TouchSensor(SensorPort.S2);
        this.touchMode =  touchSensor.getTouchMode();
        this.touchSample = new float[touchMode.sampleSize()];
        this.distanceMode = irSensor.getDistanceMode();
        this.distanceSample = new float[distanceMode.sampleSize()];
    }

    @Override
    public boolean takeControl() {
        this.obstaclesDetected[0] = this.isTouch();
        this.obstaclesDetected[1] = this.isCloseDistance();
        return this.obstaclesDetected[0] || this.obstaclesDetected[1];
    }

    @Override
    public void action() {
        this.interrupted = false;
        this.handleObstacles();
    }

    @Override
    public void suppress() {
        this.interrupted = true;
        if (this.sleepingThread != null) {
            this.sleepingThread.interrupt();
        }
    }

    public void handleObstacles() {
        beep(SINGLE_MEDIUM_BEEP);
        obstacles.add(System.currentTimeMillis());
        if (obstacles.isAtFullCapacity()) {
            final var oldest = obstacles.poll();
            if (oldest != null && System.currentTimeMillis() - oldest < 10000) {
                LOGGER.debug("Too many obstacles, inverting direction");
                beep(DOUBLE_BEEP);
                this.invertedDirection = !this.invertedDirection;
                obstacles.clear();
            }
        }
        handleObstaclesAtDistance();
        handleObstaclesAtTouch();
    }

    private void handleObstaclesAtTouch() {
        final var touch = this.obstaclesDetected[0];
        if (touch) {
            LOGGER.debug("Obstacle found by touch");
            this.circumvent();
        }
    }

    private void handleObstaclesAtDistance() {
        final var closeDistance = this.obstaclesDetected[1];
        if (closeDistance) {
            LOGGER.debug("Obstacle found");
            this.circumvent();
        }
    }

    private void circumvent() {
        if (!this.interrupted) {
            basicMovements.rotateToAngle(0);
        }
        if (!this.interrupted) {
            basicMovements.backOff();
        }
        if (!this.interrupted) {
            basicMovements.forward();
        }
        if (!this.interrupted) {
            moveAroundObstacle(this.invertedDirection);
        }
    }

    private void moveAroundObstacle(final boolean invertedDirection) {
        try {
            basicMovements.setSpeedForBothMotors(TURN_MOTOR_SPEED);
            LOGGER.debug("Rotation started");
            LOGGER.debug("First part of rotation");
            final var direction = RANDOM.nextInt(100) > 20;
            var angle = direction ? 20 : -20;
            if (invertedDirection) {
                angle = -angle;
            }
            basicMovements.rotateToAngle(angle);
            this.sleepingThread = Thread.currentThread();
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
            LOGGER.debug("Rotation interrupted");
            Notifications.beep(SINGLE_VERY_HIGH_BEEP);
        } finally {
            this.sleepingThread = null;
            LOGGER.debug("Second part of rotation");
            basicMovements.rotateToAngle(0);
            LOGGER.debug("Rotation completed");
            basicMovements.setSpeedForBothMotors(STRAIGHT_MOTOR_SPEED);
        }
    }

    private boolean isTouch() {
        touchMode.fetchSample(touchSample, 0);
        final var isOverloaded = MotorUtils.isOverloaded(basicMovements.getMotorLeft()) || MotorUtils.isOverloaded(basicMovements.getMotorRight());
        if (isOverloaded) {
            beep(SINGLE_HIGH_BEEP);
        }
        return ((int) touchSample[0]) == 1 || isOverloaded;
    }

    private boolean isCloseDistance() {
        distanceMode.fetchSample(distanceSample, 0);
        return ((int) distanceSample[0]) < 50;
    }

}
