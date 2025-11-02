package evee.custom;

import lejos.robotics.RegulatedMotor;
import lejos.robotics.RegulatedMotorListener;
import lombok.AllArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
public class CombinedMotor implements RegulatedMotor {

    final RegulatedMotor motorLeft;
    final RegulatedMotor motorRight;

    @Override
    public void addListener(RegulatedMotorListener listener) {
        motorLeft.addListener(listener);
        motorRight.addListener(listener);
    }

    @Override
    public RegulatedMotorListener removeListener() {
        final var leftListener = motorLeft.removeListener();
        final var rightListener = motorRight.removeListener();
        assert Objects.equals(leftListener, rightListener);
        return leftListener;
    }

    @Override
    public void stop(boolean immediateReturn) {
        motorLeft.stop(immediateReturn);
        motorRight.stop(immediateReturn);
    }

    @Override
    public void flt(boolean immediateReturn) {
        motorLeft.flt(immediateReturn);
        motorRight.flt(immediateReturn);
    }

    @Override
    public void waitComplete() {
        motorLeft.waitComplete();
        motorRight.waitComplete();
    }

    @Override
    public void rotate(int angle, boolean immediateReturn) {
        motorLeft.rotate(angle, immediateReturn);
        motorRight.rotate(angle, immediateReturn);
    }

    @Override
    public void rotate(int angle) {
        motorLeft.rotate(angle);
        motorRight.rotate(angle);
    }

    @Override
    public void rotateTo(int limitAngle) {
        motorLeft.rotateTo(limitAngle);
        motorRight.rotateTo(limitAngle);
    }

    @Override
    public void rotateTo(int limitAngle, boolean immediateReturn) {
        motorLeft.rotateTo(limitAngle, immediateReturn);
        motorRight.rotateTo(limitAngle, immediateReturn);
    }

    @Override
    public void setSpeed(int speed) {
        motorLeft.setSpeed(speed);
        motorRight.setSpeed(speed);
    }

    @Override
    public int getSpeed() {
        final var leftSpeed = motorLeft.getSpeed();
        final var rightSpeed = motorRight.getSpeed();
        assert Objects.equals(leftSpeed, rightSpeed);
        return leftSpeed;
    }

    @Override
    public float getMaxSpeed() {
        final var leftMaxSpeed = motorLeft.getMaxSpeed();
        final var rightMaxSpeed = motorRight.getMaxSpeed();
        assert Objects.equals(leftMaxSpeed, rightMaxSpeed);
        return leftMaxSpeed;
    }

    @Override
    public boolean isStalled() {
        final var leftIsStalled = motorLeft.isStalled();
        final var rightIsStalled = motorRight.isStalled();
        return leftIsStalled || rightIsStalled;
    }

    @Override
    public void setAcceleration(int acceleration) {
        motorLeft.setAcceleration(acceleration);
        motorRight.setAcceleration(acceleration);
    }

    @Override
    public void synchronizeWith(RegulatedMotor[] syncList) {
        motorLeft.synchronizeWith(syncList);
        motorRight.synchronizeWith(syncList);
    }

    @Override
    public void startSynchronization() {
        motorLeft.startSynchronization();
        motorRight.startSynchronization();
    }

    @Override
    public void endSynchronization() {
        motorLeft.endSynchronization();
        motorRight.endSynchronization();
    }

    @Override
    public void forward() {
        motorLeft.forward();
        motorRight.forward();
    }

    @Override
    public void backward() {
        motorLeft.backward();
        motorRight.backward();
    }

    @Override
    public void stop() {
        motorLeft.stop();
        motorRight.stop();
    }

    @Override
    public void flt() {
        motorLeft.flt();
        motorRight.flt();
    }

    @Override
    public boolean isMoving() {
        final var leftIsMoving = motorLeft.isMoving();
        final var rightIsMoving = motorRight.isMoving();
        return leftIsMoving || rightIsMoving;
    }

    @Override
    public int getRotationSpeed() {
        final var leftRotationSpeed = motorLeft.getRotationSpeed();
        final var rightRotationSpeed = motorRight.getRotationSpeed();
        assert Objects.equals(leftRotationSpeed, rightRotationSpeed);
        return leftRotationSpeed;
    }

    @Override
    public int getTachoCount() {
        final var leftTachoCount = motorLeft.getTachoCount();
        final var rightTachoCount = motorRight.getTachoCount();
        assert Objects.equals(leftTachoCount, rightTachoCount);
        return leftTachoCount;
    }

    @Override
    public void resetTachoCount() {
        motorLeft.resetTachoCount();
        motorRight.resetTachoCount();
    }

    @Override
    public void coast() {
        motorLeft.coast();
        motorRight.coast();
    }

    @Override
    public void brake() {
        motorLeft.brake();
        motorRight.brake();
    }

    @Override
    public void hold() {
        motorLeft.hold();
        motorRight.hold();
    }
}
