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
import me.islandscout.hawk.util.AdjacentBlocks;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.Direction;
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.util.Vector;

import java.util.*;

public class SprintDirection extends MovementCheck {

    private Map<UUID, Long> lastSprintTickMap;
    private Set<UUID> collisionHorizontalSet;

    public SprintDirection() {
        super("sprintdirection", true, 0, 5, 0.999, 5000, "%player% failed sprint direction, VL %vl%", null);
        lastSprintTickMap = new HashMap<>();
        collisionHorizontalSet = new HashSet<>();
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();

        //ugly
        //check for collision. make sure player is sprinting for more than 1 tick too.
        //TODO god, I hate this
        boolean collisionHorizontal = AdjacentBlocks.blockAdjacentIsSolid(e.getTo()) || AdjacentBlocks.blockAdjacentIsSolid(e.getTo().clone().add(0, 1, 0)) ||
                AdjacentBlocks.blockAdjacentIsSolid(e.getTo().clone().add(0, 1.8, 0));
        if(!pp.isSprinting())
            lastSprintTickMap.put(pp.getUuid(), pp.getCurrentTick());

        if(pp.isSwimming() || e.hasTeleported() || e.hasAcceptedKnockback() ||
                (collisionHorizontal && !collisionHorizontalSet.contains(pp.getUuid())) ||
                pp.getCurrentTick() - lastSprintTickMap.getOrDefault(pp.getUuid(), pp.getCurrentTick()) < 2) {
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
        }

        //Div by 1.7948708571637845???? What the hell are these numbers?

        dX -= prevVelocity.getX();
        dZ -= prevVelocity.getZ();

        Vector moveForce = new Vector(dX, 0, dZ);
        Vector yawVec = MathPlus.getDirection(yaw, 0);

        if(MathPlus.angle(yawVec, moveForce) > Math.PI / 4 + 0.1) { //0.1 is arbitrary. Prevents falses due to precision limitation
            punishAndTryRubberband(pp, e, e.getPlayer().getLocation());
        }
        else {
            reward(pp);
        }

        if(collisionHorizontal)
            collisionHorizontalSet.add(pp.getUuid());
        else
            collisionHorizontalSet.remove(pp.getUuid());
    }
}
