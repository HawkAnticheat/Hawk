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
 * This check limits clients' sprint and sneak toggle rates to
 * prevent exploitation of the speed check.
 */
public class ActionToggleSpeed extends MovementCheck {

    public ActionToggleSpeed(String name, String flag) {
        super(name, flag);
    }

    @Override
    protected void check(PositionEvent positionEvent) {

    }
}
