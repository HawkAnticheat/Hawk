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

import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.Pair;
import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.util.ConfigHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LagCompensator implements Listener {

    //https://developer.valvesoftware.com/wiki/Lag_compensation
    //https://www.youtube.com/watch?v=6EwaW2iz4iA
    //http://www.gabrielgambetta.com/lag-compensation.html
    //http://www.gabrielgambetta.com/client-side-prediction-live-demo.html
    //https://en.wikipedia.org/wiki/Lag#Rewind_time

    //Yes, I use System.currentTimeMillis()! Should the main thread start lagging
    //behind, the lag compensator will not be affected. The lag compensator
    //should be dependent on system time and not server ticks. System time is
    //stable for this application and is thus a reliable time reference for
    //measuring latency.

    private final Map<Entity, List<Pair<Location, Long>>> trackedEntities;
    private final int historySize;
    private final int pingOffset;
    private final boolean DEBUG;

    public LagCompensator(Hawk hawk) {
        this.trackedEntities = new ConcurrentHashMap<>();
        historySize = ConfigHelper.getOrSetDefault(20, hawk.getConfig(), "lagCompensation.historySize");
        pingOffset = ConfigHelper.getOrSetDefault(175, hawk.getConfig(), "lagCompensation.pingOffset");
        DEBUG = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "lagCompensation.debug");
        Bukkit.getPluginManager().registerEvents(this, hawk);

        hawk.getHawkSyncTaskScheduler().addRepeatingTask(new Runnable() {
            @Override
            public void run() {

                Set<Entity> collectedEntities = new HashSet<>();

                for(Player p : Bukkit.getOnlinePlayers()) {
                    List<Entity> nearbyEntities = p.getNearbyEntities(20, 10, 20);
                    for(Entity entity : nearbyEntities) {
                        //add anything that moves and is clickable
                        if(entity instanceof LivingEntity || entity instanceof Vehicle || entity instanceof Fireball) {
                            collectedEntities.add(entity);
                        }
                    }
                }

                for(Entity entity : collectedEntities) {
                    trackedEntities.put(entity, trackedEntities.getOrDefault(entity, new ArrayList<>()));
                }

                Set<Entity> expiredEntities = new HashSet<>(trackedEntities.keySet());
                expiredEntities.removeAll(collectedEntities);

                for(Entity expired : expiredEntities) {
                    trackedEntities.remove(expired);
                }

            }
        }, 20);

        hawk.getHawkSyncTaskScheduler().addRepeatingTask(new Runnable() {
            @Override
            public void run() {
                for(Entity entity : trackedEntities.keySet()) {
                    processPosition(entity);
                }
            }
        }, 1);
    }

    //Uses linear interpolation to get the best location
    //TODO: handle world change
    public Location getHistoryLocation(int rewindMillisecs, Entity entity) {
        List<Pair<Location, Long>> times = trackedEntities.get(entity);
        if (times == null) {
            return entity.getLocation();
        }
        long currentTime = System.currentTimeMillis();
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
        return entity.getLocation(); //can't find a suitable position
    }

    private void processPosition(Entity entity) {
        List<Pair<Location, Long>> times = trackedEntities.getOrDefault(entity, new ArrayList<>());
        long currTime = System.currentTimeMillis();
        if(DEBUG && entity instanceof Player)
            entity.sendMessage(ChatColor.GRAY + "[Lag Compensator] Your moves are being recorded: " + currTime);
        times.add(new Pair<>(entity.getLocation(), currTime));
        if (times.size() > historySize) times.remove(0);
        trackedEntities.put(entity, times);
    }


    /*
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        processPosition(e.getTo(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent e) {
        processPosition(e.getRespawnLocation(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        processPosition(e.getTo(), e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        processPosition(e.getPlayer().getLocation(), e.getPlayer());
    }
    */


    public int getHistorySize() {
        return historySize;
    }

    public int getPingOffset() {
        return pingOffset;
    }
}
