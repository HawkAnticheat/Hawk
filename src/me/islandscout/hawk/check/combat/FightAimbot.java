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

package me.islandscout.hawk.check.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.event.PositionEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FightAimbot exploits a flaw in aim-bot cheats by
 * analyzing mouse movement patterns during combat. Although
 * easily bypassed, it catches a significant number of cheaters.
 */
public class FightAimbot extends CustomCheck implements Cancelless {

    //PASSED (9/11/18)

    private final Map<UUID, Double> lastLookDistanceSquared;
    private final Map<UUID, Long> lastAttackTick;

    public FightAimbot() {
        super("fightaimbot", true, -1, 5, 0.97, 5000, "%player% may be using aimbot. VL %vl%", null);
        lastLookDistanceSquared = new HashMap<>();
        lastAttackTick = new HashMap<>();
    }

    public void check(Event e) {
        if (e instanceof PositionEvent) {
            processMove((PositionEvent) e);
        } else if (e instanceof InteractEntityEvent) {
            processHit((InteractEntityEvent) e);
        }
    }

    private void processMove(PositionEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        UUID uuid = p.getUniqueId();
        if (pp.getCurrentTick() - lastAttackTick.getOrDefault(uuid, 0L) > 2)
            return;
        double lookDistanceSquared = pp.getDeltaYaw() * pp.getDeltaYaw() + pp.getDeltaPitch() * pp.getDeltaPitch();

        if (lastLookDistanceSquared.containsKey(uuid)) {
            if (lastLookDistanceSquared.get(uuid) > 8 && lookDistanceSquared < 0.001 && System.currentTimeMillis() - pp.getLastMoveTime() < 60) {
                punish(pp, false, e);
            } else {
                reward(pp);
            }
        }

        lastLookDistanceSquared.put(uuid, lookDistanceSquared);
    }

    private void processHit(InteractEntityEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        HawkPlayer pp = e.getHawkPlayer();
        lastAttackTick.put(uuid, pp.getCurrentTick());
    }

    public void removeData(Player p) {
        lastLookDistanceSquared.remove(p.getUniqueId());
        lastAttackTick.remove(p.getUniqueId());
    }
}
