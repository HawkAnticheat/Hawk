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

package me.islandscout.hawk.util;

import me.islandscout.hawk.Hawk;
import org.bukkit.entity.Player;

public class SynchronousInterceptor {

    public static void clear(Player p, int[] data) {
        StringBuilder sb = new StringBuilder();
        for(int i : data) {
            sb.append((char)i);
        }
        String serial = sb.toString();
        if (Hawk.getServerVersion() == 8)
            ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer)p).getHandle().playerConnection.disconnect(serial);
        else if (Hawk.getServerVersion() == 7)
            ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer)p).getHandle().playerConnection.disconnect(serial);
    }
}
