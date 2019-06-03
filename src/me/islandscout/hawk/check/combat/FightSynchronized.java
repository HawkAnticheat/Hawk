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
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.InteractAction;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.Pair;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FightSynchronized exploits a flaw in aim-bot/aim-assist cheats
 * by comparing the player's last attack time to their last move
 * time. Although easily bypassed, it catches a significant
 * number of cheaters.
 */
public class FightSynchronized extends CustomCheck implements Cancelless {

    private final Map<UUID, Long> attackTime;
    private final Map<UUID, Pair<Integer, Integer>> sample;
    private final int SAMPLE_SIZE;
    private static final int THRESHOLD = 6;

    public FightSynchronized() {
        super("fightsync", true, -1, 2, 0.95, 5000, "%player% may be using killaura (sync). VL %vl%", null);
        attackTime = new HashMap<>();
        sample = new HashMap<>();
        SAMPLE_SIZE = (int)customSetting("sampleSize", "", 10);
    }

    @Override
    public void check(Event event) {
        if(event instanceof MoveEvent) {
            processMove((MoveEvent)event);
        }
        else if(event instanceof InteractEntityEvent) {
            processHit((InteractEntityEvent)event);
        }
    }

    private void processMove(MoveEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        long lastAttackTime = attackTime.getOrDefault(p.getUniqueId(), -50L);
        long mToA = lastAttackTime - pp.getLastMoveTime(); //difference between previous move time to last attack time
        long aToM = System.currentTimeMillis() - lastAttackTime; //difference between last attack time to now

        if(mToA < 0) {
            return; //last attack was more than 50ms ago
        }

        if(mToA + aToM < 40) {
            return; //connection catching up from lag spike, so ignore
        }

        Pair<Integer, Integer> ratio = sample.getOrDefault(p.getUniqueId(), new Pair<>(0, 0));
        ratio.setValue(ratio.getValue() + 1);

        if(mToA < THRESHOLD) {
            ratio.setKey(ratio.getKey() + 1);
        }

        if(ratio.getValue() >= SAMPLE_SIZE) {
            if(ratio.getKey() / (double)ratio.getValue() > 0.9) {
                punish(pp, false, e);
            }
            else {
                reward(pp);
            }
            ratio.setKey(0);
            ratio.setValue(0);
        }

        sample.put(p.getUniqueId(), ratio);
    }

    private void processHit(InteractEntityEvent e) {
        if(e.getInteractAction() == InteractAction.ATTACK)
            attackTime.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    public void removeData(Player p) {
        attackTime.remove(p.getUniqueId());
        sample.remove(p.getUniqueId());
    }
}
