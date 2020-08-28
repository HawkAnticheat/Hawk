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
import me.islandscout.hawk.util.AABB;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.Placeholder;
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;

public class BlockInteractReach extends BlockInteractionCheck {

    private final float MAX_REACH;
    private final float MAX_REACH_CREATIVE;

    public BlockInteractReach() {
        super("blockbreakreach", "%player% failed block interact reach, Reach: %distance%m, VL: %vl%");
        MAX_REACH = (float) customSetting("maxReach", "", 3.1);
        MAX_REACH_CREATIVE = (float) customSetting("maxReachCreative", "", 5.0);
    }

    @Override
    protected void check(InteractWorldEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();

        Location bLoc = e.getTargetedBlockLocation();
        Vector min = bLoc.toVector();
        Vector max = bLoc.toVector().add(new Vector(1, 1, 1));
        AABB targetAABB = new AABB(min, max);

        Vector ppPos;
        if(pp.isInVehicle()) {
            ppPos = hawk.getLagCompensator().getHistoryLocation(ServerUtils.getPing(p), p).toVector();
            ppPos.setY(ppPos.getY() + p.getEyeHeight());
        }
        else {
            ppPos = pp.getHeadPosition();
        }

        double maxReach = pp.getPlayer().getGameMode() == GameMode.CREATIVE ? MAX_REACH_CREATIVE : MAX_REACH;
        double dist = targetAABB.distanceToPosition(ppPos);

        if (dist > maxReach) {
            punish(pp, 1, true, e, new Placeholder("distance", MathPlus.round(dist, 2)));
            e.resync();
        } else {
            reward(pp);
        }
    }
}
