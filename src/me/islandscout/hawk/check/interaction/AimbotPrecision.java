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
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * The AimbotPrecision check detects aimbots by
 * checking for variation in head pitch precision.
 * It does this by solving the greatest common divisor
 * of the samples. All pitch changes should be divisible
 * by a constant, which is determined by the in-game
 * sensitivity. Not compatible with cinematic camera mode.
 */
public class AimbotPrecision extends MovementCheck implements Cancelless {

    //This concept could be used to determine someone's in-game sensitivity.
    //That's pretty spoopy if you ask me.

    private final Map<UUID, List<Float>> deltaPitches;
    private final Map<UUID, Float> lastDeltaPitchGCDs;

    //SAMPLES should be higher for lower mouse sensitivities
    //in order to accurately predict pitch precision
    private static final int SAMPLES = 20;

    public AimbotPrecision() {
        super("aimbotprecision", true, -1, 5, 0.9, 5000, "%player% may be using aimbot (precision), VL: %vl%", null);
        this.deltaPitches = new HashMap<>();
        this.lastDeltaPitchGCDs = new HashMap<>();
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        UUID uuid = pp.getUuid();
        float deltaPitch = e.getTo().getPitch() - e.getFrom().getPitch();
        List<Float> lastDeltaPitches = deltaPitches.getOrDefault(uuid, new ArrayList<>());

        //ignore if deltaPitch is 0 and if pitch is +/-90. Also ignore if pitchrate is too high
        if(deltaPitch != 0 && Math.abs(deltaPitch) < 10 && Math.abs(e.getTo().getPitch()) != 90) {
            lastDeltaPitches.add(Math.abs(deltaPitch));
        }

        //Only check for pitch
        //For some reason when you spin your head around too much,
        //yaw checking becomes unreliable. Precision errors?
        //Still, this check is pretty impressive.
        if(lastDeltaPitches.size() >= SAMPLES) {
            float deltaPitchGCD = MathPlus.gcdRational(lastDeltaPitches);
            float lastDeltaPitchGCD = lastDeltaPitchGCDs.getOrDefault(uuid, deltaPitchGCD);
            float gcdDiff = Math.abs(deltaPitchGCD - lastDeltaPitchGCD);
            //if GCD is significantly different or if GCD was unsolvable
            if(gcdDiff > 0.001 || deltaPitchGCD < 0.00001) {
                fail(pp, e);
            }
            else
                reward(pp);
            lastDeltaPitches.clear();
            lastDeltaPitchGCDs.put(uuid, deltaPitchGCD);
        }

        deltaPitches.put(uuid, lastDeltaPitches);
    }

    private void fail(HawkPlayer pp, MoveEvent e) {
        punishAndTryRubberband(pp, e, e.getPlayer().getLocation());
    }

    @Override
    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        deltaPitches.remove(uuid);
        lastDeltaPitchGCDs.remove(uuid);
    }
}
