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

package me.islandscout.hawk.check.movement;

import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.PositionEvent;

/**
 * This check is used to flag clients whose jumps are too
 * small. Although insignificant at first glance, small jumps
 * can be exploited to bypass other checks such as speed and
 * criticals.
 */
public class SmallHop extends MovementCheck {

    public SmallHop() {
        super("smallhop", "%player% failed small-hop, VL: %vl%");
    }

    @Override
    protected void check(PositionEvent e) {

    }
}
