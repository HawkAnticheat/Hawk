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

package me.islandscout.hawk.check.movement;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.event.bukkit.HawkPlayerAsyncVelocityChangeEvent;
import me.islandscout.hawk.util.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class Speed extends MovementCheck implements Listener {

    //I legit hate this game's movement

    //TODO Shrink this code
    //TODO False flag when landing on ice
    //TODO False flag with pistons

    //Basically, this check is doing, "if your previous speed was X then your current speed must not exceed f(X)"

    private static final double EPSILON = 0.000001;
    //Now what we can do is predict how far a player could have travelled
    //if the move doesn't have a deltaPos due to insignificance. It might reduce false-
    //positives, but it might make the check more vulnerable to abuse since players can
    //send a bunch of non-deltaPos moves, and then send one with a great deltaPos. To
    //other players, this may look like teleportation or some sort of lag switch.
    //Tolerance for client inconsistencies due to insignificance. Max no-update-pos ticks + 1
    private static final long MAX_TICKS_SINCE_POS_UPDATE = 9;
    private final double DISCREPANCY_THRESHOLD;
    private final boolean DEBUG;

    private final Set<UUID> prevMoveWasOnGround;
    private final Map<UUID, Double> prevSpeed;
    private final Map<UUID, Long> landingTick;
    private final Map<UUID, Long> sprintingJumpTick;
    private final Map<UUID, Double> discrepancies;
    private final Map<UUID, Long> lastTickOnGround;
    private final Map<UUID, Long> lastTickPosUpdate;
    private final Map<UUID, Double> lastNegativeDiscrepancies;
    private final Map<UUID, Double> negativeDiscrepanciesCumulative;
    private final Map<UUID, List<Pair<Double, Long>>> velocities; //launch velocities

    public Speed() {
        super("speed", "%player% failed movement speed, VL: %vl%");
        prevMoveWasOnGround = new HashSet<>();
        prevSpeed = new HashMap<>();
        landingTick = new HashMap<>();
        sprintingJumpTick = new HashMap<>();
        discrepancies = new HashMap<>();
        velocities = new HashMap<>();
        lastTickOnGround = new HashMap<>();
        lastTickPosUpdate = new HashMap<>();
        lastNegativeDiscrepancies = new HashMap<>();
        negativeDiscrepanciesCumulative = new HashMap<>();
        DISCREPANCY_THRESHOLD = (double) customSetting("discrepancyThreshold", "", 0.1D);
        DEBUG = (boolean) customSetting("debug", "", false);
    }

    @Override
    protected void check(MoveEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        long ticksSinceUpdatePos = pp.getCurrentTick() - lastTickPosUpdate.getOrDefault(p.getUniqueId(), pp.getCurrentTick() - 1); //will always be >= 1
        double lastSpeed;
        double speed;
        if(event.isUpdatePos()) {
            lastSpeed = prevSpeed.getOrDefault(p.getUniqueId(), 0D);
            speed = MathPlus.distance2d(event.getTo().getX() - event.getFrom().getX(), event.getTo().getZ() - event.getFrom().getZ());
        } else {
            speed = prevSpeed.getOrDefault(p.getUniqueId(), 0D) - (lastNegativeDiscrepancies.getOrDefault(p.getUniqueId(), 0D) + 0.000001);
            lastSpeed = speed;
        }
        boolean wasOnGround = prevMoveWasOnGround.contains(p.getUniqueId());
        //In theory, YES, you can abuse the on ground flag. However, that won't get you far.
        //FastFall and SmallHop should patch some NCP bypasses
        //This is one of the very few times I'll actually trust the client. What's worse: an insignificant bypass, or intolerable false flagging?
        boolean isOnGround = event.isOnGround();
        long ticksSinceLanding = pp.getCurrentTick() - landingTick.getOrDefault(p.getUniqueId(), Long.MIN_VALUE);
        long ticksSinceSprintJumping = pp.getCurrentTick() - sprintingJumpTick.getOrDefault(p.getUniqueId(), Long.MIN_VALUE);
        long ticksSinceOnGround = pp.getCurrentTick() - lastTickOnGround.getOrDefault(p.getUniqueId(), Long.MIN_VALUE);
        boolean flying = (pp.hasFlyPending() && p.getAllowFlight()) || p.isFlying();
        boolean up = event.getTo().getY() > event.getFrom().getY();
        boolean usingSomething = pp.isBlocking() || pp.isConsumingItem() || pp.isPullingBow();
        boolean sprinting = pp.isSprinting() && !pp.isSneaking() && !usingSomething;
        double walkMultiplier = 5 * p.getWalkSpeed() * speedEffectMultiplier(p); //TODO: account for latency
        double flyMultiplier = 10 * p.getFlySpeed();
        //TODO: support liquids, cobwebs, soulsand, etc...

        //handle any pending knockbacks
        if (velocities.containsKey(p.getUniqueId()) && velocities.get(p.getUniqueId()).size() > 0) {
            List<Pair<Double, Long>> kbs = velocities.get(p.getUniqueId());
            //pending knockbacks must be in order; get the first entry in the list.
            //if the first entry doesn't work (probably because they were fired on the same tick),
            //then work down the list until we find something
            int kbIndex;
            long currTime = System.currentTimeMillis();
            for (kbIndex = 0; kbIndex < kbs.size(); kbIndex++) {
                Pair<Double, Long> kb = kbs.get(kbIndex);
                if (currTime - kb.getValue() <= ServerUtils.getPing(p) + 200) {
                    //I'd suggest changing this epsilon based on the graph you showed on Discord
                    //Also, if you get knocked to a wall with great velocity, you'll flag
                    if (Math.abs(kb.getKey() - speed) < 0.15) {
                        kbs = kbs.subList(kbIndex + 1, kbs.size());
                        velocities.put(p.getUniqueId(), kbs);
                        prepareNextMove(wasOnGround, isOnGround, event, p.getUniqueId(), pp.getCurrentTick(), speed);
                        return;
                    }
                }
            }
        }

        SpeedType failed = null;
        Discrepancy discrepancy = new Discrepancy(0, 0);
        boolean checked = true;
        //LIQUID
        /*
        Location chkPos = event.getTo().clone();
        if(!flying && (AdjacentBlocks.blockAdjacentIsLiquid(chkPos) || AdjacentBlocks.blockAdjacentIsLiquid(chkPos.add(0, 1.8, 0)))) {
            //lava (goes first since it overrides water)
            if(AdjacentBlocks.matIsAdjacent(chkPos, Material.STATIONARY_LAVA, Material.LAVA) || AdjacentBlocks.matIsAdjacent(chkPos.add(0, -1.8, 0), Material.STATIONARY_LAVA, Material.LAVA)) {
                if(event.isUpdatePos()) {

                }
            }
            //water
            else {

            }
        }*/
        //LAND (instantaneous)
        if((isOnGround && ticksSinceLanding == 1) || (ticksSinceOnGround == 1 && ticksSinceLanding == 1 && !up)) {
            if(sprinting) {
                discrepancy = sprintLandingMapping(lastSpeed, speed, walkMultiplier);
                if(discrepancy.value > 0)
                    failed = SpeedType.LANDING_SPRINT;
            }
            else if(!sprinting) {
                discrepancy = walkLandingMapping(lastSpeed, speed, walkMultiplier, usingSomething);
                if(discrepancy.value > 0)
                    failed = SpeedType.LANDING_WALK;
            }
        }
        //GROUND
        else if((isOnGround && wasOnGround && ticksSinceLanding > 1) || (ticksSinceOnGround == 1 && !up && !isOnGround)) {
            if(pp.isSneaking()) {
                discrepancy = sneakGroundMapping(lastSpeed, speed, walkMultiplier, usingSomething);
                if(discrepancy.value > 0)
                    failed = SpeedType.SNEAK;
            }
            else if(!sprinting) {
                discrepancy = walkGroundMapping(lastSpeed, speed, walkMultiplier, usingSomething);
                if(discrepancy.value > 0)
                    failed = SpeedType.WALK;
            }
            else if(sprinting) {
                discrepancy = sprintGroundMapping(lastSpeed, speed, walkMultiplier);
                if(discrepancy.value > 0)
                    failed = SpeedType.SPRINT;
            }

        }
        //JUMP (instantaneous)
        else if(wasOnGround && !isOnGround && up) {
            //CONTINUE
            if(ticksSinceLanding == 1) {
                //SNEAK
                if (pp.isSneaking()) {
                    discrepancy = sneakJumpContinueMapping(lastSpeed, speed, walkMultiplier, usingSomething);
                    if(discrepancy.value > 0)
                        failed = SpeedType.SNEAK_JUMPING_CONTINUE;
                }
                //WALK
                else if (!sprinting) {
                    discrepancy = walkJumpContinueMapping(lastSpeed, speed, walkMultiplier, usingSomething);
                    if(discrepancy.value > 0)
                        failed = SpeedType.WALK_JUMPING_CONTINUE;
                }
                //SPRINT
                else if (sprinting) {
                    discrepancy = sprintJumpContinueMapping(lastSpeed, speed, walkMultiplier);
                    if(discrepancy.value > 0)
                        failed = SpeedType.SPRINT_JUMPING_CONTINUE;
                    sprintingJumpTick.put(p.getUniqueId(), pp.getCurrentTick());
                }
            }
            //START
            else {
                //SNEAK
                if (pp.isSneaking()) {
                    discrepancy = sneakJumpStartMapping(lastSpeed, speed, walkMultiplier, usingSomething);
                    if(discrepancy.value > 0)
                        failed = SpeedType.SNEAK_JUMPING_START;
                }
                //WALK
                else if (!sprinting) {
                    discrepancy = walkJumpStartMapping(lastSpeed, speed, walkMultiplier, usingSomething);
                    if(discrepancy.value > 0)
                        failed = SpeedType.WALK_JUMPING_START;
                }
                //SPRINT
                else if (sprinting) {
                    discrepancy = sprintJumpStartMapping(lastSpeed, speed, walkMultiplier);
                    if(discrepancy.value > 0)
                        failed = SpeedType.SPRINT_JUMPING_START;
                    sprintingJumpTick.put(p.getUniqueId(), pp.getCurrentTick());
                }
            }
        }
        //SPRINT_JUMP_POST (instantaneous)
        else if(!wasOnGround && !isOnGround && ticksSinceSprintJumping == 1) {
            Block b = ServerUtils.getBlockAsync(event.getFrom().clone().add(0, -1, 0));
            if(b != null && (b.getType() == Material.ICE || b.getType() == Material.PACKED_ICE)) {
                discrepancy = jumpPostIceMapping(lastSpeed, speed);
            }
            else {
                discrepancy = jumpPostMapping(lastSpeed, speed);
            }
            if(discrepancy.value > 0)
                failed = SpeedType.SPRINT_JUMP_POST;
        }
        //FLYING
        else if((pp.hasFlyPending() && p.getAllowFlight()) || p.isFlying()) {
            discrepancy = flyMapping(lastSpeed, speed, pp.isSneaking(), sprinting, flyMultiplier, usingSomething);
            if(discrepancy.value > 0)
                failed = SpeedType.FLYING;
        }
        //AIR
        else if((!((pp.hasFlyPending() && p.getAllowFlight()) || p.isFlying()) && !wasOnGround) || (!up && !isOnGround)) {
            if(pp.isSneaking()) {
                discrepancy = sneakAirMapping(lastSpeed, speed, usingSomething);
                if(discrepancy.value > 0)
                    failed = SpeedType.AIR_SNEAK;
            }
            else if(sprinting) {
                discrepancy = sprintAirMapping(lastSpeed, speed);
                if(discrepancy.value > 0)
                    failed = SpeedType.AIR_SPRINT;
            }
            else if(!sprinting) {
                discrepancy = walkAirMapping(lastSpeed, speed, usingSomething);
                if(discrepancy.value > 0)
                    failed = SpeedType.AIR_WALK;
            }
        }
        else {
            checked = false;
        }

        //Client told server that it updated its position
        if (event.isUpdatePos()) {
            double haltDistanceExpected = negativeDiscrepanciesCumulative.getOrDefault(p.getUniqueId(), 0D);
            lastNegativeDiscrepancies.put(p.getUniqueId(), 0D);
            discrepancies.put(p.getUniqueId(), Math.max(discrepancies.getOrDefault(p.getUniqueId(), 0D) + discrepancy.value, 0));
            double totalDiscrepancy = discrepancies.get(p.getUniqueId());

            if(DEBUG) {
                if(!checked)
                    p.sendMessage(ChatColor.RED + "ERROR: A move was not processed by the speed check. Please report this issue to the Discord server! Build: " + Hawk.BUILD_NAME);
                p.sendMessage((totalDiscrepancy > DISCREPANCY_THRESHOLD ? ChatColor.RED : ChatColor.GREEN) + "" + totalDiscrepancy);
            }

            if(failed != null) {
                if(totalDiscrepancy > DISCREPANCY_THRESHOLD && speed > haltDistanceExpected) {
                    punishAndTryRubberband(pp, event, p.getLocation());
                    if(DEBUG)
                        p.sendMessage(ChatColor.RED + failed.toString());
                }

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
        else if(ticksSinceUpdatePos <= MAX_TICKS_SINCE_POS_UPDATE){
            lastNegativeDiscrepancies.put(p.getUniqueId(), discrepancy.value);
            negativeDiscrepanciesCumulative.put(p.getUniqueId(), negativeDiscrepanciesCumulative.getOrDefault(p.getUniqueId(), 0D) + speed);
        }

        prepareNextMove(wasOnGround, isOnGround, event, p.getUniqueId(), pp.getCurrentTick(), speed);
    }

    private void prepareNextMove(boolean wasOnGround, boolean isOnGround, MoveEvent event, UUID uuid, long currentTick, double currentSpeed) {
        if(isOnGround) {
            prevMoveWasOnGround.add(uuid);
            lastTickOnGround.put(uuid, currentTick);
        }
        else
            prevMoveWasOnGround.remove(uuid);
        prevSpeed.put(uuid, currentSpeed);

        //player touched the ground
        if(!wasOnGround && event.isOnGround()) {
            landingTick.put(uuid, currentTick);
        }

        if(event.isUpdatePos())
            lastTickPosUpdate.put(uuid, currentTick);
    }

    private double speedEffectMultiplier(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!effect.getType().equals(PotionEffectType.SPEED))
                continue;
            double level = effect.getAmplifier() + 1;
            level = 1 + (level * 0.2);
            return level;
        }
        return 1;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVelocity(HawkPlayerAsyncVelocityChangeEvent e) {
        if(e.isAdditive())
            return;
        UUID uuid = e.getPlayer().getUniqueId();
        Vector vector = e.getVelocity();

        Vector horizVelocity = new Vector(vector.getX(), 0, vector.getZ());
        double magnitude = horizVelocity.length() + 0.018; //add epsilon for precision errors
        List<Pair<Double, Long>> kbs = velocities.getOrDefault(uuid, new ArrayList<>());
        kbs.add(new Pair<>(magnitude, System.currentTimeMillis()));
        velocities.put(uuid, kbs);
    }

    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        prevMoveWasOnGround.remove(uuid);
        prevSpeed.remove(uuid);
        landingTick.remove(uuid);
        sprintingJumpTick.remove(uuid);
        discrepancies.remove(uuid);
        velocities.remove(uuid);
    }

    //these functions must be extremely accurate to support high level speed potions
    //added epsilon of 0.000001 and truncated if necessary
    private Discrepancy walkAirMapping(double lastSpeed, double currentSpeed, boolean usingSomething) {
        double initSpeed = (usingSomething ? 0.02 / 3.607687 : 0.02) + EPSILON;
        double expected = 0.910001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy jumpPostMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.546001 * lastSpeed + 0.026001;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy jumpPostIceMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.891941 * lastSpeed + 0.025401;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sprintJumpStartMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double expected = 0.546001 * lastSpeed + (0.3274 * (1 + ((speedMultiplier - 1) * 0.389127))); //Don't question it.
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sneakJumpStartMapping(double lastSpeed, double currentSpeed, double multiplier, boolean usingSomething) {
        double initSpeed = (usingSomething ? 0.041578 / 5 : 0.041578);
        double expected = 0.546 * lastSpeed + (initSpeed + ((multiplier - 1) * initSpeed)) + EPSILON;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy walkJumpStartMapping(double lastSpeed, double currentSpeed, double multiplier, boolean usingSomething) {
        double initSpeed = (usingSomething ? 0.1 / 3.607687 : 0.1);
        double expected = 0.546 * lastSpeed + (initSpeed + ((multiplier - 1) * initSpeed)) + EPSILON;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sprintJumpContinueMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double expected = 0.910001 * lastSpeed + (0.3274 * (1 + ((speedMultiplier - 1) * 0.389127))); //Don't question it.
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sneakJumpContinueMapping(double lastSpeed, double currentSpeed, double multiplier, boolean usingSomething) {
        double initSpeed = (usingSomething ? 0.041578 / 5 : 0.041578);
        double expected = 0.91 * lastSpeed + (initSpeed + ((multiplier - 1) * initSpeed)) + EPSILON;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy walkJumpContinueMapping(double lastSpeed, double currentSpeed, double multiplier, boolean usingSomething) {
        double initSpeed = (usingSomething ? 0.1 / 3.607687 : 0.1);
        double expected = 0.91 * lastSpeed + (initSpeed + ((multiplier - 1) * initSpeed)) + EPSILON;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sprintGroundMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double initSpeed = 0.13 * speedMultiplier + EPSILON;
        double expected = 0.546001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy walkGroundMapping(double lastSpeed, double currentSpeed, double speedMultiplier, boolean blocking) {
        double initSpeed = (blocking ? 0.1 / 3.607687 : 0.1) * speedMultiplier + EPSILON;
        double expected = 0.546001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    //TODO: fix speed multiplier
    private Discrepancy sneakGroundMapping(double lastSpeed, double currentSpeed, double speedMultiplier, boolean blocking) {
        double initSpeed = (blocking ? 0.00588 : 0.041560) * speedMultiplier + EPSILON;
        double expected = 0.546001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    //speed potions do not affect swimming
    private Discrepancy waterMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.800001 * lastSpeed + 0.020001;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy lavaMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.500001 * lastSpeed + 0.020001;
        return new Discrepancy(expected, currentSpeed);
    }

    //speed potions do not affect air movement
    private Discrepancy sprintAirMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.910001 * lastSpeed + 0.026001;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sneakAirMapping(double lastSpeed, double currentSpeed, boolean usingSomething) {
        double initSpeed = (usingSomething ? 0.008317 / 5 : 0.008317) + EPSILON;
        double expected = 0.910001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sprintLandingMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double initSpeed = 0.13 * speedMultiplier + EPSILON;
        double expected = 0.910001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy walkLandingMapping(double lastSpeed, double currentSpeed, double speedMultiplier, boolean usingSomething) {
        double initSpeed = (usingSomething ? 0.1 / 3.607687 : 0.1) * speedMultiplier + EPSILON;
        double expected = 0.910001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy flyMapping(double lastSpeed, double currentSpeed, boolean sneaking, boolean sprinting, double speedMultiplier, boolean usingSomething) {
        //I really don't like this because these sneaking and sprinting variables depend on the definitions in the check() method.
        //If you change those, this may break.
        double baseSpeed = (sneaking ? 0.02078894935 : (sprinting ? 0.1 : 0.05));
        double initSpeed = (usingSomething ? baseSpeed / 3.607687 : baseSpeed) * speedMultiplier + EPSILON;
        double expected = 0.910001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    private class Discrepancy {

        double value;

        Discrepancy(double expectedSpeed, double currentSpeed) {
            value = currentSpeed - expectedSpeed;
        }

    }

    private enum SpeedType {
        WALK,
        SPRINT,
        SPRINT_JUMPING_CONTINUE,
        SPRINT_JUMPING_START,
        SNEAK_JUMPING_CONTINUE,
        SNEAK_JUMPING_START,
        WALK_JUMPING_CONTINUE,
        WALK_JUMPING_START,
        SPRINT_JUMP_POST,
        AIR_WALK,
        AIR_SNEAK,
        AIR_SPRINT,
        SNEAK,
        LANDING_WALK,
        LANDING_SPRINT,
        FLYING
    }
}
