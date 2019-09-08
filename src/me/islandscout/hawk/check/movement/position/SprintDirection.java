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
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SprintDirection extends MovementCheck {

    public SprintDirection() {
        super("sprintdirection", true, 0, 5, 0.999, 5000, "%player% failed sprint direction, VL %vl%", null);
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        //TODO return if touching wall. maybe check for instantaneous collision and if they're sprinting for at least 1 tick?
        if(!e.isUpdatePos() || pp.isSwimming() || e.hasTeleported() || e.hasAcceptedKnockback()) {
            return;
        }

        float yaw = e.getTo().getYaw();
        Vector prevVelocity = pp.getVelocity().clone();
        if(e.hasHitSlowdown()) {
            prevVelocity.multiply(0.6);
        }
        double dX = e.getTo().getX() - e.getFrom().getX();
        double dZ = e.getTo().getZ() - e.getFrom().getZ();
        float friction = e.getFriction();
        dX /= friction;
        dZ /= friction;
        if(e.isJump()) {
            float yawRadians = yaw * 0.017453292F;
            dX += (MathPlus.sin(yawRadians) * 0.2F);
            dZ -= (MathPlus.cos(yawRadians) * 0.2F);
            Debug.broadcastMessage("jump");
        }

        //multiplier 1.7948708571637845   ????

        dX -= prevVelocity.getX();
        dZ -= prevVelocity.getZ();



        Vector moveForce = new Vector(dX, 0, dZ);
        Vector yawVec = MathPlus.getDirection(yaw, 0);

        Debug.broadcastMessage("---");
        Debug.broadcastMessage("force Z: " + moveForce.getZ());
        Debug.broadcastMessage("move Z: " + (e.getTo().getZ() - e.getFrom().getZ()));

        if(MathPlus.angle(yawVec, moveForce) > Math.PI / 4 + 0.1) { //0.1 is arbitrary. Prevents falses due to precision limitation
            punishAndTryRubberband(pp, e, e.getPlayer().getLocation());
        }
        else {
            reward(pp);
        }
    }
}
