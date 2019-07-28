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
import me.islandscout.hawk.util.Direction;
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.util.Vector;

public class Strafe extends MovementCheck {

    //TODO improve on those inconsistent moves
    //TODO fix jumping
    //TODO either ignore or support other frictions (water, cobwebs, lava, etc.)

    //This unintentionally trashes yet another handful of killauras and aimassists

    private static final double THRESHOLD = 0.1;

    public Strafe() {
        super("strafe", false, -1, 10, 0.99, 5000, "", null);
    }

    @Override
    protected void check(MoveEvent e) {
        if(e.hasTeleported() || e.hasAcceptedKnockback())
            return;

        if(collidingHorizontally(e))
            return;

        HawkPlayer pp = e.getHawkPlayer();

        if(pp.isBlocking() || pp.isConsumingItem() || pp.isPullingBow() || pp.isSneaking())
            return;

        Vector moveHoriz = e.getTo().toVector().subtract(e.getFrom().toVector()).setY(0);

        //crude workaround to the stupid inconsistencies in movement
        if(moveHoriz.length() < 0.15)
            return;

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

        //you aren't pressing a WASD key
        if(force.length() < 0.0001)
            return;

        boolean up = force.clone().crossProduct(yaw).dot(new Vector(0, 1, 0)) >= 0;
        double angle = (up ? 1 : -1) * MathPlus.round(force.angle(yaw), 2);

        if(!isValidStrafe(angle))
            punish(pp, false, e);
        else
            reward(pp);
    }

    private boolean collidingHorizontally(MoveEvent e) {
        for(Direction dir : e.getBoxSidesTouchingBlocks()) {
            if(dir == Direction.EAST || dir == Direction.NORTH || dir == Direction.SOUTH || dir == Direction.WEST)
                return true;
        }
        return false;
    }

    private boolean isValidStrafe(double angle) {
        double multiple = angle / (Math.PI / 4);
        return Math.abs(multiple - Math.floor(multiple)) <= THRESHOLD ||
                Math.abs(multiple - Math.ceil(multiple)) <= THRESHOLD;
    }
}
