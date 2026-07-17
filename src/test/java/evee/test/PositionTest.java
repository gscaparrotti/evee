package evee.test;

import evee.custom.SteeringPilot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PositionTest {

    private static final double WHEEL_DIAMETER = 42.0;
    private static final double TURN_RADIUS = 155.0;
    private static final double DELTA = 1e-6;

    /** Distanza percorsa da una rotazione completa della ruota motrice. */
    private static final double STEP_DISTANCE = (WHEEL_DIAMETER / 2) * Math.PI * 2;

    private static SteeringPilot newPilot() {
        return new SteeringPilot(null, null, WHEEL_DIAMETER, TURN_RADIUS);
    }

    /** Listener di comodo che tiene traccia dell'ultima posizione registrata. */
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

        SteeringPilot.OrientedPosition pose() {
            return movement.getOrientedPosition();
        }
    }

    @Test
    public void straightMovementsAccumulateDistanceAlongHeading() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        steeringPilot.move(SteeringPilot.Direction.STRAIGHT);
        steeringPilot.move(SteeringPilot.Direction.STRAIGHT);
        steeringPilot.move(SteeringPilot.Direction.STRAIGHT);

        final var pose = listener.pose();
        assertEquals(3 * STEP_DISTANCE, pose.getX(), DELTA);
        assertEquals(0.0, pose.getY(), DELTA);
        assertEquals(0.0, pose.getOrientation(), DELTA);
    }

    @Test
    public void singleTurnMatchesClosedFormUsingTheCalibratedRadius() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        steeringPilot.move(SteeringPilot.Direction.RIGHT);

        final var expectedR = TURN_RADIUS;
        final var expectedDTheta = STEP_DISTANCE / expectedR;
        // Partendo da (0,0) con heading 0, l'ICC è (0, expectedR):
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
        rightPilot.move(SteeringPilot.Direction.RIGHT);

        final var leftPilot = newPilot();
        final var leftListener = new RecordingListener();
        leftPilot.addMovementListener(leftListener);
        leftPilot.move(SteeringPilot.Direction.LEFT);

        final var rightPose = rightListener.pose();
        final var leftPose = leftListener.pose();

        // Sterzando allo stesso angolo ma in direzioni opposte, il robot deve percorrere
        // traiettorie speculari rispetto all'asse x (stessa x, y e heading opposti).
        assertEquals(rightPose.getX(), leftPose.getX(), DELTA);
        assertEquals(rightPose.getY(), -leftPose.getY(), DELTA);
        assertEquals(rightPose.getOrientation(), -leftPose.getOrientation(), DELTA);
    }

    @Test
    public void oppositeTurnsOfEqualMagnitudeCancelHeadingChange() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        steeringPilot.move(SteeringPilot.Direction.RIGHT);
        steeringPilot.move(SteeringPilot.Direction.LEFT);

        assertEquals(0.0, listener.pose().getOrientation(), DELTA);
    }

    @Test
    public void repeatedSameDirectionTurnsStayOnTheSameCircle() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        // Centro istantaneo di curvatura, costante per sterzate consecutive nella stessa
        // direzione (heading iniziale = 0).
        final var iccX = 0.0;
        final var iccY = TURN_RADIUS;

        for (int i = 0; i < 8; i++) {
            steeringPilot.move(SteeringPilot.Direction.RIGHT);
            final var pose = listener.pose();
            final var distanceFromIcc = Math.hypot(pose.getX() - iccX, pose.getY() - iccY);
            assertEquals(TURN_RADIUS, distanceFromIcc, DELTA,
                    "La posizione deve restare sulla stessa circonferenza per sterzate consecutive nella stessa direzione");
        }
    }

    @Test
    public void turnRadiusMatchesTheCalibratedConstant() {
        // Non ricalcola R con la stessa formula usata internamente da logMovement:
        // ricava il raggio effettivo dai soli valori misurabili (arco percorso e
        // variazione di heading riportata) e lo confronta con la costante calibrata
        // turnRadius passata al costruttore, che è il raggio di sterzata reale.
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        steeringPilot.move(SteeringPilot.Direction.RIGHT);

        final var dTheta = listener.pose().getOrientation();
        final var effectiveRadius = Math.abs(STEP_DISTANCE / dTheta);

        assertEquals(TURN_RADIUS, effectiveRadius, DELTA,
                "Il raggio di sterzata calcolato dovrebbe corrispondere alla costante calibrata turnRadius");
    }

    @Test
    public void enoughConsecutiveTurnsCloseAFullCircleBackNearTheStart() {
        final var steeringPilot = newPilot();
        final var listener = new RecordingListener();
        steeringPilot.addMovementListener(listener);

        final var dThetaPerTurn = STEP_DISTANCE / TURN_RADIUS;
        final var turnsForFullCircle = (int) Math.round((2 * Math.PI) / dThetaPerTurn);

        for (int i = 0; i < turnsForFullCircle; i++) {
            steeringPilot.move(SteeringPilot.Direction.RIGHT);
        }

        final var pose = listener.pose();
        final var distanceFromStart = Math.hypot(pose.getX(), pose.getY());

        // Il numero di sterzate scelto è quello che, per il raggio calibrato, copre
        // il giro più vicino a 360°: il robot deve quindi ritrovarsi vicino al punto
        // di partenza, ben all'interno del raggio di curvatura.
        assertTrue(distanceFromStart < TURN_RADIUS,
                "Dopo circa un giro completo il robot dovrebbe trovarsi vicino al punto di partenza");
    }
}