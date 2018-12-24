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

package me.islandscout.hawk.listener;

import me.islandscout.hawk.module.PacketCore;
import me.islandscout.hawk.util.packet.PacketAdapter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class PacketListener {

    final PacketCore packetCore;
    List<PacketAdapter> adaptersInbound;
    List<PacketAdapter> adaptersOutbound;

    PacketListener(PacketCore packetCore) {
        this.packetCore = packetCore;
        this.adaptersInbound = new ArrayList<>();
        this.adaptersOutbound = new ArrayList<>();
    }

    public abstract void start(Player p);

    public abstract void stop();

    public void addAdapterInbound(PacketAdapter runnable) {
        adaptersInbound.add(runnable);
    }

    public void removeAdapterInbound(PacketAdapter runnable) {
        adaptersInbound.remove(runnable);
    }

    public void addAdapterOutbound(PacketAdapter runnable) {
        adaptersOutbound.add(runnable);
    }

    public void removeAdapterOutbound(PacketAdapter runnable) {
        adaptersOutbound.remove(runnable);
    }

}
