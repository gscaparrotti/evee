package evee.obstacles;

import ev3dev.actuators.Sound;
import ev3dev.sensors.ev3.EV3IRSensor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import evee.basicMovements.BasicMovements;
import lejos.hardware.port.SensorPort;
import lejos.robotics.subsumption.Behavior;
import org.apache.commons.collections4.queue.CircularFifoQueue;

public class ObstaclesAvoider implements Behavior {

    final EV3IRSensor irSensor;
    final EV3TouchSensor touchSensor;
    final Sound sound = Sound.getInstance();

    final BasicMovements basicMovements;

    final CircularFifoQueue<Long> obstacles = new CircularFifoQueue<>(3);
    boolean invertedDirection = false;

    volatile boolean interrupted = false;

    public ObstaclesAvoider(BasicMovements basicMovements) {
        this.basicMovements = basicMovements;
        irSensor = new EV3IRSensor(SensorPort.S1);
        touchSensor = new EV3TouchSensor(SensorPort.S2);
    }

    @Override
    public boolean takeControl() {
        return this.isTouch() || this.isCloseDistance();
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
        obstacles.add(System.currentTimeMillis());
        if (obstacles.isAtFullCapacity()) {
            final var oldest = obstacles.poll();
            if (oldest != null && System.currentTimeMillis() - oldest < 10000) {
                System.out.println("Too many obstacles, inverting direction");
                sound.beep();
                this.invertedDirection = !this.invertedDirection;
                obstacles.clear();
            }
        }
        handleObstaclesAtDistance();
        handleObstaclesAtTouch();
    }

    private void handleObstaclesAtTouch() {
        final var touch = isTouch();
        if (touch) {
            System.out.println("Obstacle found by touch");
            this.circumvent();
        }
    }

    private void handleObstaclesAtDistance() {
        final var closeDistance = isCloseDistance();
        if (closeDistance) {
            System.out.println("Obstacle found at a distance of " + closeDistance + " cm");
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
        final var touchMode = touchSensor.getTouchMode();
        float[] touchSample = new float[touchMode.sampleSize()];
        touchMode.fetchSample(touchSample, 0);
        return ((int) touchSample[0]) == 1;
    }

    private boolean isCloseDistance() {
        final var distanceMode = irSensor.getDistanceMode();
        float[] distanceSample = new float[distanceMode.sampleSize()];
        distanceMode.fetchSample(distanceSample, 0);
        return ((int) distanceSample[0]) < 50;
    }

}
