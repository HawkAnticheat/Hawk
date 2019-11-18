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

package me.islandscout.hawk.check.interaction.inventory;

/*
 * InventoryActions check originally written by Havesta; modified, split apart, and implemented into Hawk by Islandscout
 *
 * InventoryActions checks if a player is
 * - interacting with entities/blocks while inventory is opened
 */

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.*;

public class InventoryActions extends CustomCheck {

    public InventoryActions() {
        super("inventoryactions", true, 3, 5, 0.999, 5000, "%player% failed inventory-actions, VL: %vl%", null);
    }

    @Override
    protected void check(Event e) {
        HawkPlayer pp = e.getHawkPlayer();
        if(pp.hasInventoryOpen() != 0 && (e instanceof InteractEntityEvent || e instanceof BlockDigEvent ||
                e instanceof ArmSwingEvent || e instanceof InteractWorldEvent)) {
            punish(pp, true, e);
            e.resync();
            //TODO After failing several times, there's a chance that they could be legit, but the inventory state is glitched. Close the player's inventory.
        }
        else if(pp.hasInventoryOpen() == 0 && e instanceof ClickInventoryEvent) {
            punish(pp, true, e);
            e.getPlayer().updateInventory();
        }
        else {
            reward(pp);
        }
    }
}
