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

package me.islandscout.hawk.check.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.AdjacentBlocks;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * This check is used to flag clients whose jumps are too
 * small. Although insignificant at first glance, small jumps
 * can be exploited to bypass other checks such as speed and
 * criticals.
 */
public class SmallHop extends MovementCheck {

    //TODO: False flag in cobwebs

    public SmallHop() {
        super("smallhop", true, 0, 5, 0.99, 5000, "%player% failed small-hop, VL: %vl%", null);
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        double deltaY = e.hasTeleported() ? 0D : e.getTo().getY() - e.getFrom().getY();
        double prevDeltaY = pp.getVelocity().getY();
        boolean wasOnGround = pp.isOnGround();
        Location checkPos = e.getFrom().clone().add(0, 1, 0);

        if(!e.getPlayer().isFlying() && !e.hasAcceptedKnockback() && !e.isSlimeBlockBounce() && wasOnGround && deltaY > 0 && deltaY < 0.4 && prevDeltaY <= 0 &&
                !AdjacentBlocks.blockAdjacentIsSolid(checkPos) && !AdjacentBlocks.blockAdjacentIsSolid(checkPos.add(0, 1, 0)) && !AdjacentBlocks.blockAdjacentIsLiquid(checkPos.add(0, -1, 0)) &&
                !AdjacentBlocks.blockAdjacentIsLiquid(checkPos.add(0, -1, 0)) && !AdjacentBlocks.matIsAdjacent(e.getTo(), Material.LADDER, Material.VINE) &&
                !AdjacentBlocks.onGroundReally(e.getTo(), -1, false, 0.001)) {
            punishAndTryRubberband(pp, e, e.getPlayer().getLocation());
        }
        else {
            reward(pp);
        }
    }
}
