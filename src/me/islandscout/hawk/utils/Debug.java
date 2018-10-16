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
