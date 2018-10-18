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

public final class MathPlus {

    private MathPlus() {
    }

    public static double round(double number, int decimals) {
        number *= Math.pow(10, decimals);
        number = Math.round(number);
        return number / Math.pow(10, decimals);
    }

    public static double clampDegrees360(double angleDegrees) {
        angleDegrees %= 360;
        if (angleDegrees < 0)
            angleDegrees = 360 + angleDegrees;
        return angleDegrees;
    }

    //Perhaps make an angle method that compares two vectors and uses a lookup table for arccos values? Will eat up 256kiB of memory, though.
}
