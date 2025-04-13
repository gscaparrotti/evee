package evee.obstacles;

import ev3dev.sensors.ev3.EV3IRSensor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import evee.basicMovements.BasicMovements;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.SensorMode;
import lejos.robotics.subsumption.Behavior;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import static evee.utils.Notifications.Beep.*;
import static evee.utils.Notifications.beep;
import static evee.utils.Utils.LOGGER;

public class ObstaclesAvoider implements Behavior {

    final BasicMovements basicMovements;

    private final CircularFifoQueue<Long> obstacles = new CircularFifoQueue<>(3);
    private boolean invertedDirection = false;

    private volatile boolean interrupted = false;

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
            basicMovements.moveAroundObstacle(this.invertedDirection);
        }
    }

    private boolean isTouch() {
        touchMode.fetchSample(touchSample, 0);
        final var isOverloaded = basicMovements.getMotorLeft().isOverloaded() || basicMovements.getMotorRight().isOverloaded();
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
