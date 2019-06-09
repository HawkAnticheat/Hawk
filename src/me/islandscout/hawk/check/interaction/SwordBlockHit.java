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

import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.ArmSwingEvent;
import me.islandscout.hawk.event.Event;

public class SwordBlockHit extends CustomCheck {

    public SwordBlockHit() {
        super("swordblockhit", "%player% failed sword block-hit, VL: %vl%");
    }

    @Override
    protected void check(Event e) {
        //ignore creative
        //ignore if interacting with instant break block. might need to change how the dig listener works
        if(e instanceof ArmSwingEvent && e.getHawkPlayer().isBlocking() && !e.getHawkPlayer().isDigging()) {

        }
    }
}
