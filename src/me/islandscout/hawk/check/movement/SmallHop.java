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

import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.AdjacentBlocks;
import me.islandscout.hawk.util.Debug;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.*;

/**
 * This check is used to flag clients whose jumps are too
 * small. Although insignificant at first glance, small jumps
 * can be exploited to bypass other checks such as speed and
 * criticals.
 */
public class SmallHop extends MovementCheck {

    private Set<UUID> wasOnGroundSet;
    private Map<UUID, Double> prevDeltaY;

    public SmallHop() {
        super("smallhop", "%player% failed small-hop, VL: %vl%");
        wasOnGroundSet = new HashSet<>();
        prevDeltaY = new HashMap<>();
    }

    @Override
    protected void check(PositionEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        double deltaY = e.getDeltaPos().getY();
        boolean wasOnGround = wasOnGroundSet.contains(uuid);
        Location checkPos = e.getTo().clone().add(0, 1, 0);

        if(wasOnGround && deltaY > 0 && deltaY < 0.4 && prevDeltaY.getOrDefault(uuid, 0D) <= 0 &&
                !AdjacentBlocks.blockAdjacentIsSolid(checkPos) && !AdjacentBlocks.blockAdjacentIsSolid(checkPos.add(0, 1, 0))
                /*TODO: check that they aren't walking on something (such as a repeater), accurately!!!*/) {
            Debug.broadcastMessage(ChatColor.RED + "" + deltaY);
        }

        if(e.isOnGroundReally()) {
            wasOnGroundSet.add(uuid);
        }
        else {
            wasOnGroundSet.remove(uuid);
        }

        prevDeltaY.put(uuid, deltaY);
    }
}
