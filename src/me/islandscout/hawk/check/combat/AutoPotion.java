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
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.InteractWorldEvent;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.ItemSwitchEvent;

import java.util.*;

public class AutoPotion extends CustomCheck implements Cancelless {

    private Map<UUID, Long> lastSwitchTicks;
    private Set<UUID> usedSomething;
    private int MIN_SWITCH_TICKS;

    public AutoPotion() {
        super("autopotion", true, -1, 5, 0.99, 5000, "%player% may be using auto-potion, VL: %vl%", null);
        lastSwitchTicks = new HashMap<>();
        usedSomething = new HashSet<>();
        MIN_SWITCH_TICKS = (int)customSetting("minSwitchTicks", "", 2);
    }

    @Override
    protected void check(Event event) {
        if(event instanceof InteractWorldEvent) {
            usedSomething.add(event.getPlayer().getUniqueId());
            return;
        }
        if(!(event instanceof ItemSwitchEvent)) {
            return;
        }

        HawkPlayer pp = event.getHawkPlayer();
        UUID uuid = pp.getUuid();

        if(usedSomething.contains(uuid)) {
            long lastSwitchTick = lastSwitchTicks.getOrDefault(uuid, 0L);
            if(pp.getCurrentTick() - lastSwitchTick < MIN_SWITCH_TICKS) {
                punish(pp, 1, false, event);
            }
            else {
                reward(pp);
            }

            usedSomething.remove(uuid);
        }

        lastSwitchTicks.put(pp.getUuid(), pp.getCurrentTick());
    }
}
