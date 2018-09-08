package me.islandscout.hawk.utils;

public final class MathPlus {

    private MathPlus() {}

    public static double round(double number, int decimals) {
        number *= Math.pow(10, decimals);
        number = Math.round(number);
        return number / Math.pow(10, decimals);
    }

    public static double clampDegrees360(double angleDegrees) {
        angleDegrees %= 360;
        if(angleDegrees < 0)
            angleDegrees = 360 + angleDegrees;
        return angleDegrees;
    }
}
