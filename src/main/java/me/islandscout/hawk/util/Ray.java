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
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class Ray implements Cloneable {

    private Vector origin;
    private Vector direction;

    public Ray(Vector origin, Vector direction) {
        this.origin = origin;
        this.direction = direction;
    }

    public Vector getPointAtDistance(double distance) {
        Vector dir = new Vector(direction.getX(), direction.getY(), direction.getZ());
        Vector orig = new Vector(origin.getX(), origin.getY(), origin.getZ());
        return orig.add(dir.multiply(distance));
    }

    public Ray clone() {
        Ray clone;
        try {
            clone = (Ray) super.clone();
            clone.origin = this.origin.clone();
            clone.direction = this.direction.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void highlight(Hawk hawk, World world, double blocksAway, double accuracy) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, () -> {
            for (double x = 0; x < blocksAway; x += accuracy) {
                world.playEffect(getPointAtDistance(x).toLocation(world), Effect.COLOURED_DUST, 1);
            }
        }, 0L);

    }

    //https://en.wikipedia.org/wiki/Skew_lines#Nearest_Points
    public Pair<Vector, Vector> closestPointsBetweenLines(Ray other) {
        Vector n1 = direction.clone().crossProduct(other.direction.clone().crossProduct(direction));
        Vector n2 = other.direction.clone().crossProduct(direction.clone().crossProduct(other.direction));

        Vector c1 = origin.clone().add(direction.clone().multiply(other.origin.clone().subtract(origin).dot(n2) / direction.dot(n2)));
        Vector c2 = other.origin.clone().add(other.direction.clone().multiply(origin.clone().subtract(other.origin).dot(n1) / other.direction.dot(n1)));

        return new Pair<>(c1, c2);
    }

    public String toString() {
        return "origin: " + origin + " direction: " + direction;
    }

    public Vector getOrigin() {
        return origin;
    }

    public Vector getDirection() {
        return direction;
    }
}