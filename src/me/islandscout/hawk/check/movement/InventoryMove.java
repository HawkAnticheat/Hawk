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

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;

/*
 * InventoryActions check originally written by Havesta; modified, split apart, and implemented into Hawk by Islandscout
 *
 * InventoryMove checks if a player is
 * - rotating, sprinting, or sneaking while inventory is opened
 */

public class InventoryMove extends MovementCheck {

    public InventoryMove() {
        super("inventorymove", true, 3, 5, 0.999, 5000, "%player% failed inventory-move, VL: %vl%", null);
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();

        //TODO: false flag: rotation is still possible at least 1 tick after opening inventory
        //TODO: false flag: you gotta do that TP grace period thing for this too
        if((e.isUpdateRot() || pp.isSprinting() || pp.isSneaking()) && pp.hasInventoryOpen() != 0 && !e.hasTeleported()) {
            punishAndTryRubberband(pp, e);
        }
        else {
            reward(pp);
        }
    }
}