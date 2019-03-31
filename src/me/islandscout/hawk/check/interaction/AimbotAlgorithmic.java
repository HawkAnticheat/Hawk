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
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * The AimbotAlgorithmic check detects aimbots by
 * checking for inconsistencies in head pitch
 * (specifically vertical movement). It does this by
 * comparing the greatest common divisor between
 * each sample of mouse moves.
 */
public class AimbotAlgorithmic extends MovementCheck {

    private final Map<UUID, List<Float>> deltaPitches;
    private final Map<UUID, Float> lastDeltaPitchGCDs;
    private static final int GCD_COMPARE_SIZE = 11;

    public AimbotAlgorithmic() {
        super("aimbotalgorithmic", true, -1, 5, 0.9, 5000, "%player% may be using aimbot (algorithmic), VL: %vl%", null);
        this.deltaPitches = new HashMap<>();
        this.lastDeltaPitchGCDs = new HashMap<>();
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        UUID uuid = pp.getUuid();
        float deltaPitch = e.getTo().getPitch() - e.getFrom().getPitch();
        List<Float> lastDeltaPitches = deltaPitches.getOrDefault(uuid, new ArrayList<>());

        if(deltaPitch != 0 && deltaPitch < 10 && Math.abs(e.getTo().getPitch()) != 90)
            lastDeltaPitches.add(Math.abs(deltaPitch));

        //Only check for pitch
        //For some reason when you spin your head around too much,
        //yaw checking becomes unreliable. Precision errors?
        //Still, this check is pretty impressive.
        if(lastDeltaPitches.size() >= GCD_COMPARE_SIZE) {
            float deltaPitchGCD = MathPlus.gcdRational(lastDeltaPitches);
            float lastDeltaPitchGCD = lastDeltaPitchGCDs.getOrDefault(uuid, deltaPitchGCD);
            float gcdDiff = Math.abs(deltaPitchGCD - lastDeltaPitchGCD);
            if(gcdDiff > 0.001) {
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
