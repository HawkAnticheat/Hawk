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
import me.islandscout.hawk.check.BlockDigCheck;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.event.BlockDigEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detects basic autoclickers that click and then release instantaneously.
 * Only works when aiming at solid blocks.
 */
public class ClickDuration extends BlockDigCheck implements Cancelless {

    //Inspired by this resource
    //https://www.spigotmc.org/resources/autokiller-advanced-auto-clicker-detection.40520/

    private Map<UUID, Long> digStartTick;

    public ClickDuration() {
        super("clickduration", false, -1, 0, 0.99, 5000, "%player% failed click duration. Autoclicker? VL: %vl%", null);
        digStartTick = new HashMap<>();
    }

    @Override
    protected void check(BlockDigEvent e) {
        BlockDigEvent.DigAction action = e.getDigAction();
        HawkPlayer pp = e.getHawkPlayer();
        if (action == BlockDigEvent.DigAction.START)
            digStartTick.put(pp.getUuid(), pp.getCurrentTick());
        else if (action == BlockDigEvent.DigAction.CANCEL) {
            if (pp.getCurrentTick() == digStartTick.getOrDefault(pp.getUuid(), -10L)) {
                punish(pp, false, e);
            } else {
                reward(pp);
            }
        }
    }

    @Override
    public void removeData(Player p) {
        digStartTick.remove(p.getUniqueId());
    }
}
