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

package me.islandscout.hawk.check.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.check.EntityInteractionCheck;
import me.islandscout.hawk.event.*;
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.entity.Player;

import java.util.*;

public class FightSpeedConsistency extends EntityInteractionCheck implements Cancelless {

    //This check is very similar to the FightSpeed check, however I will
    //not be merging these, because their SAMPLES variable may vary
    //based on configuration. For instance, FightSpeed relies on samples
    //of 10, yet this check relies on samples of 5.

    private final int SAMPLES;
    private final int SAMPLE_SIZE;
    private final double STDEV_THRESHOLD;
    private final double MIN_CPS;
    private final int RECORD_THRESHOLD; //don't log click if it took longer than these ticks

    private final Map<UUID, List<Double>> pSamples;
    private final Map<UUID, Long> lastClickTick;
    private final Map<UUID, List<Long>> deltaTimes;

    public FightSpeedConsistency() {
        super("fightspeedconsistency", true, -1, 3, 0.99, 5000, "%player% failed click consistency. Autoclicker? VL: %vl%", null);
        MIN_CPS = (double)customSetting("checkAboveCPS", "", 10D);
        SAMPLES = (int)customSetting("samples", "", 5);
        SAMPLE_SIZE = (int)customSetting("sampleSize", "", 20);
        STDEV_THRESHOLD = (double)customSetting("stdevThreshold", "", 0.05D);
        RECORD_THRESHOLD = (int)(20 * (1 / MIN_CPS) + 1);
        pSamples = new HashMap<>();
        lastClickTick = new HashMap<>();
        deltaTimes = new HashMap<>();
    }

    @Override
    protected void check(InteractEntityEvent e) {
        if (e.getInteractAction() == InteractAction.INTERACT)
            return;
        UUID uuid = e.getPlayer().getUniqueId();
        HawkPlayer pp = e.getHawkPlayer();
        if (lastClickTick.containsKey(uuid)) {
            List<Long> deltaTs = deltaTimes.getOrDefault(uuid, new ArrayList<>());
            long deltaT = (pp.getCurrentTick() - lastClickTick.get(uuid));

            //log click if player clicks faster than this
            if (deltaT <= RECORD_THRESHOLD) {
                deltaTs.add(deltaT);

                //begin computing avg cps
                if (deltaTs.size() >= SAMPLE_SIZE) {
                    double avgCps = 0;
                    for (double entry : deltaTs) {
                        avgCps += entry;
                    }
                    double divisor = (avgCps / SAMPLE_SIZE / 20);
                    avgCps = 1 / divisor;

                    List<Double> sampless = pSamples.getOrDefault(uuid, new ArrayList<>());
                    sampless.add(avgCps);

                    //Debug.broadcastMessage(avgCps + "");

                    //Begin computing standard deviation and judge player.
                    //Only if above min cps and enough samples.
                    //Acts like a narrow barrier between the two limits to prevent false flags.
                    //Explanation: When clicks are barely meeting speed requirements, a narrow
                    //range of CPSs will be processed, thus setting off false flags.
                    if(avgCps > MIN_CPS && sampless.size() >= SAMPLES) {
                        //Debug.broadcastMessage(ChatColor.YELLOW + "" + MathPlus.stdev(sampless));
                        if(MathPlus.stdev(sampless) < STDEV_THRESHOLD) {
                            punish(pp, false, e);
                        }
                        else {
                            reward(pp);
                        }
                    }

                    if(sampless.size() >= SAMPLES) {
                        sampless.remove(0);
                    }
                    pSamples.put(uuid, sampless);
                    deltaTs.remove(0);
                }
                deltaTimes.put(uuid, deltaTs);
            }
        }
        lastClickTick.put(uuid, pp.getCurrentTick());
    }

    @Override
    public void removeData(Player p) {
        pSamples.remove(p.getUniqueId());
        lastClickTick.remove(p.getUniqueId());
        deltaTimes.remove(p.getUniqueId());
    }
}
