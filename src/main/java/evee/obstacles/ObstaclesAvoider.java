package evee.obstacles;

import ev3dev.sensors.ev3.EV3IRSensor;
import ev3dev.sensors.ev3.EV3TouchSensor;
import evee.basicMovements.BasicMovements;
import lejos.hardware.port.SensorPort;

public class ObstaclesAvoider {

    final EV3IRSensor irSensor;
    final EV3TouchSensor touchSensor;

    final BasicMovements basicMovements;

    public ObstaclesAvoider(BasicMovements basicMovements) {
        this.basicMovements = basicMovements;
        irSensor = new EV3IRSensor(SensorPort.S1);
        touchSensor = new EV3TouchSensor(SensorPort.S2);
    }

    public void handleObstacles() {
        handleObstaclesAtDistance();
        handleObstaclesAtTouch();
    }

    private void handleObstaclesAtTouch() {
        final var touch = isTouch();
        if (touch) {
            System.out.println("Obstacle found by touch");
            basicMovements.backOff();
            basicMovements.forward();
            basicMovements.moveAroundObstacle();
        }
    }

    private void handleObstaclesAtDistance() {
        final var distance = getDistance();
        if (distance < 50) {
            System.out.println("Obstacle found at a distance of " + distance + " cm");
            basicMovements.moveAroundObstacle();
        }
    }

    private boolean isTouch() {
        final var touchMode = touchSensor.getTouchMode();
        float[] touchSample = new float[touchMode.sampleSize()];
        touchMode.fetchSample(touchSample, 0);
        return ((int) touchSample[0]) == 1;
    }

    private int getDistance() {
        final var distanceMode = irSensor.getDistanceMode();
        float[] distanceSample = new float[distanceMode.sampleSize()];
        distanceMode.fetchSample(distanceSample, 0);
        return (int) distanceSample[0];
    }

}
