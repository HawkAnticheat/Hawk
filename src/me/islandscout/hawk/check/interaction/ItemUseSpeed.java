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
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.InteractItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemUseSpeed extends CustomCheck {

    private Map<UUID, Long> lastEventTick;

    public ItemUseSpeed() {
        super("itemusespeed", true, 5, 5, 0.99, 5000, "%player% failed item-use speed, VL: %vl%", null);
        lastEventTick = new HashMap<>();
    }

    @Override
    protected void check(Event e) {
        if(!(e instanceof InteractItemEvent)) {
            return;
        }
        HawkPlayer pp = e.getHawkPlayer();
        UUID uuid = pp.getUuid();
        long currTick = pp.getCurrentTick();
        if(currTick == lastEventTick.getOrDefault(uuid, -1L)) {
            punish(pp, true, e);
        }
        else {
            reward(pp);
        }
        lastEventTick.put(uuid, pp.getCurrentTick());
    }
}
