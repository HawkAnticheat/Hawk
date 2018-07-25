package me.islandscout.hawk.utils;

import org.bukkit.Location;

public class LocationTime {

    private long time;
    private Location location;

    public LocationTime(long time, Location location) {
        this.time = time;
        this.location = location;
    }

    public long getTime() {
        return time;
    }

    public Location getLocation() {
        return location;
    }
}
