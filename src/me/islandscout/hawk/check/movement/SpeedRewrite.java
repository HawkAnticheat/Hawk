/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check.movement;

import com.sun.crypto.provider.DHKeyPairGenerator;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.Debug;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class SpeedRewrite extends MovementCheck {

    //IMPORTANT: Must use previous move to determine current material being stood on for friction calculation.

    //Basically, this check is doing, "if your previous speed was X then your current speed must not exceed f(X)"

    //Problem: As soon as the client lands, the client will update its position immediately in attempt to update
    //  the onGround status. Now, chances are, this move has very little change in position. Therefore, the next
    //  move needs to make up for that. I suggest finding the distance between the move prior to landing and the
    //  move succeeding the landing move, then dividing that by 2.

    private final double GROUND_DEFAULT;
    private final double GROUND_ICE;
    private final double AIR_DEFAULT;
    private final double FLY_DEFAULT;

    private final Set<UUID> prevMoveWasOnGround;
    private final Map<UUID, Double> prevSpeed;
    private final Map<UUID, Vector> posBeforeLanding;
    private final Map<UUID, Long> landingTick;


    public SpeedRewrite() {
        super("speednew", "%player% failed movement speed, VL: %vl%");
        GROUND_DEFAULT = Math.pow(0.28635, 2);
        GROUND_ICE = Math.pow(0.271, 2);
        AIR_DEFAULT = Math.pow(0.2888889, 2);
        FLY_DEFAULT = 1;
        prevMoveWasOnGround = new HashSet<>();
        prevSpeed = new HashMap<>();
        posBeforeLanding = new HashMap<>();
        landingTick = new HashMap<>();
    }

    int iLocal = 0;
    boolean readyLocal = false;
    boolean wasOnGroundLocal = true;
    double prevSpeedLocal = 0;

    @Override
    protected void check(PositionEvent event) {
        if (!event.hasDeltaPos())
            return;
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        double speed = Math.sqrt(Math.pow(event.getTo().getX() - event.getFrom().getX(), 2) + Math.pow(event.getTo().getZ() - event.getFrom().getZ(), 2));
        double lastSpeed = prevSpeed.getOrDefault(p.getUniqueId(), 0D);
        boolean wasOnGround = prevMoveWasOnGround.contains(p.getUniqueId());
        boolean isOnGround = event.isOnGround(); //TODO: This can be spoofed. Find a better alternative or patch it.
        long ticksSinceLanding = pp.getCurrentTick() - landingTick.getOrDefault(p.getUniqueId(), 0L);

        SpeedType failed = null;
        if(isOnGround && ticksSinceLanding == 1) {
            if(pp.isSprinting() && speed > sprintLandingMapping(lastSpeed)) {
                failed = SpeedType.LANDING;
            }
            /*else if(!pp.isSprinting() && speed > walkLandingMapping(lastSpeed)) {

            }*/
        }
        else if(!pp.isSprinting() /*&& isOnGround*/ && wasOnGround && speed > walkGroundMapping(lastSpeed)) {
            failed = SpeedType.WALK;
        } else if(pp.isSprinting() && isOnGround && wasOnGround && speed > sprintGroundMapping(lastSpeed)) {
            failed = SpeedType.SPRINT;
        } else if(pp.isSprinting() && !isOnGround && wasOnGround && speed > sprintJumpMapping(lastSpeed)) {
            failed = SpeedType.SPRINT_JUMP;
        } else if(!wasOnGround && speed > airMapping(lastSpeed)) {
            failed = SpeedType.AIR;
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
            posBeforeLanding.put(p.getUniqueId(), event.getFrom().toVector());
        }


        double speedSquared = speed * speed;
        if (speedSquared != 0)
            readyLocal = true;
        if (readyLocal) {

            if(!isOnGround && wasOnGround) {
                //use this to generate mappings
                //Debug.broadcastMessage("(" + Math.sqrt(prevSpeedLocal) + ", " + Math.sqrt(speedSquared) + ")");
            }
            //Debug.broadcastMessage("(" + iLocal + ", " + Math.sqrt(speedSquared) + ")");
            iLocal++;
        }

        wasOnGroundLocal = isOnGround;
        prevSpeedLocal = speedSquared;
    }


    //these functions must be extremely accurate to support high level speed potions
    //added epsilon of 0.000001 and truncated if necessary
    private double iceMapping(double lastSpeed) {
        if(lastSpeed >= 0.05)
            return 0.891802 * lastSpeed + 0.029233;
        else
            return 0.0846;
    }

    private double sprintGroundMapping(double lastSpeed) {
        //Debug.broadcastMessage("chking sprint-ground");
        if(lastSpeed >= 0.1)
            return 0.546001 * lastSpeed + 0.130001;
        else
            return 0.16;
    }

    private double walkGroundMapping(double lastSpeed) {
        //Debug.broadcastMessage("chking walk-ground");
        if(lastSpeed >= 0.1)
            return 0.546001 * lastSpeed + 0.100001;
        else
            return 0.16;
    }

    private double walkJumpMapping(double lastSpeed) {
        return 0;
    }

    //speed potions do not affect sprint-jumps
    private double sprintJumpMapping(double lastSpeed) {
        //Debug.broadcastMessage("chking sprint-jump");
        return 0.910001 * lastSpeed + 0.3274;
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
    private double airMapping(double lastSpeed) {
        return 0.910001 * lastSpeed + 0.026001;
    }

    private double sprintLandingMapping(double lastSpeed) {
        return 0.910001 * lastSpeed + 0.127401;
    }

    private double walkLandingMapping(double lastSpeed) {
        return 0.910001 * lastSpeed + 0.100001;
    }

    private enum SpeedType {
        WALK,
        SPRINT,
        SPRINT_JUMP,
        AIR,
        LANDING
    }
}
