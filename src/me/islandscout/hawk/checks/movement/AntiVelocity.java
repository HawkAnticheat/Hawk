/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.utils.Pair;
import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.MovementCheck;
import me.islandscout.hawk.checks.Cancelless;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import me.islandscout.hawk.utils.ConfigHelper;
import me.islandscout.hawk.utils.Debug;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class AntiVelocity extends MovementCheck implements Listener, Cancelless {

    //this is such a pain

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
                    Pair<Vector, Long> kb = kbs.get(kbIndex);
                    if (currTime - kb.getValue() <= ping + 200) {
                        if (currVelocity.angle(kb.getKey()) < 0.26 && currVelocity.clone().subtract(kb.getKey()).length() < 0.14) {
                            reward(pp);
                            kbs = kbs.subList(kbIndex + 1, kbs.size());
                            break;
                        }
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
    public void onVelocity(PlayerVelocityEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        Vector vector = null;
        if (Hawk.getServerVersion() == 7) {
            vector = e.getVelocity();
        } else if (Hawk.getServerVersion() == 8) {
            //lmao Bukkit is broken. event velocity is broken when attacked by a player (NMS.EntityHuman.java, attack(Entity))
            vector = e.getPlayer().getVelocity();
        }
        if (vector == null)
            return;

        List<Pair<Vector, Long>> kbs = velocities.getOrDefault(uuid, new ArrayList<>());
        kbs.add(new Pair<>(vector, System.currentTimeMillis()));
        velocities.put(uuid, kbs);
    }

    @Override
    public void removeData(Player p) {
        velocities.remove(p.getUniqueId());
    }
}
