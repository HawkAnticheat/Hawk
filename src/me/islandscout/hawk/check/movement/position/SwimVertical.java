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
import me.islandscout.hawk.util.*;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.*;

public class SwimVertical extends MovementCheck {

    //TODO: false flag when exiting liquid while on slab while against wall

    private Set<UUID> wasReadyToExit;

    public SwimVertical() {
        super("swimvertical", "%player% failed swim vertical, VL: %vl%");
        wasReadyToExit = new HashSet<>();
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        double currentDeltaY = MathPlus.round(e.getTo().getY() - e.getFrom().getY(), 6);
        //TODO: optimize
        Set<Direction> boxSidesTouchingBlocks = AdjacentBlocks.checkTouchingBlock(new AABB(e.getFrom().toVector().add(new Vector(-0.299, 0.001, -0.299)), e.getFrom().toVector().add(new Vector(0.299, 1.799, 0.299))), e.getFrom().getWorld(), 0.1, pp.getClientVersion());

        boolean readyToExit = boxSidesTouchingBlocks.size() > 0 && !e.isInWater() && pp.isInWater();
        boolean exiting = readyToExit && currentDeltaY == 0.34;
        if(exiting || (wasReadyToExit.contains(pp.getUuid()) && currentDeltaY == 0.3)) {
            reward(pp);
        }

        else if(pp.isSwimming() && !e.isTeleportAccept() && !e.isOnGroundReally() && !pp.isFlying() && !e.hasAcceptedKnockback()) {
            //TODO: when you're getting pushed down by water, your terminal velocity is < 0.1 when going up, thus bypass when swimming up
            if(Math.abs(currentDeltaY) >= 0.1) {
                //i check when it is >= 0.1 because this game is broken
                //and i don't want work around each individual axis that does this
                //stupid compression-like behavior
                float flowForce = (float)pp.getWaterFlowForce().getY();
                double prevDeltaY = pp.getVelocity().getY();
                Set<Material> liquidTypes = new HashSet<>();
                float kineticPreservation = (liquidTypes.contains(Material.LAVA) || liquidTypes.contains(Material.STATIONARY_LAVA) ? Physics.KINETIC_PRESERVATION_LAVA : Physics.KINETIC_PRESERVATION_WATER);
                if(currentDeltaY < kineticPreservation * prevDeltaY + (-(Physics.MOVE_LIQUID_FORCE + 0.000001) + flowForce) ||
                        currentDeltaY > kineticPreservation * prevDeltaY + (Physics.MOVE_LIQUID_FORCE + 0.000001 + flowForce)) {
                    punishAndTryRubberband(pp, e);
                }
                else
                    reward(pp);
            }
        }

        if(readyToExit)
            wasReadyToExit.add(pp.getUuid());
        else
            wasReadyToExit.remove(pp.getUuid());
    }
}
