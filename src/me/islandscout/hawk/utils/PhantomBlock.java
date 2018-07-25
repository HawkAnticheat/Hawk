package me.islandscout.hawk.utils;

import org.bukkit.Location;
import org.bukkit.Material;

public class PhantomBlock {

    private Location location;
    private Material material;
    private long initTime;

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
}
