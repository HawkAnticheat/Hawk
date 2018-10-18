/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class Debug {

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
          - Get the class name and possibly the reference of the troublesome object. (getClass().getName() usually does this)
     */

    public static void broadcastMessage(String str) {
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[Hawk DEBUG]: " + ChatColor.RESET + str);
    }

    public static void sendToPlayer(Player player, String str) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[Hawk DEBUG]: " + ChatColor.RESET + str);
    }
}
