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
import me.islandscout.hawk.wrap.packet.WrappedPacket;
import org.bukkit.entity.Player;

public class ArmSwingEvent extends Event {

    private final int type;

    public ArmSwingEvent(Player p, HawkPlayer pp, int type, WrappedPacket packet) {
        super(p, pp, packet);
        this.type = type;
    }

    public int getType() {
        return type;
    }

    //TODO post process? Check if all swing packets are forwarded to nearby players. If this is true, we need to implement a rate limiter to mitigate network congestion.
}
