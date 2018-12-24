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

package me.islandscout.hawk.check.movement;

import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.PositionEvent;

//Not really an important check. This just stops skids from thinking they're so cool.
public class InvalidPitch extends MovementCheck {

    //PASSED (9/11/18)

    public InvalidPitch() {
        super("invalidpitch", "%player% failed invalid pitch. VL: %vl%");
    }

    @Override
    protected void check(PositionEvent event) {
        if (!event.hasDeltaRot())
            return;
        if (event.getTo().getPitch() < -90 || event.getTo().getPitch() > 90)
            punishAndTryRubberband(event.getHawkPlayer(), event, event.getPlayer().getLocation());
        else
            reward(event.getHawkPlayer());
    }
}
