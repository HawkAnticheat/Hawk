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
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.Pair;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.PhysicsUtils;
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.*;

public class AntiVelocity extends MovementCheck implements Listener, Cancelless {

    private final Map<UUID, List<Pair<Vector, Long>>> velocities; //launch velocities

    public AntiVelocity() {
        super("antivelocity", false, -1, 5, 0.95, 5000, "%player% may be using antivelocity. VL: %vl%", null);
        velocities = new HashMap<>();
    }

    @Override
    protected void check(PositionEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        Vector currVelocity = new Vector(event.getTo().getX() - event.getFrom().getX(), event.getTo().getY() - event.getFrom().getY(), event.getTo().getZ() - event.getFrom().getZ());
        long currTime = System.currentTimeMillis();
        int ping = ServerUtils.getPing(p);
        if (!p.isFlying() && !p.isInsideVehicle()) {

            //handle any pending knockbacks
            if (velocities.containsKey(p.getUniqueId()) && velocities.get(p.getUniqueId()).size() > 0) {
                List<Pair<Vector, Long>> kbs = velocities.get(p.getUniqueId());
                //pending knockbacks must be in order; get the first entry in the list.
                //if the first entry doesn't work (probably because they were fired on the same tick),
                //then work down the list until we find something
                int kbIndex;
                for (kbIndex = 0; kbIndex < kbs.size(); kbIndex++) {
                    Pair<Vector, Long> kbPair = kbs.get(kbIndex);
                    long timeSinceKb = currTime - kbPair.getValue();
                    if (timeSinceKb <= ping + 200 && timeSinceKb > ping - 100) {
                        Vector kb = kbPair.getKey();
                        Vector diff = kb.clone().subtract(currVelocity);
                        Debug.broadcastMessage("DIFF: " + diff.length() + "");
                        Debug.broadcastMessage(kb.length() + " " + (kb.length() + sprintGroundMapping(-kb.length())) + "");

                        /*if (currVelocity.angle(kbPair.getKey()) < 0.26 && currVelocity.clone().subtract(kbPair.getKey()).length() < 0.14) {
                            reward(pp);
                            kbs = kbs.subList(kbIndex + 1, kbs.size());
                            break;
                        }*/
                    } else if (kbIndex == kbs.size() - 1){
                        //We've waited too long. Flag the player.
                        kbs.clear();
                        punish(pp, false, event);
                    }
                }
                velocities.put(p.getUniqueId(), kbs);
            }

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(HawkPlayerAsyncVelocityChangeEvent e) {
        if(e.isAdditive())
            return;
        UUID uuid = e.getPlayer().getUniqueId();
        Vector vector = e.getVelocity();

        List<Pair<Vector, Long>> kbs = velocities.getOrDefault(uuid, new ArrayList<>());
        kbs.add(new Pair<>(vector, System.currentTimeMillis()));
        velocities.put(uuid, kbs);
    }

    @Override
    public void removeData(Player p) {
        velocities.remove(p.getUniqueId());
    }

    private double sprintGroundMapping(double lastSpeed) {
        //Debug.broadcastMessage("chking walk-ground");
        return 0.546001 * lastSpeed + 0.130001;
    }

    private double sprintAirMapping(double lastSpeed) {
            return 0.910001 * lastSpeed + 0.026001;
    }
}
