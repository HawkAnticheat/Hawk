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

package me.islandscout.hawk.check.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.BlockInteractionCheck;
import me.islandscout.hawk.event.MaterialInteractionEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockInteractSpeed extends BlockInteractionCheck {

    private final Map<UUID, Long> lastPlaceTick;

    public BlockInteractSpeed() {
        super("blockplacespeed", "%player% failed block place speed. VL: %vl%");
        lastPlaceTick = new HashMap<>();
    }

    @Override
    protected void check(MaterialInteractionEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        if (pp.getCurrentTick() == lastPlaceTick.getOrDefault(p.getUniqueId(), 0L))
            punishAndTryCancelAndBlockRespawn(pp, e);
        else
            reward(pp);

        lastPlaceTick.put(p.getUniqueId(), pp.getCurrentTick());
    }

    @Override
    public void removeData(Player p) {
        lastPlaceTick.remove(p.getUniqueId());
    }
}
