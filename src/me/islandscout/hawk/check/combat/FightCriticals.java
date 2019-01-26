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

package me.islandscout.hawk.check.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.EntityInteractionCheck;
import me.islandscout.hawk.event.InteractAction;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.util.AdjacentBlocks;
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class FightCriticals extends EntityInteractionCheck {

    public FightCriticals() {
        super("fightcriticals", "%player% failed fight criticals. VL: %vl%");
    }

    @Override
    protected void check(InteractEntityEvent e) {
        if (e.getInteractAction() == InteractAction.ATTACK && !e.getPlayer().isFlying()) {
            HawkPlayer att = e.getHawkPlayer();
            Location loc = att.getLocation().clone();

            Block below = ServerUtils.getBlockAsync(loc.add(0, -0.3, 0));
            Block above = ServerUtils.getBlockAsync(loc.add(0, 2.3, 0));
            if (below == null || above == null)
                return;
            //TODO: false flag when jumping onto block. Check that jump height isn't 0.0?
            if (AdjacentBlocks.onGroundReally(att.getLocation(), -1, true, 0.02) && !att.isOnGround() ||
                    (att.getFallDistance() != 0 && att.getTotalAscensionSinceGround() < 0.3 && att.getFallDistance() < 0.3 && below.getType().isSolid() && !above.getType().isSolid())) {
                punish(att, true, e);
                return;
            }
            reward(att);
        }
    }
}
