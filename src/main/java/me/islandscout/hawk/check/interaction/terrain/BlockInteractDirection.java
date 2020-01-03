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

package me.islandscout.hawk.check.interaction.terrain;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.BlockInteractionCheck;
import me.islandscout.hawk.event.InteractWorldEvent;
import me.islandscout.hawk.util.*;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BlockInteractDirection extends BlockInteractionCheck {

    private final double BOX_EXPAND;
    private final boolean DEBUG_HITBOX;
    private final boolean DEBUG_RAY;

    public BlockInteractDirection() {
        super("blockinteractdirection", true, 10, 10, 0.9, 5000, "%player% failed block interact direction, VL: %vl%", null);
        BOX_EXPAND = (double) customSetting("boxExpand", "", 0.2);
        DEBUG_HITBOX = (boolean) customSetting("hitbox", "debug", false);
        DEBUG_RAY = (boolean) customSetting("ray", "debug", false);
    }

    @Override
    protected void check(InteractWorldEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Location bLoc = e.getTargetedBlockLocation();
        Vector pos;
        if(pp.isInVehicle()) {
            pos = hawk.getLagCompensator().getHistoryLocation(ServerUtils.getPing(p), p).toVector();
            pos.setY(pos.getY() + p.getEyeHeight());
        }
        else {
            pos = pp.getHeadPosition();
        }
        Vector dir = MathPlus.getDirection(pp.getYaw(), pp.getPitch());
        Vector extraDir = MathPlus.getDirection(pp.getYaw() + pp.getDeltaYaw(), pp.getPitch() + pp.getDeltaPitch());

        Vector min = bLoc.toVector();
        Vector max = bLoc.toVector().add(new Vector(1, 1, 1));
        AABB targetAABB = new AABB(min, max);
        targetAABB.expand(BOX_EXPAND, BOX_EXPAND, BOX_EXPAND);

        if (DEBUG_HITBOX)
            targetAABB.highlight(hawk, p.getWorld(), 0.25);
        if (DEBUG_RAY)
            new Ray(pos, extraDir).highlight(hawk, p.getWorld(), 6F, 0.3);

        if(targetAABB.betweenRays(pos, dir, extraDir)) {
            reward(pp);
        }
        else {
            punish(pp, true, e);
        }
    }
}
