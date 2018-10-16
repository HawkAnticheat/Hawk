package me.islandscout.hawk.utils;

public final class PhysicsUtils {

    private PhysicsUtils() {
    }

    public static double waterYPosFunc(double initVelocityY, double deltaTime) {
        deltaTime++;
        return -0.1 * deltaTime - 5 * Math.pow(0.8, deltaTime) * (0.1 + initVelocityY) + 5 * (0.1 + initVelocityY);
    }

    public static double waterYVelFunc(double initVelocityY, double deltaTime) {
        return (0.1 + initVelocityY) * Math.pow(0.8, deltaTime) - 0.1;
    }

    public static double airYPosFunc(double initVelocityY, double deltaTime) {
        deltaTime++;
        return -3.92 * deltaTime - 50 * Math.pow(0.98, deltaTime) * (3.92 + initVelocityY) + 50 * (3.92 + initVelocityY);
    }

    public static double airYVelFunc(double initVelocityY, double deltaTime) {
        return (3.92 + initVelocityY) * Math.pow(0.98, deltaTime) - 3.92;
    }
}
