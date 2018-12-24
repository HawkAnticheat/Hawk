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
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.Debug;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * FightAimbot exploits flaws in aim-bot cheats by
 * analyzing mouse movement patterns during combat. Although
 * easily bypassed, it catches a significant number of cheaters.
 * Aim accuracy should also be accounted for since this check
 * may false flag during certain circumstances.
 */
public class FightAimbot extends CustomCheck implements Cancelless {

    //TODO: Optimize

    private final Map<UUID, List<Vector>> lastMouseMovess;
    private final Map<UUID, List<MouseSample>> mouseSampless;

    private static final int MOVES_PER_SAMPLE = 3; //must be 2 or more
    private final int MOVES_BEFORE_HIT;

    public FightAimbot() {
        super("fightaimbot", true, -1, 5, 0.93, 5000, "%player% may be using aimbot. VL %vl%", null);
        lastMouseMovess = new HashMap<>();
        mouseSampless = new HashMap<>();
        MOVES_BEFORE_HIT = MOVES_PER_SAMPLE / 2;
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
        List<Vector> lastMoves = lastMouseMovess.getOrDefault(uuid, new ArrayList<>());
        Vector mouseMove = new Vector(pp.getDeltaYaw(), pp.getDeltaPitch(), 0);
        List<MouseSample> mouseSamples = mouseSampless.getOrDefault(uuid, new ArrayList<>());

        for(int i = mouseSamples.size() - 1; i >= 0; i--) {
            MouseSample sample = mouseSamples.get(i);
            if(sample.moves.size() < MOVES_PER_SAMPLE) {
                sample.moves.add(mouseMove);
            }
            else {
                boolean result = analyze(sample);

                if(!result) {
                    punish(pp, false, e);
                }
                else {
                    reward(pp);
                }

                mouseSamples.remove(sample);
            }
        }

        if(lastMoves.size() >= MOVES_BEFORE_HIT) {
            lastMoves.remove(0);
        }
        lastMoves.add(mouseMove);
        lastMouseMovess.put(uuid, lastMoves);

        mouseSampless.put(uuid, mouseSamples);
    }

    private void processHit(InteractEntityEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        List<Vector> lastMoves = lastMouseMovess.getOrDefault(uuid, new ArrayList<>());
        MouseSample sample = new MouseSample(lastMoves);
        List<MouseSample> mouseSamples = mouseSampless.getOrDefault(uuid, new ArrayList<>());
        mouseSamples.add(sample);
        mouseSampless.put(uuid, mouseSamples);
    }

    private boolean analyze(MouseSample sample) {
        return analyzeStutter(sample) && analyzeTwitch(sample) && analyzeJump(sample);
    }

    private boolean analyzeStutter(MouseSample sample) {
        double minSpeed = Double.MAX_VALUE;
        double maxSpeed = 0D;
        Vector prevVector = null;
        double maxAngle = 0D;
        double lastSpeed = 0D;
        for(Vector vector : sample.moves) {
            double speed = vector.length();
            maxSpeed = Math.max(speed, maxSpeed);
            minSpeed = Math.min(speed, minSpeed);
            if(prevVector != null && prevVector.lengthSquared() != 0 && vector.lengthSquared() != 0)
                maxAngle =  Math.max(maxAngle, prevVector.angle(vector));
            prevVector = vector;
            lastSpeed = speed;
        }
        if(Double.isNaN(maxAngle))
            maxAngle = 0D;
        return !(maxSpeed - minSpeed > 4 && minSpeed < 0.01 && maxAngle < 0.1 /*&& lastSpeed > 1*/); //this lastSpeed check eliminates a false positive
    }

    private boolean analyzeTwitch(MouseSample sample) {
        double prevSpeed = 0;
        Vector prevVector = null;
        for(Vector vector : sample.moves) {
            double speed = vector.length();

            if(prevVector != null && speed > 20 && prevSpeed > 20 && vector.angle(prevVector) > 2.25) { //perhaps make these configurable constants?
                return false;
            }
            prevVector = vector;
            prevSpeed = speed;
        }
        return true;
    }

    private boolean analyzeJump(MouseSample sample) {
        double prevSpeed = 0;
        for(Vector vector : sample.moves) {
            double speed = vector.length();
            double decceleration = prevSpeed - speed;
            if(decceleration > 30) //TODO: & angle must be greater than X
                return false;
            prevSpeed = speed;
        }
        return true;
    }

    public void removeData(Player p) {
        lastMouseMovess.remove(p.getUniqueId());
        mouseSampless.remove(p.getUniqueId());
    }

    private class MouseSample {

        private List<Vector> moves;

        private MouseSample(List<Vector> moves) {
            this.moves = new ArrayList<>();
            this.moves.addAll(moves);
        }
    }

    public enum AimbotType {
        STUTTER,
        TWITCH,
        JUMP
    }
}
