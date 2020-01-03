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
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class InvalidPlacement extends BlockInteractionCheck {

    public InvalidPlacement() {
        super("invalidplacement", true, 0, 0, 0.999, 5000, "%player% failed invalid placement, VL: %vl%", null);
    }

    @Override
    protected void check(InteractWorldEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        Block targetedBlock = ServerUtils.getBlockAsync(e.getTargetedBlockLocation());
        if(targetedBlock == null)
            return;
        Material mat = targetedBlock.getType();
        if(targetedBlock.isLiquid() || mat == Material.AIR) {
            punishAndTryCancelAndBlockRespawn(pp, 1, e);
        }
    }
}
