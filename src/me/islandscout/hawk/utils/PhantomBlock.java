package me.islandscout.hawk.utils;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

public class PhantomBlock {

    private final Location location;
    private final Material material;
    private final long initTime;

    public PhantomBlock(Location location, Material material) {
        this.location = location;
        this.material = material;
        initTime = System.currentTimeMillis();
    }

    public Location getLocation() {
        return location;
    }

    public Material getMaterial() {
        return material;
    }

    public long getInitTime() {
        return initTime;
    }

    public static PhantomBlock playerIsOnPhantomBlock(HawkPlayer pp, Location playerLoc) {
        AABB feet = new AABB(
                new Vector(-0.3, -0.01, -0.3).add(playerLoc.toVector()),
                new Vector(0.3, 0, 0.3).add(playerLoc.toVector()));
        for (PhantomBlock pBlock : pp.getPhantomBlocks()) {
            AABB cube = new AABB(pBlock.getLocation().toVector(), pBlock.getLocation().toVector().add(new Vector(1, 1, 1)));
            if (feet.isColliding(cube)) {
                return pBlock;
            }
        }
        return null;
    }
}
