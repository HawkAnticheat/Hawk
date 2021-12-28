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

package me.islandscout.hawk.util.packet;

import net.minecraft.server.v1_8_R3.PacketPlayOutPosition;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collections;

public final class TeleportUtil8 {

    public static void sendTeleportPacket(Player p, Location loc) {
        ((CraftPlayer)p).getHandle().playerConnection.sendPacket(
                new PacketPlayOutPosition(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), Collections.emptySet()));
    }
}
