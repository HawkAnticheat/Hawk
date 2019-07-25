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
import me.islandscout.hawk.util.block.BlockNMS;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class BlockInteractDirection extends BlockInteractionCheck {

    private final boolean DEBUG_HITBOX;
    private final boolean DEBUG_RAY;

    public BlockInteractDirection() {
        super("blockinteractdirection", true, 10, 10, 0.9, 5000, "%player% failed block interact direction, VL: %vl%", null);
        DEBUG_HITBOX = (boolean) customSetting("hitbox", "debug", false);
        DEBUG_RAY = (boolean) customSetting("ray", "debug", false);
    }

    @Override
    protected void check(InteractWorldEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Location bLoc = e.getTargetedBlockLocation();
        Vector pos = pp.getPosition().clone().add(new Vector(0, pp.isSneaking() ? 1.54 : 1.62, 0));
        Vector dir = MathPlus.getDirection(pp.getYaw(), pp.getPitch());
        //Note: in MC 1.8, the cursor yaw is not updated per frame, but rather per tick.
        //For ray-hitbox checks, this means that we do not need to extrapolate the yaw,
        //but for this check it does not matter whether we do it or not.
        Vector extraDir = MathPlus.getDirection(pp.getYaw() + pp.getDeltaYaw() * 2, pp.getPitch() + pp.getDeltaPitch() * 2); //2 is an arbitrary multiplier. Make it configurable?

        Vector min = bLoc.toVector();
        Vector max = bLoc.toVector().add(new Vector(1, 1, 1));
        AABB targetAABB = new AABB(min, max);

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
