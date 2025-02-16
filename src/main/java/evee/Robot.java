package evee;

import evee.basicMovements.BasicMovements;
import evee.follower.Follower;
import evee.obstacles.ObstaclesAvoider;

@SuppressWarnings("FieldCanBeLocal")
public class Robot {

    private BasicMovements basicMovements;
    private ObstaclesAvoider obstaclesAvoider;
    private Follower follower;

    @SuppressWarnings("InfiniteLoopStatement")
    private void runRobot() {
        System.out.println("Welcome!");
        prepareEnvironment();
        while (true) {
            this.basicMovements.forward();
            this.follower.updateRotation();
            this.obstaclesAvoider.handleObstacles();
        }
    }

    private void prepareEnvironment() {
        this.basicMovements = new BasicMovements();
        this.obstaclesAvoider = new ObstaclesAvoider(this.basicMovements);
        this.follower = new Follower(this.basicMovements);
    }

    public static void main(final String[] args){
        new Robot().runRobot();
    }

}
