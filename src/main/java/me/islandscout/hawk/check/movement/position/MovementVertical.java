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
import me.islandscout.hawk.util.MathPlus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementVertical extends MovementCheck {

    private final Map<UUID, Double> discrepancies;

    public MovementVertical() {
        super("movementvertical", "%player% failed vertical movement, VL: %vl%");
        discrepancies = new HashMap<>();
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        double lastDeltaY = pp.getVelocity().getY();
        double deltaY = e.getTo().getY() - e.getFrom().getY();
        double discrepancy = 0;

        //TODO: check for collision on top of collision box; ignore move if true & if previous deltaY is > 0
        if(e.hasTeleported() || e.hasAcceptedKnockback() || pp.isFlyingClientside())
            return;

        //TODO: remember to stop people from "reverse stepping"
        //TODO: remember to stop people from "stepping"
        //TODO: remember to not cause a bypass to allow people to fly onto fences

        //COMPARE MOVEMENT HERE
        if(!pp.isOnGround() && !(e.isOnGround() && deltaY < 0))
            discrepancy = airMapping(lastDeltaY, deltaY);
        else if(true) {

        }

        discrepancy = MathPlus.round(discrepancy, 6);
        double discrepancyCumulative = discrepancy + discrepancies.getOrDefault(pp.getUuid(), 0D);

        //COMPARE CUMULATIVE DISCREPANCY HERE
        if(discrepancyCumulative > 0.1) {
            punishAndTryRubberband(pp, discrepancyCumulative * 10, e, e.getPlayer().getLocation());
        }
        else {
            reward(pp);
        }


        if(discrepancyCumulative > 0) {
            discrepancyCumulative = Math.max(0, discrepancyCumulative - 0.01);
        }
        else {
            discrepancyCumulative = Math.min(0, discrepancyCumulative + 0.01);
        }
        discrepancies.put(pp.getUuid(), discrepancyCumulative);
    }

    private double airMapping(double lastDeltaY, double currentDeltaY) {
        double expected = 0.98 * lastDeltaY - 0.0784;
        return currentDeltaY - expected;
    }
}
