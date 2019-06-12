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
import me.islandscout.hawk.event.BlockDigEvent;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.event.InteractWorldEvent;

public class MultiAction extends CustomCheck {

    //I think 1.7 clients can punch blocks while eating/blocking. I might need to look into this.

    public MultiAction() {
        super("multiaction", false, 0, 10, 0.95, 5000, "%player% failed multi-action, VL: %vl%", null);
    }

    @Override
    protected void check(Event event) {
        if(!(event instanceof InteractEntityEvent ||
                event instanceof InteractWorldEvent ||
                event instanceof BlockDigEvent))
            return;
        HawkPlayer pp = event.getHawkPlayer();
        if(pp.isBlocking() || pp.isConsumingItem() || pp.isPullingBow()) {
            punish(pp, 1, true, event);
        }
        else {
            reward(pp);
        }
    }
}
