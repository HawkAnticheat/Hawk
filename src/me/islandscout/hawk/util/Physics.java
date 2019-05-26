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

/**
 * High accuracy Minecraft physics library.
 */
public final class Physics {

    //Calculus is fun

    //Legit, I don't know how else to describe these.
    //This is the amount of kinetic energy that is preserved per tick.
    //Without this, the player's velocity would steadily approach infinity,
    //and their position vs. time function would look like a quadratic.
    public static final float KINETIC_PRESERVATION_AIR_HORIZONTAL = 0.91F;
    public static final float KINETIC_PRESERVATION_AIR_VERTICAL = 0.98F;
    public static final float KINETIC_PRESERVATION_GROUND = 0.546F;
    public static final float KINETIC_PRESERVATION_ICE = 0.891801F;
    public static final float KINETIC_PRESERVATION_WATER = 0.8F;
    public static final float KINETIC_PRESERVATION_LAVA = 0.5F;

    public static final float GRAVITY_FORCE = 0.08F;

    //Player movement force (applies for horizontal and vertical movement; not individual axes).
    //Technically, gravity does not work in liquids or cobwebs.
    public static final float MOVE_LIQUID_FORCE = 0.02F;
    public static final float MOVE_WALK_FORCE = 0.1F;
    public static final float MOVE_HORIZONTAL_AIR_FORCE = 0.02F;
    public static final float MOVE_HORIZONTAL_FLY_FORCE = 0.05F;

    //Water flow force
    public static final float WATER_FLOW_FORCE_MULTIPLIER = 0.014F;

    public static final float JUMP_INITIAL_VELOCITY_VERTICAL = 0.42F;

    //Fun fact: The client should never reach these values. When
    //graphing the client's velocity on a velocity vs. time graph,
    //the velocity appears to exponentially decay towards one of
    //these specific values. In other words, the function's limit
    //as time approaches infinity is one of these constants.
    public static final float TERMINAL_VELOCITY_WALK = 50F/227F;
    public static final float TERMINAL_VELOCITY_FLY = 5F/9F;
    public static final float TERMINAL_VELOCITY_SWIM_WATER = 0.1F;
    public static final float TERMINAL_VELOCITY_FALL = 3.92F;

    public static final float SNEAK_MULTIPLIER = 0.41561F;
    public static final float FLY_SPRINT_MULTIPLIER = 2F;
    public static final float SPRINT_MULTIPLIER = 1.3F;

    private Physics() {
    }

    /**
     * Vertical position vs. time functions.
     */
    public static double airYPosFunc(double initVelocityY, long deltaTime) {
        deltaTime++;
        return -3.92 * deltaTime - 50 * Math.pow(0.98, deltaTime) * (3.92 + initVelocityY) + 50 * (3.92 + initVelocityY);
    }

    public static double waterYPosFunc(double initVelocityY, long deltaTime) {
        deltaTime++;
        return -0.1 * deltaTime - 5 * Math.pow(0.8, deltaTime) * (0.1 + initVelocityY) + 5 * (0.1 + initVelocityY);
    }

    /**
     * Vertical velocity vs. time functions.
     */
    public static double airYVelFunc(double initVelocityY, long deltaTime) {
        return (3.92 + initVelocityY) * Math.pow(0.98, deltaTime) - 3.92;
    }

    public static double waterYVelFunc(double initVelocityY, long deltaTime) {
        return (0.1 + initVelocityY) * Math.pow(0.8, deltaTime) - 0.1;
    }

    public static Pair<Double, Double> nextExpectedHorizontalSpeed() {
        return null;
    }
}
