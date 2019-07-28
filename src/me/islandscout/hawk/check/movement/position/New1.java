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

package me.islandscout.hawk.check.movement.position;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.util.Vector;

public class New1 extends MovementCheck {

    public New1() {
        super("new1", "%player% failed new1, VL: %vl%");
    }

    @Override
    protected void check(MoveEvent e) {
        //remember to check if you've just TP'd or took KB
        //remember to only check if your speed is great enough
        //remember, only works if you aren't sneaking or using an item
        //remember, only works when you're pressing down on a WASD key
        //remember to check if you're colliding horizontally
        HawkPlayer pp = e.getHawkPlayer();
        double moveFactor = pp.isSprinting() ? 0.13 : 0.1;
        double friction = e.isOnGround() ? 0.546 : 0.91;
        double dX = e.getTo().getX() - e.getFrom().getX();
        double dZ = e.getTo().getZ() - e.getFrom().getZ();
        dX /= friction;
        dZ /= friction;
        dX -= pp.getVelocity().getX();
        dZ -= pp.getVelocity().getZ();
        //Debug.broadcastMessage(MathPlus.round(dX, 6) * friction / moveFactor);

        //maybe the magnitude of this vector can be used to replace inertia!
        //maybe 0 means that you aren't pressing on a key
        Vector force = new Vector(dX, 0, dZ);
        Vector yaw = MathPlus.getDirection(e.getTo().getYaw(), 0);
        Debug.sendToPlayer(pp.getPlayer(), "force: " + MathPlus.round(force.length(), 6));

        boolean up = force.clone().crossProduct(yaw).dot(new Vector(0, 1, 0)) >= 0;
        double angle = (up ? 1 : -1) * MathPlus.round(force.angle(yaw), 2);
        Debug.sendToPlayer(pp.getPlayer(), "angle: " + angle);
    }
}
