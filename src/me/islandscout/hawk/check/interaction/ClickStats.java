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

package me.islandscout.hawk.check.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.ArmSwingEvent;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.util.Debug;

import java.util.*;

public class ClickStats extends CustomCheck implements Cancelless {

    //Impossible to swing twice in a tick, unless attacking or hitting block or in creative
    // It has to do with the stupid "leftClickCounter" in Minecraft.java

    private Map<UUID, Map<Integer, Integer>> histogramMap;
    private Map<UUID, Long> lastClickTimeMap;
    private Map<UUID, Integer> totalClicksMap;

    private static final int MAX_PERIOD = 4;
    private static final int SAMPLES = 80;

    public ClickStats() {
        super("clickstats", true, -1, 0, 0.99, 5000, "%player% failed click statistics, VL: %vl%", null);
        this.histogramMap = new HashMap<>();
        this.lastClickTimeMap = new HashMap<>();
        this.totalClicksMap = new HashMap<>();
    }

    @Override
    protected void check(Event event) {
        if(event instanceof ArmSwingEvent) {

            HawkPlayer pp = event.getHawkPlayer();

            if(!lastClickTimeMap.containsKey(pp.getUuid())) {
                lastClickTimeMap.put(pp.getUuid(), pp.getCurrentTick());
                return;
            }

            long currTime = pp.getCurrentTick();
            long lastClickTime = lastClickTimeMap.getOrDefault(pp.getUuid(), currTime);

            int dTicks = (int) (currTime - lastClickTime);

            if(dTicks <= MAX_PERIOD) {
                Map<Integer, Integer> histogram = histogramMap.getOrDefault(pp.getUuid(), new HashMap<>());

                histogram.put(dTicks, histogram.getOrDefault(dTicks, 0) + 1);

                int totalClicks = totalClicksMap.getOrDefault(pp.getUuid(), 0) + 1;
                totalClicksMap.put(pp.getUuid(), totalClicks);

                if (totalClicks >= SAMPLES) {

                    totalClicksMap.put(pp.getUuid(), 0);

                    //Debug.broadcastMessage("CLICKER HISTOGRAM");
                    for(int i = 0; i <= MAX_PERIOD; i++) {

                        StringBuilder sb = new StringBuilder(i + ": ");

                        for(int j = 0; j < histogram.getOrDefault(i, 0); j++) {
                            sb.append("*");
                        }

                        //Debug.broadcastMessage(sb.toString());
                    }

                    histogram.clear();
                }

                histogramMap.put(pp.getUuid(), histogram);


            }

            lastClickTimeMap.put(pp.getUuid(), currTime);
        }
    }
}
