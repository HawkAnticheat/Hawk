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

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.util.ClientBlock;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.wrap.block.WrappedBlock7;
import me.islandscout.hawk.wrap.block.WrappedBlock8;
import me.islandscout.hawk.wrap.packet.WrappedPacket;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class InteractWorldEvent extends Event {

    //Represents a PacketPlayInBlockPlace packet. Pretty much interacting with blocks and held items.

    private final Location location;
    private final Material material;
    private final BlockFace blockFace;
    private final InteractionType interactionType;
    private final Vector cursorPosition;

    public InteractWorldEvent(Player p, HawkPlayer pp, Location location, Material material, BlockFace blockFace, Vector cursorPosition, InteractionType interactionType, WrappedPacket packet) {
        super(p, pp, packet);
        this.location = location;
        this.material = material;
        this.blockFace = blockFace;
        this.interactionType = interactionType;
        this.cursorPosition = cursorPosition;
    }

    @Override
    public boolean preProcess() {
        if(pp.isTeleporting()) {
            resync();
            return false;
        }
        return true;
    }

    @Override
    public void postProcess() {
        if (!isCancelled() && getInteractionType() == InteractWorldEvent.InteractionType.PLACE_BLOCK) {
            ClientBlock clientBlock = new ClientBlock(pp.getCurrentTick(), getPlacedBlockMaterial());
            pp.addClientBlock(getPlacedBlockLocation(), clientBlock);
        }
    }

    @Override
    public void resync() {
        Block b = ServerUtils.getBlockAsync(getPlacedBlockLocation());
        Block targeted = ServerUtils.getBlockAsync(getTargetedBlockLocation());
        if(b == null || targeted == null)
            return;
        if (Hawk.getServerVersion() == 7) {
            WrappedBlock7.getWrappedBlock(b, pp.getClientVersion()).sendPacketToPlayer(pp.getPlayer());
            WrappedBlock7.getWrappedBlock(targeted, pp.getClientVersion()).sendPacketToPlayer(pp.getPlayer());
        } else if (Hawk.getServerVersion() == 8) {
            WrappedBlock8.getWrappedBlock(b, pp.getClientVersion()).sendPacketToPlayer(pp.getPlayer());
            WrappedBlock8.getWrappedBlock(targeted, pp.getClientVersion()).sendPacketToPlayer(pp.getPlayer());
        }
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public Location getPlacedBlockLocation() {
        return location;
    }

    public Material getPlacedBlockMaterial() {
        return material;
    }

    public BlockFace getTargetedBlockFace() {
        return blockFace;
    }

    public Location getTargetedBlockLocation() {
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
            case INVALID:
                return location;
        }
        return null;
    }

    public Vector getTargetedBlockFaceNormal() {
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
            case INVALID:
                return new Vector(0, 1, 0);
        }
        return null;
    }

    public Vector getCursorPositionOnTargetedBlock() {
        return cursorPosition;
    }

    public enum BlockFace {
        NORTH, SOUTH, EAST, WEST, TOP, BOTTOM, INVALID
    }

    public enum InteractionType {
        PLACE_BLOCK, INTERACT_BLOCK
    }
}
