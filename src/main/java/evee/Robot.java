package evee;

import evee.basicMovements.BasicMovements;
import evee.follower.Follower;
import evee.obstacles.ObstaclesAvoider;
import lejos.robotics.subsumption.Arbitrator;
import lejos.robotics.subsumption.Behavior;

import static evee.utils.Utils.LOGGER;

@SuppressWarnings("FieldCanBeLocal")
public class Robot {

    private final BasicMovements basicMovements;
    private final ObstaclesAvoider obstaclesAvoider;
    private final Follower follower;

    private final Arbitrator arbitrator;

    public Robot() {
        this.basicMovements = new BasicMovements();
        this.obstaclesAvoider = new ObstaclesAvoider(this.basicMovements);
        this.follower = new Follower(this.basicMovements);
        this.arbitrator = new Arbitrator(new Behavior[]{this.basicMovements, this.follower, this.obstaclesAvoider});
    }

    private void runRobot() {
        LOGGER.info("Welcome!");
        this.arbitrator.go();
    }

    public static void main(final String[] args){
        new Robot().runRobot();
    }

}
