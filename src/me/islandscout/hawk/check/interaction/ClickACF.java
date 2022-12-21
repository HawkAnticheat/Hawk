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

import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.ArmSwingEvent;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.MoveEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClickACF extends CustomCheck implements Cancelless {

    private static final double ACF_THRESHOLD = 0.87;
    private static final int ACF_PERIOD_THRESHOLD = 9;
    private static final double MIN_ACF_0_NORM = 13; //Normalized acf(0)
    private static final int SAMPLES = 30;
    private List<Integer> clicks;
    private int clicksThisTick;

    public ClickACF() {
        super("clickacf", true, -1, 0, 0.99, 5000, "%player% failed click auto-correlation, VL: %vl%", null);
        this.clicks = new ArrayList<>();
    }

    @Override
    protected void check(Event event) {
        if(event instanceof MoveEvent) {

            this.clicks.add(this.clicksThisTick);
            this.clicksThisTick = 0;

            if(this.clicks.size() >= SAMPLES) {

                System.out.println("clicks: " + Arrays.toString(this.clicks.toArray()));

                double[] acf = new double[SAMPLES];

                for(int i = 0; i < SAMPLES; i++) {

                    double dot = 0;
                    for(int j = 0; j < SAMPLES; j++) {

                        dot += this.clicks.get(j) * this.clicks.get((j + i) % this.clicks.size());
                    }

                    if(i == 0 && dot / (SAMPLES / 20D) < MIN_ACF_0_NORM) { //enforce minimum clickrate
                        System.out.println("CPS not high enough: " + dot / (SAMPLES / 20D));
                        break;
                    }

                    acf[i] = dot;

                    if(i > 0 && i <= ACF_PERIOD_THRESHOLD && dot > acf[0] * ACF_THRESHOLD) {
                        System.out.println("PATTERN DETECTED! threshold: " + (acf[0] * ACF_THRESHOLD) + ", period: " + i);
                    }
                }

                this.clicks.clear();

                System.out.println("ACF: " + Arrays.toString(acf));
            }



        } else if(event instanceof ArmSwingEvent) {
            this.clicksThisTick++;
        }
    }
}
