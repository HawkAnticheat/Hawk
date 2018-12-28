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

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.Debug;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SpeedRewrite extends MovementCheck {

    //TODO: Rename to VelocityMagnitude
    //Suggestion: A good false-positive filter is to accumulate the total discrepancy between client & server
    //and if it exceeds a threshold after some time, then rubberband/flag. DON'T decrease it if the player
    //is standing slow or is moving slower than expected, because it is too confusing. The question is: where
    //do we rubberband to?
    //In the config, have it like this:
    //  discrepancyThreshold: 1.0
    //  tolerance: 0.01 (after each move, multiply total discrepancy by (1 - tolerance))
    //  debug: false (show current discrepancy (color coded: green is under or equal thres, red is above thres))

    //Basically, this check is doing, "if your previous speed was X then your current speed must not exceed f(X)"

    private static final double EPSILON = 0.000001;

    private final Set<UUID> prevMoveWasOnGround;
    private final Map<UUID, Double> prevSpeed;
    private final Map<UUID, Long> landingTick;
    private final Map<UUID, Long> sprintingJumpTick;
    private final Map<UUID, Double> discrepancy;

    public SpeedRewrite() {
        super("speed", "%player% failed movement speed, VL: %vl%");
        prevMoveWasOnGround = new HashSet<>();
        prevSpeed = new HashMap<>();
        landingTick = new HashMap<>();
        sprintingJumpTick = new HashMap<>();
        discrepancy = new HashMap<>();
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
        //LAND (instantaneous)
        if(isOnGround && ticksSinceLanding == 1) {
            if(pp.isSprinting() && speed > sprintLandingMapping(lastSpeed, multiplier)) {
                failed = SpeedType.LANDING_SPRINT;
            }
            else if(!pp.isSprinting() && speed > walkLandingMapping(lastSpeed, multiplier)) {
                failed = SpeedType.LANDING_WALK;
            }
        }
        //GROUND
        else if(isOnGround && wasOnGround && ticksSinceLanding > 1) {
            if(pp.isSneaking() && speed > sneakGroundMapping(lastSpeed, multiplier)) {
                failed = SpeedType.SNEAK;
            }
            else if(!pp.isSprinting() && speed > walkGroundMapping(lastSpeed, multiplier)) {
                failed = SpeedType.WALK;
            }
            else if(pp.isSprinting() && speed > sprintGroundMapping(lastSpeed, multiplier)) {
                failed = SpeedType.SPRINT;
            }

        }
        //SPRINT-JUMP (instantaneous)
        else if(pp.isSprinting() && wasOnGround && !isOnGround && up) {
            //SPRINT-JUMP_CONTINUE
            if(ticksSinceLanding == 1 && speed > sprintJumpContinueMapping(lastSpeed, multiplier)) {
                failed = SpeedType.SPRINT_JUMPING_CONTINUE;
            }
            //SPRINT-JUMP_START
            else if(ticksSinceLanding != 1 && speed > sprintJumpStartMapping(lastSpeed, multiplier)) {
                failed = SpeedType.SPRINT_JUMPING_START;
            }
            sprintingJumpTick.put(p.getUniqueId(), pp.getCurrentTick());
        }
        //POST_JUMP (instantaneous)
        else if(!wasOnGround && !isOnGround && ticksSinceSprintJumping == 1 && speed > jumpPostMapping(lastSpeed)) {
            failed = SpeedType.SPRINT_JUMP_POST;
        }
        //FLYING
        else if(((pp.hasFlyPending() && p.getAllowFlight()) || p.isFlying()) && speed > flyMapping(lastSpeed)) {
            failed = SpeedType.FLYING;
        }
        //AIR
        else if(!((pp.hasFlyPending() && p.getAllowFlight()) || p.isFlying()) && !wasOnGround) {
            if(pp.isSneaking() && speed > sneakAirMapping(lastSpeed)) {
                failed = SpeedType.AIR_SNEAK;
            }
            else if(pp.isSprinting() && speed > sprintAirMapping(lastSpeed)) {
                failed = SpeedType.AIR_SPRINT;
            }
            else if(!pp.isSprinting() && speed > walkAirMapping(lastSpeed)) {
                failed = SpeedType.AIR_WALK;
            }
        }


        if(failed != null) {
            //punish(pp, false, event);
            //tryRubberband(event, p.getLocation());
            Debug.broadcastMessage(failed + " (" + lastSpeed + ", " + speed + ")");
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
    private double walkAirMapping(double lastSpeed) {
        return 0.910001 * lastSpeed + 0.020001;
    }

    private double jumpPostMapping(double lastSpeed) {
        return 0.546001 * lastSpeed + 0.026001;
    }

    private double sprintJumpStartMapping(double lastSpeed, double speedMultiplier) {
        return 0.546001 * lastSpeed + (0.3274 * (1 + ((speedMultiplier - 1) * 0.389127))); //Don't question it.
    }

    private double sprintJumpContinueMapping(double lastSpeed, double speedMultiplier) {
        return 0.910001 * lastSpeed + (0.3274 * (1 + ((speedMultiplier - 1) * 0.389127))); //Don't question it.
    }

    private double sprintGroundMapping(double lastSpeed, double speedMultiplier) {
        double initSpeed = 0.13 * speedMultiplier + EPSILON;
        if(lastSpeed >= 0.13)
            return 0.546001 * lastSpeed + initSpeed;
        else
            return 0.2;
    }

    private double walkGroundMapping(double lastSpeed, double speedMultiplier) {
        double initSpeed = 0.1 * speedMultiplier + EPSILON;
        if(lastSpeed >= 0.1)
            return 0.546001 * lastSpeed + initSpeed;
        else
            return 0.16;
    }

    //TODO: fix speed multiplier
    private double sneakGroundMapping(double lastSpeed, double speedMultiplier) {
        double initSpeed = 0.041560 * speedMultiplier + EPSILON;
        if(lastSpeed >= 0.1)
            return 0.546001 * lastSpeed + initSpeed;
        else
            return 0.096161;
    }

    //speed potions do not affect swimming
    private double waterMapping(double lastSpeed) {
        return 0.800001 * lastSpeed + 0.020001;
    }

    private double lavaMapping(double lastSpeed) {
        return 0;
    }

    private double groundWaterMapping(double lastSpeed) {
        return 0;
    }

    private double groundLavaMapping(double lastSpeed) {
        return 0;
    }

    //speed potions do not affect air movement
    private double sprintAirMapping(double lastSpeed) {
        if(lastSpeed >= 0.1)
            return 0.910001 * lastSpeed + 0.026001;
        else
            return 0.16;
    }

    private double sneakAirMapping(double lastSpeed) {
        if(lastSpeed >= 0.1)
            return 0.910001 * lastSpeed + 0.008317;
        else
            return 0.099317;
    }

    private double sprintLandingMapping(double lastSpeed, double speedMultiplier) {
        double initSpeed = 0.13 * speedMultiplier + EPSILON;
        return 0.910001 * lastSpeed + initSpeed;
    }

    private double walkLandingMapping(double lastSpeed, double speedMultiplier) {
        double initSpeed = 0.1 * speedMultiplier + EPSILON;
        return 0.910001 * lastSpeed + initSpeed;
    }

    private double flyMapping(double lastSpeed) {
        return 0.910001 * lastSpeed + 0.050001;
    }

    private enum SpeedType {
        WALK,
        SPRINT,
        SPRINT_JUMPING_CONTINUE,
        SPRINT_JUMPING_START,
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
