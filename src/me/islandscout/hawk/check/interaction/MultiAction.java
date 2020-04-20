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
import me.islandscout.hawk.event.*;

public class MultiAction extends CustomCheck {

    public MultiAction() {
        super("multiaction", false, 0, 10, 0.95, 5000, "%player% failed multi-action, VL: %vl%", null);
    }

    @Override
    protected void check(Event event) {
        if (!(event instanceof InteractEntityEvent ||
                event instanceof InteractWorldEvent ||
                event instanceof BlockDigEvent ||
                event instanceof InteractItemEvent))
            return;

        HawkPlayer pp = event.getHawkPlayer();

        //interacting while using item
        if (!(event instanceof InteractItemEvent) && pp.getClientVersion() == 8 &&
                (pp.isBlocking() || pp.isConsumingOrPullingBowMetadataIncluded())) {
            punish(pp, 1, true, event);
            event.resync();
        }
        //interacting while digging
        else if (pp.getClientVersion() == 8 && pp.isDigging() &&
                !(event instanceof BlockDigEvent && ((BlockDigEvent) event).getDigAction() != BlockDigEvent.DigAction.START)) {
            punish(pp, 1, true, event);
            event.resync();
        } else {
            reward(pp);
        }
    }
}
