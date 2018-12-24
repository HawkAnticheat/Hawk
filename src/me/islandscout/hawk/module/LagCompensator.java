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

package me.islandscout.hawk.module;

import me.islandscout.hawk.util.Pair;
import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.util.ConfigHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class LagCompensator implements Listener {

    //https://developer.valvesoftware.com/wiki/Lag_compensation
    //https://www.youtube.com/watch?v=6EwaW2iz4iA
    //http://www.gabrielgambetta.com/lag-compensation.html
    //http://www.gabrielgambetta.com/client-side-prediction-live-demo.html
    //https://en.wikipedia.org/wiki/Lag#Rewind_time

    private final Map<UUID, List<Pair<Location, Long>>> locationTimes;
    private final int historySize;
    private final int pingOffset;
    private static final int TIME_RESOLUTION = 40; //in milliseconds

    public LagCompensator(Hawk hawk) {
        this.locationTimes = new HashMap<>();
        historySize = ConfigHelper.getOrSetDefault(20, hawk.getConfig(), "lagCompensation.historySize");
        pingOffset = ConfigHelper.getOrSetDefault(175, hawk.getConfig(), "lagCompensation.pingOffset");
        Bukkit.getPluginManager().registerEvents(this, hawk);
    }

    //uses linear interpolation to get the best location
    public Location getHistoryLocation(int rewindMillisecs, Player player) {
        List<Pair<Location, Long>> times = locationTimes.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        if (times == null) {
            return player.getLocation();
        }
        int rewindTime = rewindMillisecs + pingOffset; //player a + avg processing time.
        for (int i = times.size() - 1; i >= 0; i--) { //loop backwards
            int elapsedTime = (int) (currentTime - times.get(i).getValue());
            if (elapsedTime >= rewindTime) {
                if (i == times.size() - 1) {
                    return times.get(i).getKey();
                }
                double nextMoveWeight = (elapsedTime - rewindTime) / (double) (elapsedTime - (currentTime - times.get(i + 1).getValue()));
                Location before = times.get(i).getKey().clone();
                Location after = times.get(i + 1).getKey();
                Vector interpolate = after.toVector().subtract(before.toVector());
                interpolate.multiply(nextMoveWeight);
                before.add(interpolate);
                return before;
            }
        }
        return player.getLocation(); //can't find a suitable position
    }

    private void processPosition(Location loc, Player p) {
        List<Pair<Location, Long>> times = locationTimes.getOrDefault(p.getUniqueId(), new ArrayList<>());
        long currTime = System.currentTimeMillis();
        if (times.size() > 0 && currTime - times.get(times.size() - 1).getValue() < TIME_RESOLUTION)
            return;
        times.add(new Pair<>(loc, currTime));
        if (times.size() > historySize) times.remove(0);
        locationTimes.put(p.getUniqueId(), times);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        processPosition(e.getTo(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent e) {
        processPosition(e.getRespawnLocation(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        processPosition(e.getTo(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        processPosition(e.getPlayer().getLocation(), e.getPlayer());
    }


    public int getHistorySize() {
        return historySize;
    }

    public int getPingOffset() {
        return pingOffset;
    }
}
