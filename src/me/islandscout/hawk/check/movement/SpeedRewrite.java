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
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.Debug;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SpeedRewrite extends MovementCheck {

    //TODO: Rename to VelocityMagnitude
    //Suggestion: A good false-positive filter is to accumulate the total discrepancies between client & server
    //and if it exceeds a threshold after some time, then rubberband/flag.
    //In the config, have it like this:
    //  discrepancyThreshold: 0.1
    //  debug: false (show current discrepancy (color coded: green is under or equal thres, red is above thres))

    //Basically, this check is doing, "if your previous speed was X then your current speed must not exceed f(X)"

    private static final double EPSILON = 0.000001;

    private final Set<UUID> prevMoveWasOnGround;
    private final Map<UUID, Double> prevSpeed;
    private final Map<UUID, Long> landingTick;
    private final Map<UUID, Long> sprintingJumpTick;
    private final Map<UUID, Double> discrepancies;

    public SpeedRewrite() {
        super("speed", "%player% failed movement speed, VL: %vl%");
        prevMoveWasOnGround = new HashSet<>();
        prevSpeed = new HashMap<>();
        landingTick = new HashMap<>();
        sprintingJumpTick = new HashMap<>();
        discrepancies = new HashMap<>();
    }

    @Override
    protected void check(PositionEvent event) {
        //Suggestion: Now what you could do is predict how far a player could have travelled
        //if the move doesn't have a deltaPos due to insignificance. It might make the check
        //more accurate, but more vulnerable to abuse since players can send a bunch of non-
        //deltaPos moves, and then send one with a great deltaPos. To other players, this
        //may look like teleportation.
        if (!event.hasDeltaPos())
            return;
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        double speed = Math.sqrt(Math.pow(event.getTo().getX() - event.getFrom().getX(), 2) + Math.pow(event.getTo().getZ() - event.getFrom().getZ(), 2));
        double lastSpeed = prevSpeed.getOrDefault(p.getUniqueId(), 0D);
        boolean wasOnGround = prevMoveWasOnGround.contains(p.getUniqueId());
        //In theory, YES, you can abuse the on ground flag. However, the GroundSpoof check has the job of taking care of you.
        //This is one of the very few times I'll actually trust the client. What's worse: an insignificant bypass, or intolerable false flagging?
        boolean isOnGround = event.isOnGround();
        long ticksSinceLanding = pp.getCurrentTick() - landingTick.getOrDefault(p.getUniqueId(), Long.MIN_VALUE);
        long ticksSinceSprintJumping = pp.getCurrentTick() - sprintingJumpTick.getOrDefault(p.getUniqueId(), Long.MIN_VALUE);
        boolean up = event.getTo().getY() > event.getFrom().getY();
        double multiplier = 5 * p.getWalkSpeed() * speedEffectMultiplier(p); //TODO: account for latency
        //TODO: support fly speeds
        //TODO: support liquids, cobwebs, soulsand, etc...

        SpeedType failed = null;
        Discrepancy discrepancy = new Discrepancy(0, 0);
        //LAND (instantaneous)
        if(isOnGround && ticksSinceLanding == 1) {
            if(pp.isSprinting()) {
                discrepancy = sprintLandingMapping(lastSpeed, speed, multiplier);
                if(discrepancy.value > 0)
                    failed = SpeedType.LANDING_SPRINT;
            }
            else if(!pp.isSprinting()) {
                discrepancy = walkLandingMapping(lastSpeed, speed, multiplier);
                if(discrepancy.value > 0)
                    failed = SpeedType.LANDING_WALK;
            }
        }
        //GROUND
        else if(isOnGround && wasOnGround && ticksSinceLanding > 1) {
            if(pp.isSneaking()) {
                discrepancy = sneakGroundMapping(lastSpeed, speed, multiplier);
                if(discrepancy.value > 0)
                    failed = SpeedType.SNEAK;
            }
            else if(!pp.isSprinting()) {
                discrepancy = walkGroundMapping(lastSpeed, speed, multiplier);
                if(discrepancy.value > 0)
                    failed = SpeedType.WALK;
            }
            else if(pp.isSprinting()) {
                discrepancy = sprintGroundMapping(lastSpeed, speed, multiplier);
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
                    discrepancy = sneakJumpContinueMapping(lastSpeed, speed, multiplier);
                    if(discrepancy.value > 0)
                        failed = SpeedType.SNEAK_JUMPING_CONTINUE;
                }
                //WALK
                else if (!pp.isSprinting()) {
                    discrepancy = walkJumpContinueMapping(lastSpeed, speed, multiplier);
                    if(discrepancy.value > 0)
                        failed = SpeedType.WALK_JUMPING_CONTINUE;
                }
                //SPRINT
                else if (pp.isSprinting()) {
                    discrepancy = sprintJumpContinueMapping(lastSpeed, speed, multiplier);
                    if(discrepancy.value > 0)
                        failed = SpeedType.SPRINT_JUMPING_CONTINUE;
                    sprintingJumpTick.put(p.getUniqueId(), pp.getCurrentTick());
                }
            }
            //START
            else {
                //SNEAK
                if (pp.isSneaking()) {
                    discrepancy = sneakJumpStartMapping(lastSpeed, speed, multiplier);
                    if(discrepancy.value > 0)
                        failed = SpeedType.SNEAK_JUMPING_START;
                }
                //WALK
                else if (!pp.isSprinting()) {
                    discrepancy = walkJumpStartMapping(lastSpeed, speed, multiplier);
                    if(discrepancy.value > 0)
                        failed = SpeedType.WALK_JUMPING_START;
                }
                //SPRINT
                else if (pp.isSprinting()) {
                    discrepancy = sprintJumpStartMapping(lastSpeed, speed, multiplier);
                    if(discrepancy.value > 0)
                        failed = SpeedType.SPRINT_JUMPING_START;
                    sprintingJumpTick.put(p.getUniqueId(), pp.getCurrentTick());
                }
            }
        }
        //SPRINT_JUMP_POST (instantaneous)
        else if(!wasOnGround && !isOnGround && ticksSinceSprintJumping == 1) {
            discrepancy = jumpPostMapping(lastSpeed, speed);
            if(discrepancy.value > 0)
                failed = SpeedType.SPRINT_JUMP_POST;
        }
        //FLYING
        else if((pp.hasFlyPending() && p.getAllowFlight()) || p.isFlying()) {
            discrepancy = flyMapping(lastSpeed, speed);
            if(discrepancy.value > 0)
                failed = SpeedType.FLYING;
        }
        //AIR
        else if((!((pp.hasFlyPending() && p.getAllowFlight()) || p.isFlying()) && !wasOnGround) || (!up && !isOnGround)) {
            if(pp.isSneaking()) {
                discrepancy = sneakAirMapping(lastSpeed, speed);
                if(discrepancy.value > 0)
                    failed = SpeedType.AIR_SNEAK;
            }
            else if(pp.isSprinting()) {
                discrepancy = sprintAirMapping(lastSpeed, speed);
                if(discrepancy.value > 0)
                    failed = SpeedType.AIR_SPRINT;
            }
            else if(!pp.isSprinting()) {
                discrepancy = walkAirMapping(lastSpeed, speed);
                if(discrepancy.value > 0)
                    failed = SpeedType.AIR_WALK;
            }
        }
        else {
            Debug.broadcastMessage(ChatColor.RED + "ERROR: Severe issue occurred in new speed check. Please report to discord server. Build: " + Hawk.BUILD_NAME);
        }
        discrepancies.put(p.getUniqueId(), Math.max(discrepancies.getOrDefault(p.getUniqueId(), 0D) + discrepancy.value, 0));
        double totalDiscrepancy = discrepancies.get(p.getUniqueId());

        if(failed != null && totalDiscrepancy > 0.1) {
            punishAndTryRubberband(pp, event, p.getLocation());
        }


        if(isOnGround)
            prevMoveWasOnGround.add(p.getUniqueId());
        else
            prevMoveWasOnGround.remove(p.getUniqueId());
        prevSpeed.put(p.getUniqueId(), speed);

        //player touched the ground
        if(!wasOnGround && event.isOnGround()) {
            landingTick.put(p.getUniqueId(), pp.getCurrentTick());
        }
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

    //these functions must be extremely accurate to support high level speed potions
    //added epsilon of 0.000001 and truncated if necessary
    private Discrepancy walkAirMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.910001 * lastSpeed + 0.020001;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy jumpPostMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.546001 * lastSpeed + 0.026001;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sprintJumpStartMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double expected = 0.546001 * lastSpeed + (0.3274 * (1 + ((speedMultiplier - 1) * 0.389127))); //Don't question it.
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sneakJumpStartMapping(double lastSpeed, double currentSpeed, double multiplier) {
        double expected = 0.546 * lastSpeed + (0.041578 + ((multiplier - 1) * 0.041578)) + EPSILON;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy walkJumpStartMapping(double lastSpeed, double currentSpeed, double multiplier) {
        double expected = 0.546 * lastSpeed + (0.1 + ((multiplier - 1) * 0.1)) + EPSILON;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sprintJumpContinueMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double expected = 0.910001 * lastSpeed + (0.3274 * (1 + ((speedMultiplier - 1) * 0.389127))); //Don't question it.
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sneakJumpContinueMapping(double lastSpeed, double currentSpeed, double multiplier) {
        double expected = 0.91 * lastSpeed + (0.041578 + ((multiplier - 1) * 0.041578)) + EPSILON;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy walkJumpContinueMapping(double lastSpeed, double currentSpeed, double multiplier) {
        double expected = 0.91 * lastSpeed + (0.1 + ((multiplier - 1) * 0.1)) + EPSILON;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sprintGroundMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double initSpeed = 0.13 * speedMultiplier + EPSILON;
        double expected;
        if(lastSpeed >= 0.13)
            expected = 0.546001 * lastSpeed + initSpeed;
        else
            expected = 0.2;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy walkGroundMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double initSpeed = 0.1 * speedMultiplier + EPSILON;
        double expected;
        if(lastSpeed >= 0.1)
            expected = 0.546001 * lastSpeed + initSpeed;
        else
            expected = 0.16;
        return new Discrepancy(expected, currentSpeed);
    }

    //TODO: fix speed multiplier
    private Discrepancy sneakGroundMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double initSpeed = 0.041560 * speedMultiplier + EPSILON;
        double expected;
        if(lastSpeed >= 0.1)
            expected = 0.546001 * lastSpeed + initSpeed;
        else
            expected = 0.096161;
        return new Discrepancy(expected, currentSpeed);
    }

    //speed potions do not affect swimming
    private Discrepancy waterMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.800001 * lastSpeed + 0.020001;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy lavaMapping(double lastSpeed, double currentSpeed) {
        double expected = 0;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy groundWaterMapping(double lastSpeed, double currentSpeed) {
        double expected = 0;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy groundLavaMapping(double lastSpeed, double currentSpeed) {
        double expected = 0;
        return new Discrepancy(expected, currentSpeed);
    }

    //speed potions do not affect air movement
    private Discrepancy sprintAirMapping(double lastSpeed, double currentSpeed) {
        double expected;
        if(lastSpeed >= 0.1)
            expected = 0.910001 * lastSpeed + 0.026001;
        else
            expected = 0.16;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sneakAirMapping(double lastSpeed, double currentSpeed) {
        double expected;
        if(lastSpeed >= 0.1)
            expected = 0.910001 * lastSpeed + 0.008317;
        else
            expected = 0.099317;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy sprintLandingMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double initSpeed = 0.13 * speedMultiplier + EPSILON;
        double expected = 0.910001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy walkLandingMapping(double lastSpeed, double currentSpeed, double speedMultiplier) {
        double initSpeed = 0.1 * speedMultiplier + EPSILON;
        double expected = 0.910001 * lastSpeed + initSpeed;
        return new Discrepancy(expected, currentSpeed);
    }

    private Discrepancy flyMapping(double lastSpeed, double currentSpeed) {
        double expected = 0.910001 * lastSpeed + 0.050001;
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
