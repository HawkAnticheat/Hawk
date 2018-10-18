/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

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
