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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WrongBlock extends BlockDigCheck {

    //PASSED (9/11/18)

    private final Map<UUID, Block> blockinteracted;

    public WrongBlock() {
        super("wrongblock", "%player% failed wrong block. VL: %vl%");
        blockinteracted = new HashMap<>();
    }

    public void check(BlockDigEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Block b = e.getBlock();
        if (e.getDigAction() == BlockDigEvent.DigAction.START) {
            blockinteracted.put(p.getUniqueId(), e.getBlock());
        } else if (e.getDigAction() == BlockDigEvent.DigAction.COMPLETE) {
            if ((!blockinteracted.containsKey(p.getUniqueId()) || !b.equals(blockinteracted.get(p.getUniqueId())))) {
                punishAndTryCancelAndBlockRespawn(pp, e);
            } else
                reward(pp);
        }

    }

    public void removeData(Player p) {
        blockinteracted.remove(p.getUniqueId());
    }
}
