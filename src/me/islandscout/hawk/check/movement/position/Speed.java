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

package me.islandscout.hawk.check.movement.position;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.*;

public class Speed extends MovementCheck implements Listener {

    //I legit hate this game's movement.
    //Do you realize how much easier it would be if
    //the server handled movement?

    //TODO False flag with pistons

    //Basically, this check is doing, "if your previous speed was X then your current speed must not exceed f(X)"

    private static final double EPSILON = 0.000001;
    //Now what we can do is predict how far a player could have travelled
    //if the move doesn't have a deltaPos due to insignificance. It might reduce false-
    //positives, but it might make the check more vulnerable to abuse since players can
    //send a bunch of non-deltaPos moves, and then send one with a great deltaPos. To
    //other players, this may look like teleportation or some sort of lag switch.
    private static final long MAX_NO_MOVES = 9;
    private final double DISCREPANCY_THRESHOLD;
    private final double VL_FAIL_DISCREPANCY_FACTOR;
    private final boolean RESET_DISCREPANCY_ON_FAIL;
    private final boolean DEBUG;

    private final Map<UUID, Double> prevSpeed;
    private final Map<UUID, Double> discrepancies;
    private final Map<UUID, Integer> noMovesMap;
    private final Map<UUID, Double> lastNegativeDiscrepancies;
    private final Map<UUID, Double> negativeDiscrepanciesCumulative;

    public Speed() {
        super("speed", true, 0, 5, 0.99, 5000, "%player% failed movement speed, VL: %vl%", null);
        prevSpeed = new HashMap<>();
        discrepancies = new HashMap<>();
        noMovesMap = new HashMap<>();
        lastNegativeDiscrepancies = new HashMap<>();
        negativeDiscrepanciesCumulative = new HashMap<>();
        DISCREPANCY_THRESHOLD = (double) customSetting("discrepancyThreshold", "", 0.1D);
        VL_FAIL_DISCREPANCY_FACTOR = (double) customSetting("vlFailDiscrepancyFactor", "", 10D);
        RESET_DISCREPANCY_ON_FAIL = (boolean) customSetting("resetDiscrepancyOnFail", "", true);
        DEBUG = (boolean) customSetting("debug", "", false);
    }

    @Override
    protected void check(MoveEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();

        double lastSpeed;
        double speed;
        if(event.isUpdatePos()) {
            lastSpeed = prevSpeed.getOrDefault(p.getUniqueId(), 0D);
            speed = MathPlus.distance2d(event.getTo().getX() - event.getFrom().getX(), event.getTo().getZ() - event.getFrom().getZ());
        }
        else {
            speed = prevSpeed.getOrDefault(p.getUniqueId(), 0D) - (lastNegativeDiscrepancies.getOrDefault(p.getUniqueId(), 0D) + EPSILON);
            lastSpeed = speed;
        }

        int noMoves = noMovesMap.getOrDefault(p.getUniqueId(), 0);
        if(event.isUpdatePos())
            noMoves = 0;
        else
            noMoves++;

        boolean flying = (pp.hasFlyPending() && p.getAllowFlight()) || p.isFlying();
        boolean swimming = pp.isInLiquid(); //this needs improvement

        float friction = event.getFriction();
        float maxForce = event.getMaxExpectedInputForce();

        //It's the previous location. Block collision is done after moving the entity.
        Set<Material> touchedBlocks = WrappedEntity.getWrappedEntity(p).getCollisionBox(event.getFrom().toVector()).getMaterials(pp.getWorld());

        //handle any pending knockbacks
        if(event.hasAcceptedKnockback()) {
            prepareNextMove(p.getUniqueId(), noMoves, speed, touchedBlocks);
            return;
        }

        double lastSpeedCompare = lastSpeed;
        //Handle other things in the game that multiply velocity.
        //These affect velocity first (coincidentally, these are all multiplication operations)
        double handleMultipliers = 1;
        if(event.hasHitSlowdown())
            handleMultipliers *= 0.6;
        if(touchedBlocks.contains(Material.SOUL_SAND))
            handleMultipliers *= 0.4;
        if(touchedBlocks.contains(Material.WEB))
            handleMultipliers *= 0.25;
        if(Hawk.getServerVersion() > 7 && touchedBlocks.contains(Material.SLIME_BLOCK)) {
            //TODO I believe webs affect this mot Y too.
            if (Math.abs(pp.getVelocity().getY()) < 0.1 && !pp.isSneaking()) {
                handleMultipliers *= 0.4 + Math.abs(pp.getVelocity().getY()) * 0.2;
            }
        }
        //Handle other things in the game that add to velocity.
        //These affect velocity later (coincidentally, these are all addition operations)
        double handleAdders = 0;
        if(pp.isSprinting() && event.isJump()) {
            handleAdders += 0.2;
        }
        //Finally, the expected speed calculation
        double expected = friction * lastSpeedCompare * handleMultipliers + (maxForce + handleAdders + EPSILON);

        Discrepancy discrepancy;
        //LIQUID
        if(swimming && !flying) {

            Vector move = new Vector(event.getTo().getX() - event.getFrom().getX(), 0, event.getTo().getZ() - event.getFrom().getZ());
            Vector waterForce = event.getWaterFlowForce().clone().setY(0).normalize().multiply(Physics.WATER_FLOW_FORCE_MULTIPLIER);
            double waterForceLength = waterForce.length();
            //you can just normalize them and do a dot product. should be faster.
            double computedForce = MathPlus.cos((float)MathPlus.angle(move, waterForce)) * waterForceLength;

            computedForce += 0.003; //add epsilon to allow room for error

            if(Double.isNaN(computedForce)) {
                computedForce = waterForceLength;
                //wtf how can this still be NaN?
                if(Double.isNaN(computedForce)) {
                    computedForce = 0;
                }
            }

            discrepancy = waterMapping(lastSpeed, speed, computedForce);
        }
        else {
            discrepancy = new Discrepancy(expected, speed);
        }

        //Client told server that it updated its position. Checking time.
        if (event.isUpdatePos()) {
            double haltDistanceExpected = negativeDiscrepanciesCumulative.getOrDefault(p.getUniqueId(), 0D);
            lastNegativeDiscrepancies.put(p.getUniqueId(), 0D);
            if(discrepancy.value < 0 || speed > haltDistanceExpected)
                discrepancies.put(p.getUniqueId(), Math.max(discrepancies.getOrDefault(p.getUniqueId(), 0D) + discrepancy.value, 0));
            double totalDiscrepancy = discrepancies.getOrDefault(p.getUniqueId(), 0D);

            if(DEBUG) {
                p.sendMessage((totalDiscrepancy > DISCREPANCY_THRESHOLD ? ChatColor.RED : ChatColor.GREEN) + "" + totalDiscrepancy);
            }

            if(discrepancy.value > 0 && totalDiscrepancy > DISCREPANCY_THRESHOLD) {
                punishAndTryRubberband(pp, discrepancy.value * VL_FAIL_DISCREPANCY_FACTOR, event, p.getLocation());
                if(RESET_DISCREPANCY_ON_FAIL)
                    discrepancies.put(p.getUniqueId(), 0D);
            }
            else {
                reward(pp);
            }

            lastNegativeDiscrepancies.put(p.getUniqueId(), 0D);
            negativeDiscrepanciesCumulative.put(p.getUniqueId(), 0D);
        }
        //Client sent a flying packet, but didn't update position.
        //The move might have not been significant enough, so we need
        //to prepare for when the client decides it's time to update
        //position.
        else if(noMoves <= MAX_NO_MOVES){
            lastNegativeDiscrepancies.put(p.getUniqueId(), discrepancy.value);
            negativeDiscrepanciesCumulative.put(p.getUniqueId(), negativeDiscrepanciesCumulative.getOrDefault(p.getUniqueId(), 0D) + speed);
        }

        prepareNextMove(p.getUniqueId(), noMoves, speed, touchedBlocks);
    }

    private void prepareNextMove(UUID uuid, int noMoves, double currentSpeed, Set<Material> touchedBlocks) {
        if(touchedBlocks.contains(Material.WEB)) {
            currentSpeed = 0;
        }
        prevSpeed.put(uuid, currentSpeed);
        noMovesMap.put(uuid, noMoves);
    }

    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        prevSpeed.remove(uuid);
        discrepancies.remove(uuid);
        noMovesMap.remove(uuid);
        lastNegativeDiscrepancies.remove(uuid);
        negativeDiscrepanciesCumulative.remove(uuid);
    }

    //speed potions do not affect swimming
    private Discrepancy waterMapping(double lastSpeed, double currentSpeed, double waterFlowForce) {
        double expected = 0.800001 * lastSpeed + 0.020001 + waterFlowForce;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy lavaMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.500001 * lastSpeed + 0.020001;
        return new Discrepancy(expected, currentSpeed);
    }

    private class Discrepancy {

        double value;

        Discrepancy(double expectedSpeed, double currentSpeed) {
            value = currentSpeed - expectedSpeed;
        }

    }
}
