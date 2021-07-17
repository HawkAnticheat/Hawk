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

//import io.github.retrooper.packetevents.PacketEvents;
import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class ServerUtils {



    private ServerUtils() {
    }

    public static int getPing(Player p) {
        //return SuperPingLib.getPing(p);
        if (Hawk.getServerVersion() == 8)
            return ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) p).getHandle().ping;
        if (Hawk.getServerVersion() == 7)
            return ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer) p).getHandle().ping;
        return -1;
    }

    public static int getProtocolVersion(Player p) {
        if (Hawk.USING_PACKETEVENTS) {
            //return PacketEvents.getAPI().getPlayerUtils().getClientVersion(p).getProtocolVersion();
        }
        if(Hawk.getServerVersion() == 7) {
            net.minecraft.server.v1_7_R4.PlayerConnection pConnection = ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer) p).getHandle().playerConnection;
            if(pConnection == null)
                return 5;
            return pConnection.networkManager.getVersion();
        }
        return 47;
    }

    public static double getTps() {
        if (Hawk.getServerVersion() == 8)
            return net.minecraft.server.v1_8_R3.MinecraftServer.getServer().recentTps[0];
        if (Hawk.getServerVersion() == 7)
            return net.minecraft.server.v1_7_R4.MinecraftServer.getServer().recentTps[0];
        return -1;
    }

    public static int getCurrentTick() {
        if (Hawk.getServerVersion() == 8)
            return net.minecraft.server.v1_8_R3.MinecraftServer.currentTick;
        if (Hawk.getServerVersion() == 7)
            return net.minecraft.server.v1_7_R4.MinecraftServer.currentTick;
        return -1;
    }

    public static double getStress() {
        if (Hawk.getServerVersion() == 8)
            return net.minecraft.server.v1_8_R3.MathHelper.a(net.minecraft.server.v1_8_R3.MinecraftServer.getServer().h) * 2.0E-8D;
        if (Hawk.getServerVersion() == 7)
            return net.minecraft.server.v1_7_R4.MathHelper.a(net.minecraft.server.v1_7_R4.MinecraftServer.getServer().g) * 2.0E-8D;
        return -1;
    }

    //Will return null if chunk in location is not in memory. Do not modify blocks asynchronously!
    public static Block getBlockAsync(Location loc) {
        if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
            return loc.getBlock();
        return null;
    }

    //Will return null if chunk in location is not in memory. Do not modify chunks asynchronously!
    public static Chunk getChunkAsync(Location loc) {
        if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
            return loc.getChunk();
        return null;
    }

    public static AABB[] getBlockCollisionBoxesAsyncClientSide(Location loc, HawkPlayer pp) {
        if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            ClientBlock cb = pp.getClientBlocks().get(loc);
            if (cb == null) {
                return WrappedBlock.getWrappedBlock(loc.getBlock(), pp.getClientVersion()).getCollisionBoxes();
            }

            AABB[] result = {null};
            if (cb.getMaterial().isSolid()) {
                //It would be nice if I can manage to get the correct bounding boxes for a hypothetical block.
                //Bounding boxes depend on the data of the block. Unfortunately, data isn't stored in NMS
                //Block; it's stored in NMS World. So, I'd need to "set" the block on the main thread, but
                //that literally defeats the purpose of this method. My checks NEED this utility on the
                //network thread WHILE the player is placing blocks.
                result[0] = new AABB(loc.toVector(), loc.toVector().clone().add(new Vector(1, 1, 1)));
                return result;
            }
            result[0] = new AABB(new Vector(0, 0, 0), new Vector(0, 0, 0));
            return result;
        }
        return null;
    }
}
