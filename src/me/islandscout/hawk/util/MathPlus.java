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
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

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
     * Faster implementations of the trigonometric functions
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

    //Perhaps make an angle method that compares two vectors and uses a lookup table for arccos values? Will eat up 256kiB of memory, though.
}
