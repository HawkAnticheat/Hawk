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

package me.islandscout.hawk.check.movement;

import me.islandscout.hawk.event.bukkit.HawkPlayerAsyncVelocityChangeEvent;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stops water walk NCP bypass.
 */
public class LiquidExit extends MovementCheck implements Listener {

    private final Map<UUID, Pair<Double, Long>> kbTime; //[Pair] K: vertical velocity, V: timestamp

    public LiquidExit() {
        super("liquidexit", true, 0, 3, 0.99, 5000, "%player% failed liquid exit. VL: %vl%", null);
        kbTime = new HashMap<>();
    }

    @Override
    protected void check(MoveEvent e) {
        Player p = e.getPlayer();
        if (p.isFlying() || p.isInsideVehicle())
            return;

        Location from = e.getFrom();
        Location to = e.getTo();
        Location fromModdedY = from.clone(); //use this location to stop a false flag when swimming out of a waterfall
        fromModdedY.setY(to.getY());
        double deltaY = to.getY() - from.getY();

        //emerged upwards from liquid
        //TODO: If a player does emerge from liquid, enforce correct Y velocity
        if (deltaY > 0 && AdjacentBlocks.blockAdjacentIsLiquid(from) && !AdjacentBlocks.blockAdjacentIsLiquid(to) &&
                !AdjacentBlocks.blockAdjacentIsLiquid(fromModdedY) && !(AdjacentBlocks.blockNearbyIsSolid(from, true) || AdjacentBlocks.blockNearbyIsSolid(to, true))) {
            Pair<Double, Long> kb = kbTime.getOrDefault(p.getUniqueId(), new Pair<>(0D, 0L));
            long ticksSinceKb = System.currentTimeMillis() - kb.getValue();
            ticksSinceKb /= 50;
            //check if they're being knocked out of the water
            if (PhysicsUtils.waterYVelFunc(kb.getKey(), ticksSinceKb) < 0) {
                punishAndTryRubberband(e.getHawkPlayer(), e, p.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVelocity(HawkPlayerAsyncVelocityChangeEvent e) {
        if(e.isAdditive())
            return;
        Vector vector = e.getVelocity();
        kbTime.put(e.getPlayer().getUniqueId(), new Pair<>(vector.getY(), System.currentTimeMillis() + ServerUtils.getPing(e.getPlayer())));
    }

    @Override
    public void removeData(Player p) {
        kbTime.remove(p.getUniqueId());
    }
}
