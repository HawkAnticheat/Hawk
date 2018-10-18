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

package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.EntityInteractionCheck;
import me.islandscout.hawk.events.InteractAction;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.MathPlus;
import me.islandscout.hawk.utils.Placeholder;
import org.bukkit.entity.Player;

import java.util.*;

public class FightSpeed extends EntityInteractionCheck {

    //PASSED (9/11/18)

    private final Map<UUID, Long> lastClickTime; //in client ticks
    private final Map<UUID, List<Long>> deltaTimes;
    private static final double RECORD_SENSITIVITY = 4; //don't log click if it took longer than these ticks
    private static final int SAMPLES = 10;
    private final boolean CANCEL_SAME_TICK;


    public FightSpeed() {
        super("fightspeed", "%player% failed attack speed. CPS: %cps%, VL: %vl%");
        lastClickTime = new HashMap<>();
        deltaTimes = new HashMap<>();
        CANCEL_SAME_TICK = true;
    }

    @Override
    protected void check(InteractEntityEvent e) {
        if (e.getInteractAction() == InteractAction.INTERACT)
            return;
        UUID uuid = e.getPlayer().getUniqueId();
        HawkPlayer pp = e.getHawkPlayer();
        if (lastClickTime.containsKey(uuid)) {
            List<Long> deltaTs = deltaTimes.getOrDefault(uuid, new ArrayList<>());
            long deltaT = (pp.getCurrentTick() - lastClickTime.get(uuid));
            if (CANCEL_SAME_TICK && deltaT == 0)
                e.setCancelled(true);
            if (deltaT <= RECORD_SENSITIVITY) {
                deltaTs.add(deltaT);
                if (deltaTs.size() >= SAMPLES) {
                    double avgCps = 0;
                    for (double entry : deltaTs) {
                        avgCps += entry;
                    }
                    double divisor = (avgCps / SAMPLES / 20);
                    avgCps = 1 / (divisor == 0 ? Double.NaN : divisor);
                    //if someone manages to get a NaN, they're dumb af
                    if (avgCps > 15 || Double.isNaN(avgCps)) {
                        punish(pp, true, e, new Placeholder("cps", (Double.isNaN(avgCps) ? "INVALID" : MathPlus.round(avgCps, 2) + "")));
                    } else {
                        reward(pp);
                    }
                    deltaTs.remove(0);
                }
                deltaTimes.put(uuid, deltaTs);
            }
        }
        lastClickTime.put(uuid, pp.getCurrentTick());
    }

    @Override
    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        lastClickTime.remove(uuid);
        deltaTimes.remove(uuid);
    }
}
