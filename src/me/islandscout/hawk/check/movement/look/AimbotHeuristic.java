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
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.event.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * AimbotHeuristic exploits flaws in aim-bot cheats by
 * analyzing mouse movement patterns during interaction. Although
 * easily bypassed, it catches a significant number of cheaters.
 * Caution is advised since this check may false flag during
 * certain circumstances. This check should only be used as a
 * hint that the player might be cheating.
 */
public class AimbotHeuristic extends CustomCheck implements Cancelless {

    private final Map<UUID, List<Vector>> mouseMoves;
    private final Map<UUID, List<Long>> clickTimes;

    private static final int MOVES_PER_SAMPLE = 4; //must be greater than 0
    private final int MOVES_AFTER_HIT;

    public AimbotHeuristic() {
        super("aimbotheuristic", false, -1, 5, 0.99, 5000, "&8%player% may be using aimbot (heuristic), VL %vl%", null);
        mouseMoves = new HashMap<>();
        clickTimes = new HashMap<>();

        MOVES_AFTER_HIT = MOVES_PER_SAMPLE - MOVES_PER_SAMPLE / 2;
    }

    public void check(Event e) {
        if (e instanceof MoveEvent) {
            processMove((MoveEvent) e);
        } else if (e instanceof InteractEntityEvent || e instanceof InteractWorldEvent) {
            processClick(e);
        }
    }

    private void processMove(MoveEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        UUID uuid = p.getUniqueId();

        List<Vector> lastMoves = mouseMoves.getOrDefault(uuid, new ArrayList<>());
        Vector mouseMove = new Vector(e.getTo().getYaw() - e.getFrom().getYaw(), e.getTo().getPitch() - e.getFrom().getPitch(), 0);

        lastMoves.add(mouseMove);
        //make size 1 bigger so that we can get the move before
        //the first move that we check
        if(lastMoves.size() > MOVES_PER_SAMPLE + 1) {
            lastMoves.remove(0);
        }
        mouseMoves.put(uuid, lastMoves);

        if(clickedXMovesBefore(MOVES_AFTER_HIT, pp)) {
            double minSpeed = Double.MAX_VALUE;
            double maxSpeed = 0D;
            double maxAngle = 0D;
            for(int i = 1; i < lastMoves.size(); i++) {
                Vector lastMouseMove = lastMoves.get(0);
                Vector currMouseMove = lastMoves.get(i);
                double speed = currMouseMove.length();
                double lastSpeed = lastMouseMove.length();
                double angle = (lastSpeed != 0 && lastSpeed != 0) ? lastMouseMove.angle(currMouseMove) : 0D;
                if(Double.isNaN(angle))
                    angle = 0D;
                maxSpeed = Math.max(speed, maxSpeed);
                minSpeed = Math.min(speed, minSpeed);
                maxAngle = Math.max(angle, maxAngle);

                //stutter
                if(maxSpeed - minSpeed > 4 && minSpeed < 0.01 && maxAngle < 0.1 /*&& lastSpeed > 1*/) { //this lastSpeed check eliminates a false positive
                    punishEm(pp, e);
                }
                //twitching or zig zags
                else if(speed > 20 && lastSpeed > 20 && angle > 2.25) {
                    punishEm(pp, e);
                }
                //jump discontinuity
                else if(speed - lastSpeed < -30) {  //TODO: & angle must be greater than X
                    punishEm(pp, e);
                }
                else {
                    reward(pp);
                }
            }
        }
    }

    private boolean clickedXMovesBefore(long x, HawkPlayer pp) {
        List<Long> clickTimess = clickTimes.getOrDefault(pp.getUuid(), new ArrayList<>());
        long time = pp.getCurrentTick() - x;
        for(int i = 0; i < clickTimess.size(); i++) {
            if(time == clickTimess.get(i)) {
                clickTimess.remove(i);
                return true;
            }
        }
        return false;
    }

    private void processClick(Event e) {
        UUID uuid = e.getPlayer().getUniqueId();
        List<Long> clickTimess = clickTimes.getOrDefault(uuid, new ArrayList<>());
        long currTick = e.getHawkPlayer().getCurrentTick();
        if(!clickTimess.contains(currTick))
            clickTimess.add(e.getHawkPlayer().getCurrentTick());

        //memory leak fail-safe, if necessary?
        for(int i = clickTimess.size() - 1; i >= 0; i--) {
            if(currTick - clickTimess.get(i) > MOVES_PER_SAMPLE + 1)
                clickTimess.remove(i);
        }

        clickTimes.put(uuid, clickTimess);
    }

    private void punishEm(HawkPlayer pp, MoveEvent e) {
        punish(pp, false, e);
    }

    @Override
    public void removeData(Player p) {
        mouseMoves.remove(p.getUniqueId());
        clickTimes.remove(p.getUniqueId());
    }
}
