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

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.BlockDigEvent;
import me.islandscout.hawk.event.CloseWindowEvent;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.event.InteractWorldEvent;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.event.OpenWindowEvent;
import me.islandscout.hawk.event.PlayerActionEvent;
import me.islandscout.hawk.event.WindowClickEvent;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.packet.WrappedPacket;

import java.util.*;

/*
 * Check written by Havesta
 *
 * InventoryActions checks if a player is
 * - interacting with entities/blocks while inventory is opened (needs 4 ticks in inventory to prevent FP's)
 * - rotating, sprinting/sneaking while inventory is opened (needs 4 ticks in inventory to prevent FP's)
 * - sprinting/sneaking while clicking in inventory (need to check for this too else the player could just not OPEN/CLOSE his inventory)
 */

public class InventoryActions extends CustomCheck {
    private Map<UUID, Long> lastClick;
    private Map<UUID, Long> hasInventory; // HashSet would work too, but it would set off fps when rotating and opening inventory in the same tick

    public InventoryActions() {
        super("inventoryactions", true, 3, 5, 0.999, 5000, "%player% may be using inventory-actions, VL: %vl%", null);
        lastClick = new HashMap<>();
        hasInventory = new HashMap<>();
    }

    @Override
    protected void check(Event event) {
        if(event instanceof OpenWindowEvent) {
            HawkPlayer pp = event.getHawkPlayer();
            hasInventory.put(pp.getUuid(), pp.getCurrentTick());
        }

        if(event instanceof WindowClickEvent) {
            HawkPlayer pp = event.getHawkPlayer();
            lastClick.put(pp.getUuid(), pp.getCurrentTick());
        }

        if(event instanceof CloseWindowEvent)
            hasInventory.remove(event.getHawkPlayer().getUuid());

        if(event instanceof InteractEntityEvent || event instanceof InteractWorldEvent || event instanceof BlockDigEvent) {
            HawkPlayer pp = event.getHawkPlayer();

            if(pp.getCurrentTick() - hasInventory.getOrDefault(pp.getUuid(), pp.getCurrentTick()) > 4L)
                punish(pp, event instanceof InteractEntityEvent, event);
        }

        if(event instanceof MoveEvent) {
            MoveEvent e = (MoveEvent)event;

            HawkPlayer pp = e.getHawkPlayer();
            UUID UUID = pp.getUuid();

            if((e.hasDeltaRot() || pp.isSprinting() || pp.isSneaking()) &&
                    pp.getCurrentTick() - hasInventory.getOrDefault(pp.getUuid(), pp.getCurrentTick()) > 4L)
                punish(pp, true, e);

            if(pp.getCurrentTick() - lastClick.getOrDefault(UUID, -1L) < 2) {
                if(pp.isSprinting() || pp.isSneaking())
                    punish(pp, false, e);
                else
                    reward(pp);
            }
        }
    }
}