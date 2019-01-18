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
import me.islandscout.hawk.util.packet.WrappedPacket;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class InteractWorldAndItemEvent extends Event {

    //Represents a PacketPlayInBlockPlace packet. Pretty much interacting with blocks and held items.

    private final Location location;
    private final Material material;
    private final BlockFace blockFace;
    private final InteractionType interactionType;

    public InteractWorldAndItemEvent(Player p, HawkPlayer pp, Location location, Material material, BlockFace blockFace, InteractionType interactionType, WrappedPacket packet) {
        super(p, pp, packet);
        this.location = location;
        this.material = material;
        this.blockFace = blockFace;
        this.interactionType = interactionType;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public Location getPlacedBlockLocation() {
        if(interactionType == InteractionType.USE_ITEM)
            return null;
        return location;
    }

    public Material getPlacedBlockMaterial() {
        if(interactionType == InteractionType.USE_ITEM)
            return null;
        return material;
    }

    public BlockFace getTargetedBlockFace() {
        if(interactionType == InteractionType.USE_ITEM)
            return null;
        return blockFace;
    }

    public Location getTargetedBlockLocation() {
        if(interactionType == InteractionType.USE_ITEM)
            return null;
        switch (blockFace) {
            case TOP:
                return new Location(location.getWorld(), location.getX(), location.getY() - 1, location.getZ());
            case EAST:
                return new Location(location.getWorld(), location.getX() - 1, location.getY(), location.getZ());
            case WEST:
                return new Location(location.getWorld(), location.getX() + 1, location.getY(), location.getZ());
            case NORTH:
                return new Location(location.getWorld(), location.getX(), location.getY(), location.getZ() + 1);
            case SOUTH:
                return new Location(location.getWorld(), location.getX(), location.getY(), location.getZ() - 1);
            case BOTTOM:
                return new Location(location.getWorld(), location.getX(), location.getY() + 1, location.getZ());
        }
        return null;
    }

    public Vector getTargetedBlockFaceNormal() {
        if(interactionType == InteractionType.USE_ITEM)
            return null;
        switch (blockFace) {
            case TOP:
                return new Vector(0, 1, 0);
            case BOTTOM:
                return new Vector(0, -1, 0);
            case SOUTH:
                return new Vector(0, 0, 1);
            case NORTH:
                return new Vector(0, 0, -1);
            case WEST:
                return new Vector(-1, 0, 0);
            case EAST:
                return new Vector(1, 0, 0);
            default:
                return null;
        }
    }

    public enum BlockFace {
        NORTH, SOUTH, EAST, WEST, TOP, BOTTOM
    }

    public enum InteractionType {
        USE_ITEM, PLACE_BLOCK, INTERACT_BLOCK
    }
}
