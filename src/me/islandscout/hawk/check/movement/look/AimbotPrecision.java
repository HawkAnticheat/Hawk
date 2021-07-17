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

package me.islandscout.hawk.check.movement.look;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * The AimbotPrecision check detects aim-bots by
 * checking for variation in head pitch precision.
 * It does this by solving the greatest common divisor
 * of the samples. All pitch changes should be divisible
 * by a constant, which is determined by the in-game
 * mouse sensitivity. Not compatible with cinematic
 * camera mode.
 *
 * The main issue with this approach to detecting aimbots
 * (besides cinematic camera mode) is its unreliability
 * at high client framerates. Because rotation is updated
 * per frame, there can be multiple rotation updates per
 * tick. Every time a new delta yaw/pitch is added to the
 * current rotation, there is precision loss due to the
 * arithmetic involved. These errors can add up very quickly,
 * ultimately causing the GCD algorithm to fail and
 * produce false-positives.
 */
public class AimbotPrecision extends MovementCheck implements Cancelless {

    //This concept could be used to determine someone's in-game sensitivity.
    //That's pretty spoopy if you ask me.

    //Inspired by https://www.spigotmc.org/threads/killaura-detection.143226/page-17#post-2821779

    private final Map<UUID, List<Float>> deltaPitches;
    private final Map<UUID, Float> lastDeltaPitchGCDs;

    private final int SAMPLES;
    private final float PITCHRATE_LIMIT;

    public AimbotPrecision() {
        super("aimbotprecision", false, -1, 5, 0.9, 5000, "%player% failed aimbot (precision), VL: %vl%", null);
        SAMPLES = (int)customSetting("samples", "", 10);
        PITCHRATE_LIMIT = (float)((double)customSetting("ignorePitchrateHigherThan", "", 0.96D));
        this.deltaPitches = new HashMap<>();
        this.lastDeltaPitchGCDs = new HashMap<>();
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        UUID uuid = pp.getUuid();
        float deltaPitch = e.getTo().getPitch() - e.getFrom().getPitch();
        List<Float> lastDeltaPitches = deltaPitches.getOrDefault(uuid, new ArrayList<>());

        //ignore if deltaPitch is 0 or >= 10 or if pitch is +/-90.
        if(deltaPitch != 0 && Math.abs(deltaPitch) <= PITCHRATE_LIMIT && Math.abs(e.getTo().getPitch()) != 90) {
            lastDeltaPitches.add(Math.abs(deltaPitch));
        }

        //Only check for pitch
        //For some reason when you spin your head around too much,
        //yaw checking becomes unreliable. Precision errors?
        //Still, this check is pretty impressive.
        if(lastDeltaPitches.size() >= SAMPLES) {

            float deltaPitchGCD = MathPlus.gcdRational(lastDeltaPitches);
            float gcdDiff = Math.abs(deltaPitchGCD - lastDeltaPitchGCDs.getOrDefault(uuid, deltaPitchGCD));

            if(gcdDiff > 0.001) {

                //Assuming sens didn't change, add last GCD in our delta pitches
                //just in case we didn't collect enough samples to compute the same GCD.
                //Try again. Mathematically, the GCD should remain the same.
                if(lastDeltaPitchGCDs.containsKey(uuid)) {
                    float lastDeltaPitchGCD = lastDeltaPitchGCDs.get(uuid);
                    if(lastDeltaPitchGCD > 0.001) {
                        lastDeltaPitches.add(lastDeltaPitchGCD);
                        deltaPitchGCD = MathPlus.gcdRational(lastDeltaPitches);
                        gcdDiff = Math.abs(deltaPitchGCD - lastDeltaPitchGCDs.getOrDefault(uuid, deltaPitchGCD));
                    }
                }
            }

            //if GCD is practically unsolvable
            if(deltaPitchGCD < 0.00001) {
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
        punish(pp, false, e);
    }

    @Override
    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        deltaPitches.remove(uuid);
        lastDeltaPitchGCDs.remove(uuid);
    }
}
