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
import me.islandscout.hawk.check.BlockDigCheck;
import me.islandscout.hawk.event.BlockDigEvent;
import me.islandscout.hawk.util.AABB;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class BlockBreakReach extends BlockDigCheck {

    private final float MAX_REACH;
    private final float MAX_REACH_CREATIVE;

    public BlockBreakReach() {
        super("blockbreakreach", "%player% failed block break reach, Reach: %distance%m, VL: %vl%");
        MAX_REACH = (float) customSetting("maxReach", "", 3.1);
        MAX_REACH_CREATIVE = (float) customSetting("maxReachCreative", "", 5.0);
    }

    @Override
    protected void check(BlockDigEvent e) {
        HawkPlayer pp = e.getHawkPlayer();

        Location bLoc = e.getBlock().getLocation();
        Vector min = bLoc.toVector();
        Vector max = bLoc.toVector().add(new Vector(1, 1, 1));
        AABB targetAABB = new AABB(min, max);

        Vector ppPos = pp.getPosition().clone().add(new Vector(0D, pp.isSneaking() ? 1.54F : 1.62F, 0D));

        double maxReach = pp.getPlayer().getGameMode() == GameMode.CREATIVE ? MAX_REACH_CREATIVE : MAX_REACH;
        double dist = targetAABB.distanceToPosition(ppPos);

        if (dist > maxReach) {
            punish(pp, 1, true, e, new Placeholder("distance", MathPlus.round(dist, 2)));
        } else {
            reward(pp);
        }
    }
}
