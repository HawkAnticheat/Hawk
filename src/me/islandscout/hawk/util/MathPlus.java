/*
 * This file is part of Hawk Anticheat.
 * Copyright (C) 2018 Hawk Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.util;

import me.islandscout.hawk.Hawk;
import org.bukkit.util.Vector;

import java.util.List;

public final class MathPlus {

    private MathPlus() {
    }

    public static double round(double number, int decimals) {
        number *= Math.pow(10, decimals);
        number = Math.round(number);
        return number / Math.pow(10, decimals);
    }

    public static float clampDegrees360(float angleDegrees) {
        angleDegrees %= 360;
        if (angleDegrees < 0)
            angleDegrees = 360 + angleDegrees;
        return angleDegrees;
    }

    public static double distance2d(double x, double y) {
        return Math.sqrt(x*x + y*y);
    }

    public static Vector getDirection(float yaw, float pitch) {
        Vector vector = new Vector();
        float rotX = (float)Math.toRadians(yaw);
        float rotY = (float)Math.toRadians(pitch);
        vector.setY(-sin(rotY));
        double xz = cos(rotY);
        vector.setX(-xz * sin(rotX));
        vector.setZ(xz * cos(rotX));
        return vector;
    }

    /**
     * Returns the angle between two non-zero, finite vectors without ever returning
     * NaN, unlike Bukkit's Vector#angle(Vector)
     */
    public static double angle(Vector a, Vector b) {
        double dot = Math.min(Math.max(a.dot(b) / (a.length() * b.length()), -1), 1);
        return Math.acos(dot);
    }

    /**
     * Faster implementations of trigonometric functions
     */
    public static float sin(float radians) {
        if (Hawk.getServerVersion() == 8)
            return net.minecraft.server.v1_8_R3.MathHelper.sin(radians);
        if (Hawk.getServerVersion() == 7)
            return net.minecraft.server.v1_7_R4.MathHelper.sin(radians);
        return (float)Math.sin(radians);
    }

    public static float cos(float radians) {
        if (Hawk.getServerVersion() == 8)
            return net.minecraft.server.v1_8_R3.MathHelper.cos(radians);
        if (Hawk.getServerVersion() == 7)
            return net.minecraft.server.v1_7_R4.MathHelper.cos(radians);
        return (float)Math.cos(radians);
    }

    public static void rotateVectorsEulerXYZ(Vector[] vertices, float radX, float radY, float radZ) {
        for(Vector vertex : vertices) {
            double x, y, z;

            //rotate around X axis (pitch)
            z = vertex.getZ();
            y = vertex.getY();
            vertex.setZ(z * cos(radX) - y * sin(radX));
            vertex.setY(z * sin(radX) + y * cos(radX));

            //rotate around Y axis (yaw)
            x = vertex.getX();
            z = vertex.getZ();
            vertex.setX(x * cos(radY) - z * sin(radY));
            vertex.setZ(x * sin(radY) + z * cos(radY));

            //rotate around Z axis (roll)
            x = vertex.getX();
            y = vertex.getY();
            vertex.setX(x * cos(radZ) - y * sin(radZ));
            vertex.setY(x * sin(radZ) + y * cos(radZ));
        }
    }

    public static double stdev(List<Double> data) {
        double[] array = new double[data.size()];
        for(int i = 0; i < array.length; i++) {
            array[i] = data.get(i);
        }
        return stdev(array);
    }

    public static double stdev(double[] data) {
        double mean = mean(data);
        double dividend = 0;
        for(double num : data) {
            dividend += Math.pow(num - mean, 2);
        }
        return Math.sqrt(dividend / (data.length - 1));
    }

    public static double mean(double[] data) {
        double ans = 0;
        for(double num : data) {
            ans += num;
        }
        ans /= data.length;
        return ans;
    }

    public static double mean(List<Double> data) {
        double[] array = new double[data.size()];
        for(int i = 0; i < array.length; i++) {
            array[i] = data.get(i);
        }
        return mean(array);
    }

    public static double range(double[] data) {
        double high = Double.NEGATIVE_INFINITY;
        double low = Double.POSITIVE_INFINITY;
        for(Double num : data) {
            if(num > high)
                high = num;
            if(num < low)
                low = num;
        }
        return high - low;
    }

    public static double range(List data) {
        double[] array = new double[data.size()];
        for(int i = 0; i < array.length; i++) {
            array[i] = ((Number)data.get(i)).doubleValue();
        }
        return range(array);
    }

    public static double min(List data) {
        double min = Double.MAX_VALUE;
        for (Object datum : data) {
            double value = ((Number) datum).doubleValue();
            if (value < min)
                min = value;
        }
        return min;
    }

    public static double max(List data) {
        double max = -Double.MAX_VALUE;
        for (Object datum : data) {
            double value = ((Number) datum).doubleValue();
            if (value > max)
                max = value;
        }
        return max;
    }

    public static float derivative(MathFunction func, double x) {
        double h = (x * 1E-8);
        return (float)((func.func(x + h) - func.func(x))/h);
    }

    //rough approximate
    public static float integral(MathFunction func, double lowerBound, double upperBound) {
        int trapezoids = 128;
        double trapWidth = (upperBound - lowerBound) / trapezoids;
        double x0 = lowerBound;
        double sum = 0;
        for(double x1 = lowerBound + trapWidth; x1 <= upperBound; x1+=trapWidth) {
            sum += func.func(x1) + func.func(x0);
            x0 = x1;
        }
        return (float)((trapWidth / 2D) * sum);
    }

    public static double truncateRound(double num, double sigFigs) {
        int numExponent = (int)Math.floor(Math.log10(num));
        int shift = (int) Math.pow(10, sigFigs - numExponent - 1);
        double result = Math.round(num * shift);
        return result / shift;
    }

    public static float gcdRational(float a, float b) {
        if(a == 0) {
            return b;
        }
        int quotient = getIntQuotient(b, a);
        float remainder = ((b / a) - quotient) * a;
        if(Math.abs(remainder) < Math.max(a, b) * 1E-3F)
            remainder = 0;
        return gcdRational(remainder, a);
    }

    public static float gcdRational(List<Float> numbers) {
        float result = numbers.get(0);
        for (int i = 1; i < numbers.size(); i++) {
            result = gcdRational(numbers.get(i), result);
        }
        return result;
    }

    public static int getIntQuotient(float dividend, float divisor) {
        float ans = dividend / divisor;
        float error = Math.max(dividend, divisor) * 1E-3F;
        return (int)(ans + error);
    }
}
