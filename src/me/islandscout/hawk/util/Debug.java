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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class Debug {

    private static final boolean SUPPRESS_DEBUGS = false;

    private Debug() {
    }

    /*
        Debugging tips:
        Does something seem impossible? Does it seem as if Java is broken and you want to
        smash your keyboard? Consider this:
          - Have you tried to search documentation?
          - Have you tried to search through the server source?
          - Have you tried checking if something extends a troublesome object; possibly overriding its method you're
            calling?
            - For instance: getting the AABB of blocks from Block. Plants return null because BlockPlant extends Block
              and overrides the AABB getter method with its own.
          - Get the class name (Object#getClass().getName()) and compare the reference of the troublesome object (using ==).
     */

    public static void broadcastMessage(Object msg) {
        if(!SUPPRESS_DEBUGS)
            Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[Hawk DEBUG]: " + ChatColor.RESET + msg);
    }

    public static void sendToPlayer(Player player, String str) {
        if(!SUPPRESS_DEBUGS)
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[Hawk DEBUG]: " + ChatColor.RESET + str);
    }

    public static void setTagColor(Player p, ChatColor color) {
        if(color == null) {
            p.setPlayerListName(null);
        } else {
            String tag = color + "" + p.getPlayerListName();
            if(!p.getPlayerListName().equals(tag)) {
                p.setPlayerListName(tag);
            }
        }
    }

}
