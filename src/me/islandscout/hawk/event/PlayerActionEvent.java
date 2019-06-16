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

package me.islandscout.hawk.event;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.util.packet.WrappedPacket;
import org.bukkit.entity.Player;

public class PlayerActionEvent extends Event {

    private PlayerAction action;

    public PlayerActionEvent(Player p, HawkPlayer pp, WrappedPacket wPacket, PlayerAction action) {
        super(p, pp, wPacket);
        this.action = action;
    }

    @Override
    public void postProcess() {
        if (!isCancelled()) {
            switch (action) {
                case SNEAK_START:
                    pp.setSneaking(true);
                    break;
                case SNEAK_STOP:
                    pp.setSneaking(false);
                    break;
                case SPRINT_START:
                    pp.setSprinting(true);
                    break;
                case SPRINT_STOP:
                    pp.setSprinting(false);
                    break;
            }
        }
    }

    public PlayerAction getAction() {
        return action;
    }

    public enum PlayerAction {
        SNEAK_START, //1
        SNEAK_STOP, //2
        BED_LEAVE, //3
        SPRINT_START, //4
        SPRINT_STOP, //5
        HORSE_JUMP, //6
        INVENTORY_OPEN,
        UNKNOWN
    }
}
