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

public class CustomPayLoadEvent extends Event {

    private final String tag;
    private final int length;
    private final byte[] data;

    public CustomPayLoadEvent(String tag, int length, byte[] data, Player p, HawkPlayer pp, WrappedPacket packet) {
        super(p, pp, packet);
        this.tag = tag;
        this.length = length;
        this.data = data;
    }

    public String getTag() {
        return tag;
    }

    public int getLength() {
        return length;
    }

    public byte[] getData() {
        return data;
    }
}
