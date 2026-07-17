package evee.test;

import evee.custom.SteeringPilot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PositionTest {

    private static final double WHEEL_DIAMETER = 42.0;
    private static final double TURN_RADIUS = 155.0;
    private static final double DELTA = 1e-6;

    /** Distance travelled by one full rotation of the drive wheel. */
    private static final double STEP_DISTANCE = (WHEEL_DIAMETER / 2) * Math.PI * 2;

    private static SteeringPilot newPilot() {
        return new SteeringPilot(null, null, WHEEL_DIAMETER, TURN_RADIUS);
    }

    /** Convenience listener that keeps track of the last recorded position. */
    private static class RecordingListener implements SteeringPilot.MovementListener {
        SteeringPilot.Movement movement;

        @Override
        public SteeringPilot.Movement getPreviousMovement() {
            return movement;
        }

        @Override
        public void movementEnded(SteeringPilot.Movement movement) {
            this.movement = movement;
        }

        @Override
        public void markObstacleFound() {
        }

        SteeringPilot.OrientedPosition pose() {
            return movement.getOrientedPosition();
        }
    }

    @Test
    public void straightMovementsAccumulateDistanceAlongHeading() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        steeringPilot.move(SteeringPilot.Direction.STRAIGHT, SteeringPilot.Bearing.FORWARD);
        steeringPilot.move(SteeringPilot.Direction.STRAIGHT, SteeringPilot.Bearing.FORWARD);
        steeringPilot.move(SteeringPilot.Direction.STRAIGHT, SteeringPilot.Bearing.FORWARD);

        final var pose = listener.pose();
        assertEquals(3 * STEP_DISTANCE, pose.getX(), DELTA);
        assertEquals(0.0, pose.getY(), DELTA);
        assertEquals(0.0, pose.getOrientation(), DELTA);
    }

    @Test
    public void backwardStraightMovementSubtractsDistanceAlongHeading() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        steeringPilot.move(SteeringPilot.Direction.STRAIGHT, SteeringPilot.Bearing.FORWARD);
        steeringPilot.move(SteeringPilot.Direction.STRAIGHT, SteeringPilot.Bearing.BACKWARD);

        final var pose = listener.pose();
        assertEquals(0.0, pose.getX(), DELTA);
        assertEquals(0.0, pose.getY(), DELTA);
        assertEquals(0.0, pose.getOrientation(), DELTA);
    }

    @Test
    public void backwardTurnAppliesTheOppositeHeadingChangeOfTheEquivalentForwardTurn() {
        final var forwardPilot = newPilot();
        final var forwardListener = new RecordingListener();
        forwardPilot.addMovementListener(forwardListener);
        forwardPilot.move(SteeringPilot.Direction.RIGHT, SteeringPilot.Bearing.FORWARD);

        final var backwardPilot = newPilot();
        final var backwardListener = new RecordingListener();
        backwardPilot.addMovementListener(backwardListener);
        backwardPilot.move(SteeringPilot.Direction.RIGHT, SteeringPilot.Bearing.BACKWARD);

        final var forwardPose = forwardListener.pose();
        final var backwardPose = backwardListener.pose();

        // Travelling the distance in the opposite direction flips the sign of dTheta:
        // x and heading are negated, while y (an even function of dTheta around the
        // shared ICC) stays unchanged.
        assertEquals(forwardPose.getX(), -backwardPose.getX(), DELTA);
        assertEquals(forwardPose.getY(), backwardPose.getY(), DELTA);
        assertEquals(forwardPose.getOrientation(), -backwardPose.getOrientation(), DELTA);
    }

    @Test
    public void singleTurnMatchesClosedFormUsingTheCalibratedRadius() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        steeringPilot.move(SteeringPilot.Direction.RIGHT, SteeringPilot.Bearing.FORWARD);

        final var expectedR = TURN_RADIUS;
        final var expectedDTheta = STEP_DISTANCE / expectedR;
        // Starting from (0,0) with heading 0, the ICC is (0, expectedR):
        // x = expectedR * sin(dTheta), y = expectedR * (1 - cos(dTheta))
        final var expectedX = expectedR * Math.sin(expectedDTheta);
        final var expectedY = expectedR * (1 - Math.cos(expectedDTheta));

        final var pose = listener.pose();
        assertEquals(expectedX, pose.getX(), DELTA);
        assertEquals(expectedY, pose.getY(), DELTA);
        assertEquals(expectedDTheta, pose.getOrientation(), DELTA);
    }

    @Test
    public void leftAndRightTurnsAreMirrorImagesOfEachOther() {
        final var rightPilot = newPilot();
        final var rightListener = new RecordingListener();
        rightPilot.addMovementListener(rightListener);
        rightPilot.move(SteeringPilot.Direction.RIGHT, SteeringPilot.Bearing.FORWARD);

        final var leftPilot = newPilot();
        final var leftListener = new RecordingListener();
        leftPilot.addMovementListener(leftListener);
        leftPilot.move(SteeringPilot.Direction.LEFT, SteeringPilot.Bearing.FORWARD);

        final var rightPose = rightListener.pose();
        final var leftPose = leftListener.pose();

        // Steering at the same angle but in opposite directions, the robot must follow
        // mirror-image trajectories about the x axis (same x, opposite y and heading).
        assertEquals(rightPose.getX(), leftPose.getX(), DELTA);
        assertEquals(rightPose.getY(), -leftPose.getY(), DELTA);
        assertEquals(rightPose.getOrientation(), -leftPose.getOrientation(), DELTA);
    }

    @Test
    public void oppositeTurnsOfEqualMagnitudeCancelHeadingChange() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        steeringPilot.move(SteeringPilot.Direction.RIGHT, SteeringPilot.Bearing.FORWARD);
        steeringPilot.move(SteeringPilot.Direction.LEFT, SteeringPilot.Bearing.FORWARD);

        assertEquals(0.0, listener.pose().getOrientation(), DELTA);
    }

    @Test
    public void repeatedSameDirectionTurnsStayOnTheSameCircle() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        // Instantaneous center of curvature, constant for consecutive turns in the same
        // direction (starting heading = 0).
        final var iccX = 0.0;
        //noinspection UnnecessaryLocalVariable
        final var iccY = TURN_RADIUS;

        for (int i = 0; i < 8; i++) {
            steeringPilot.move(SteeringPilot.Direction.RIGHT, SteeringPilot.Bearing.FORWARD);
            final var pose = listener.pose();
            final var distanceFromIcc = Math.hypot(pose.getX() - iccX, pose.getY() - iccY);
            assertEquals(TURN_RADIUS, distanceFromIcc, DELTA,
                    "The position must stay on the same circle for consecutive turns in the same direction");
        }
    }

    @Test
    public void turnRadiusMatchesTheCalibratedConstant() {
        // Does not recompute R with the same formula used internally by logMovement:
        // it derives the effective radius purely from measurable values (the arc
        // travelled and the reported heading change) and compares it against the
        // calibrated turnRadius constant passed to the constructor, which is the
        // real steering radius.
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        steeringPilot.move(SteeringPilot.Direction.RIGHT, SteeringPilot.Bearing.FORWARD);

        final var dTheta = listener.pose().getOrientation();
        final var effectiveRadius = Math.abs(STEP_DISTANCE / dTheta);

        assertEquals(TURN_RADIUS, effectiveRadius, DELTA,
                "The computed steering radius should match the calibrated turnRadius constant");
    }

    @Test
    public void enoughConsecutiveTurnsCloseAFullCircleBackNearTheStart() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        final var dThetaPerTurn = STEP_DISTANCE / TURN_RADIUS;
        final var turnsForFullCircle = (int) Math.round((2 * Math.PI) / dThetaPerTurn);

        for (int i = 0; i < turnsForFullCircle; i++) {
            steeringPilot.move(SteeringPilot.Direction.RIGHT, SteeringPilot.Bearing.FORWARD);
        }

        final var pose = listener.pose();
        final var distanceFromStart = Math.hypot(pose.getX(), pose.getY());

        // The chosen number of turns is the one that, for the calibrated radius, covers
        // the loop closest to 360°: the robot should therefore end up near its starting
        // point, well within the turning radius.
        assertTrue(distanceFromStart < TURN_RADIUS,
                "After roughly one full loop the robot should end up near its starting point");
    }
}