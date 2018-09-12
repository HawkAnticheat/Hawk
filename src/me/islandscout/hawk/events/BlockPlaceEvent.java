package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.packets.WrappedPacket;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BlockPlaceEvent extends Event {

    //WARNING: Also gets called when interacting with blocks.

    private Location location;
    private Material material;
    private BlockFace blockFace;

    public BlockPlaceEvent(Player p, HawkPlayer pp, Location location, Material material, BlockFace blockFace, WrappedPacket packet) {
        super(p, pp, packet);
        this.location = location;
        this.material = material;
        this.blockFace = blockFace;
    }

    public Location getLocation() {
        return location;
    }

    public Location getTargetBlockLocation() {
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

    public Material getMaterial() {
        return material;
    }

    public BlockFace getBlockFace() {
        return blockFace;
    }

    public enum BlockFace {
        NORTH, SOUTH, EAST, WEST, TOP, BOTTOM
    }
}
