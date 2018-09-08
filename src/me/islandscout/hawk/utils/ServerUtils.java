package me.islandscout.hawk.utils;

import me.islandscout.hawk.Hawk;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class ServerUtils {

    private ServerUtils() {}

    public static int getPing(Player p) {
        if (Hawk.getServerVersion() == 8)
            return ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) p).getHandle().ping;
        if (Hawk.getServerVersion() == 7)
            return ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer) p).getHandle().ping;
        return -1;
    }

    //ONLY WORKS ON SPIGOT #1649 OR DERIVATIVES
    public static int getClientVersion(Player p) {
        if(Hawk.getServerVersion() == 7) {
            int protocol = ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer) p).getHandle().playerConnection.networkManager.getVersion();
            if(protocol == 47)
                return 8;
            else if(protocol == 5)
                return 7;
            /*
            If above causes issues for people not running 1.7.10, try this:

            Integer ver = (Integer)channel.attr(protocolVersion).get();
            return ver != null ? ver.intValue() : 5;
             */
        }
        return 8;
    }

    public static double getTps() {
        if (Hawk.getServerVersion() == 8)
            return net.minecraft.server.v1_8_R3.MinecraftServer.getServer().recentTps[0];
        if (Hawk.getServerVersion() == 7)
            return net.minecraft.server.v1_7_R4.MinecraftServer.getServer().recentTps[0];
        return -1;
    }

    public static double getStress() {
        if (Hawk.getServerVersion() == 8)
            return net.minecraft.server.v1_8_R3.MathHelper.a(net.minecraft.server.v1_8_R3.MinecraftServer.getServer().h) * 1.0E-6D;
        if (Hawk.getServerVersion() == 7)
            return net.minecraft.server.v1_7_R4.MathHelper.a(net.minecraft.server.v1_7_R4.MinecraftServer.getServer().g) * 1.0E-6D;
        return -1;
    }

    //Will return null if chunk in location is not in memory. Do not modify blocks asynchronously!
    public static Block getBlockAsync(Location loc) {
        if(loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
            return loc.getBlock();
        return null;
    }

    //Will return null if chunk in location is not in memory. Do not modify chunks asynchronously!
    public static Chunk getChunkAsync(Location loc) {
        if(loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
            return loc.getChunk();
        return null;
    }
}
