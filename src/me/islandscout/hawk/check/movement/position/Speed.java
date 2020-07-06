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
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.HawkEventListener;
import me.islandscout.hawk.event.ItemSwitchEvent;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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
    //if the move doesn't have a deltaPos due to insignificance.
    private static final double MAX_NO_MOVE_DISTANCE = 0.03;
    private final double DISCREPANCY_THRESHOLD;
    private final double VL_FAIL_DISCREPANCY_FACTOR;
    private final boolean RESET_DISCREPANCY_ON_FAIL;
    private final int RELEASE_ITEM_OVER_VL;
    private final boolean DEBUG;

    private final Map<UUID, Double> prevSpeed;
    private final Map<UUID, Double> discrepancies;
    private final Map<UUID, Integer> noMovesMap;
    private final Map<UUID, Double> lastNegativeDiscrepancies;
    private final Map<UUID, Double> negativeDiscrepanciesCumulative;
    private final Map<UUID, Integer> slotSwitchQuirkTicksMap; //I hate this game.
    private final Map<UUID, Long> lastSlotSwitchTickMap;

    public Speed() {
        super("speed", true, 0, 5, 0.99, 5000, "%player% failed movement speed, VL: %vl%", null);
        prevSpeed = new HashMap<>();
        discrepancies = new HashMap<>();
        noMovesMap = new HashMap<>();
        lastNegativeDiscrepancies = new HashMap<>();
        negativeDiscrepanciesCumulative = new HashMap<>();
        slotSwitchQuirkTicksMap = new HashMap<>();
        lastSlotSwitchTickMap = new HashMap<>();
        DISCREPANCY_THRESHOLD = (double) customSetting("discrepancyThreshold", "", 0.1D);
        VL_FAIL_DISCREPANCY_FACTOR = (double) customSetting("vlFailDiscrepancyFactor", "", 10D);
        RESET_DISCREPANCY_ON_FAIL = (boolean) customSetting("resetDiscrepancyOnFail", "", true);
        RELEASE_ITEM_OVER_VL = (int) customSetting("releaseItemOverVl", "", 4);
        DEBUG = (boolean) customSetting("debug", "", false);
    }

    @Override
    protected void check(MoveEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();

        int noMoves = noMovesMap.getOrDefault(p.getUniqueId(), 0);
        double lastSpeed;
        double speed;
        if(event.isUpdatePos()) {
            lastSpeed = prevSpeed.getOrDefault(p.getUniqueId(), 0D);
            if(noMoves > 0) {
                //Players don't update position unless they've moved at least 0.03 blocks (or if 20 ticks passed)
                lastSpeed = Math.min(lastSpeed, MAX_NO_MOVE_DISTANCE);
            }
            speed = MathPlus.distance2d(event.getTo().getX() - event.getFrom().getX(), event.getTo().getZ() - event.getFrom().getZ());
        }
        else {
            speed = prevSpeed.getOrDefault(p.getUniqueId(), 0D) - (lastNegativeDiscrepancies.getOrDefault(p.getUniqueId(), 0D) + EPSILON);
            lastSpeed = speed;
        }

        if(event.isUpdatePos())
            noMoves = 0;
        else
            noMoves++;

        boolean flying = pp.isFlying();
        boolean swimming = pp.isInWater(); //this needs improvement
        //it is possible that the player has pressed the jump key even if this move is "stepped" (only while sprinting)
        boolean jump = event.isJump() || (event.isStep() && pp.isOnGround() && pp.isSprinting());

        float friction = event.getFriction();
        float maxForce = event.getMaxExpectedInputForce();
        float maxForceNoItemUse = event.getMaxExpectedInputForceNoItemUse();

        //It represents the previous location. Block collision is done after moving the entity.
        Set<Material> touchedBlocks = WrappedEntity.getWrappedEntity(p).getCollisionBox(event.getFrom().toVector()).getMaterials(pp.getWorld());

        //handle any pending knockbacks
        if(event.hasAcceptedKnockback()) {
            prepareNextMove(pp, noMoves, speed, touchedBlocks);
            return;
        }

        double lastSpeedCompare = lastSpeed;
        //Handle other things in the game that multiply velocity.
        //These affect velocity first (coincidentally, these are all multiplication operations)
        double handleMultipliers = 1;
        if(event.hasHitSlowdown())
            handleMultipliers *= 0.6;
        if(touchedBlocks.contains(Material.SOUL_SAND) && !flying)
            handleMultipliers *= 0.4;
        if(touchedBlocks.contains(Material.WEB) && !flying)
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
        if(pp.isSprinting() && jump) {
            handleAdders += 0.2;
        }

        //Finally, the expected speed calculation. There's another "expected" because of a dumb quirk when switching slots while using an item.
        //Optimize later.
        double expected = friction * lastSpeedCompare * handleMultipliers + (maxForce + handleAdders + EPSILON);
        double expectedNoItemUse = friction * lastSpeedCompare * handleMultipliers + (maxForceNoItemUse + handleAdders + EPSILON);

        //I hate this game. Handle slot switching quirk. The slot switch packet is delayed by a tick.
        //This sort of "event listening" must be done because Hawk's MoveEvents can be skipped under certain conditions.
        long slotSwitchTick = pp.getLastSlotSwitchTick();
        boolean switchedSlot = slotSwitchTick != lastSlotSwitchTickMap.getOrDefault(p.getUniqueId(), slotSwitchTick);
        int slotSwitchQuirkTicks = slotSwitchQuirkTicksMap.getOrDefault(p.getUniqueId(), 0);
        if(switchedSlot) {
            slotSwitchQuirkTicks = 0;
        }
        lastSlotSwitchTickMap.put(p.getUniqueId(), slotSwitchTick);

        //Calculate discrepancy.
        Discrepancy discrepancy; //This is the one that will be used for final comparison. It is the value of either discrepancyBase or discrepancyNoItemUse.
        Discrepancy discrepancyBase; //Used for pretty much 99.99% of moves.
        Discrepancy discrepancyNoItemUse; //Similar to discrepancyBase, but doesn't consider item-use slowdown.
        //LIQUID
        if(swimming && !flying) {

            Vector move = new Vector(event.getTo().getX() - event.getFrom().getX(), 0, event.getTo().getZ() - event.getFrom().getZ());
            Vector waterForce = event.getWaterFlowForce().clone().setY(0);
            double waterForceLength = waterForce.length();
            double moveLength = move.length();
            double computedForce = moveLength == 0 ? waterForceLength : (move.dot(waterForce) / moveLength);

            double depthStriderModifier = 0;
            if(Hawk.getServerVersion() == 8) {
                depthStriderModifier = p.getInventory().getBoots().getEnchantmentLevel(Enchantment.DEPTH_STRIDER);
            }

            computedForce += 0.003; //add epsilon to allow room for error

            discrepancy = waterMapping(lastSpeed, speed, computedForce);
        }
        else {
            discrepancyBase = new Discrepancy(expected, speed);
            discrepancyNoItemUse = new Discrepancy(expectedNoItemUse, speed);
            if((pp.isBlocking() || pp.isConsumingItem() || pp.isPullingBow()) && Math.abs(discrepancyNoItemUse.value) < Math.abs(discrepancyBase.value)) {
                //This means that they're using an item, but they're moving at normal speed. This COULD be due to that
                //dumb item switch quirk, so we'll grant them only one extra tick. It resets when they switch their slot.
                if(slotSwitchQuirkTicks < 1) {
                    //Allow them to move at normal speed for 1 tick.
                    discrepancy = discrepancyNoItemUse;
                    slotSwitchQuirkTicks++;
                } else {
                    discrepancy = discrepancyBase;
                }
            } else {
                discrepancy = discrepancyBase;
            }
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

            if(discrepancy.value > 0 && totalDiscrepancy > DISCREPANCY_THRESHOLD && !event.isPossiblePistonPush()) {
                punishAndTryRubberband(pp, discrepancy.value * VL_FAIL_DISCREPANCY_FACTOR, event);
                if(RESET_DISCREPANCY_ON_FAIL)
                    discrepancies.put(p.getUniqueId(), 0D);
                if(RELEASE_ITEM_OVER_VL > -1 && (pp.isPullingBow() || pp.isConsumingItem() || pp.isBlocking()) && pp.getVL(this) > RELEASE_ITEM_OVER_VL)
                    pp.releaseItem();
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
        else {
            lastNegativeDiscrepancies.put(p.getUniqueId(), discrepancy.value);

            //TODO improve these two lines...
            double val = negativeDiscrepanciesCumulative.getOrDefault(p.getUniqueId(), 0D);
            negativeDiscrepanciesCumulative.put(p.getUniqueId(), Math.min(val + speed, MAX_NO_MOVE_DISTANCE + speed));
            //negativeDiscrepanciesCumulative.put(p.getUniqueId(), negativeDiscrepanciesCumulative.getOrDefault(p.getUniqueId(), 0D) + speed);
        }

        slotSwitchQuirkTicksMap.put(p.getUniqueId(), slotSwitchQuirkTicks);
        prepareNextMove(pp, noMoves, speed, touchedBlocks);
    }

    private void prepareNextMove(HawkPlayer pp, int noMoves, double currentSpeed, Set<Material> touchedBlocks) {
        if(touchedBlocks.contains(Material.WEB) && !pp.isFlying()) {
            currentSpeed = 0;
        }
        UUID uuid = pp.getUuid();
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
        slotSwitchQuirkTicksMap.remove(uuid);
        lastSlotSwitchTickMap.remove(uuid);
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
