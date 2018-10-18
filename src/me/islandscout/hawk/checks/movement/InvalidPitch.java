/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.checks.MovementCheck;
import me.islandscout.hawk.events.PositionEvent;

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
