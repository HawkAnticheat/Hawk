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

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.Direction;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Strafe extends MovementCheck {

    //This unintentionally trashes yet another handful of killauras and aimassists

    private static final double THRESHOLD_GROUND = 0.1;
    private static final double THRESHOLD_AIR = 0.3;

    private final Map<UUID, Long> landingTick;
    private final Map<UUID, Long> sprintingJumpTick;
    private final Map<UUID, Long> lastTickOnGround;
    private final Map<UUID, Long> lastIdleTick;

    public Strafe() {
        super("strafe", false, 5, 5, 0.99, 5000, "", null);
        landingTick = new HashMap<>();
        sprintingJumpTick = new HashMap<>();
        lastTickOnGround = new HashMap<>();
        lastIdleTick = new HashMap<>();
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();

        Block footBlock = ServerUtils.getBlockAsync(pp.getPlayer().getLocation().clone().add(0, -1, 0));
        if(footBlock == null)
            return;
        final float groundFriction = WrappedBlock.getWrappedBlock(footBlock).getSlipperiness() * 0.91F;
        final float airFriction = 0.91F;

        Vector moveHoriz = e.getTo().toVector().subtract(e.getFrom().toVector()).setY(0);
        //double moveFactor = pp.isSprinting() ? 0.13 : 0.1;
        boolean teleportBug = pp.getCurrentTick() - pp.getLastTeleportAcceptTick() < 2;
        boolean wasOnGround = teleportBug ? e.isOnGroundReally() : pp.isOnGround();
        boolean isOnGround = teleportBug ? e.isOnGroundReally() : e.isOnGround();
        long ticksSinceLanding = pp.getCurrentTick() - landingTick.getOrDefault(pp.getUuid(), -10L);
        long ticksSinceSprintJumping = pp.getCurrentTick() - sprintingJumpTick.getOrDefault(pp.getUuid(), -10L);
        long ticksSinceOnGround = pp.getCurrentTick() - lastTickOnGround.getOrDefault(pp.getUuid(), -10L);
        long ticksSinceIdle = pp.getCurrentTick() - lastIdleTick.getOrDefault(pp.getUuid(), pp.getCurrentTick());
        boolean up = e.getTo().getY() > e.getFrom().getY();
        double friction;

        //LAND (instantaneous)
        if((isOnGround && ticksSinceLanding == 1) || (ticksSinceOnGround == 1 && ticksSinceLanding == 1 && !up)) {
            friction = airFriction;
        }
        //GROUND
        else if((isOnGround && wasOnGround && ticksSinceLanding > 1) || (ticksSinceOnGround == 1 && !up && !isOnGround)) {
            friction = groundFriction;
        }
        //JUMP (instantaneous)
        else if(wasOnGround && !isOnGround && up) {
            if(pp.isSprinting()) {
                sprintingJumpTick.put(pp.getUuid(), pp.getCurrentTick());
            }
            friction = airFriction;
        }
        //SPRINT_JUMP_POST (instantaneous)
        else if(!wasOnGround && !isOnGround && ticksSinceSprintJumping == 1) {
            friction = groundFriction;
        }
        //JUMP_POST (instantaneous)
        else if(!wasOnGround && !isOnGround && ticksSinceLanding == 2) {
            friction = groundFriction;
        }
        //AIR
        else if((!((pp.hasFlyPending() && e.getPlayer().getAllowFlight()) || e.getPlayer().isFlying()) && !wasOnGround) || (!up && !isOnGround)) {
            friction = airFriction;
        }
        else {
            friction = airFriction;
        }

        Vector prevVelocity = pp.getVelocity().clone();
        if(e.hasHitSlowdown()) {
            prevVelocity.multiply(0.6);
        }
        double dX = e.getTo().getX() - e.getFrom().getX();
        double dZ = e.getTo().getZ() - e.getFrom().getZ();
        dX /= friction;
        dZ /= friction;
        dX -= prevVelocity.getX();
        dZ -= prevVelocity.getZ();
        //Debug.broadcastMessage(MathPlus.round(dX, 6) * friction / moveFactor);

        Vector accelDir = new Vector(dX, 0, dZ);
        Vector yaw = MathPlus.getDirection(e.getTo().getYaw(), 0);

        //Need to return if speed is too small since the client likes to set a motion
        //component vector to 0 if it is too small. Can't check the accelDir's component
        //vectors individually since that would open a bypass i.e. running along an axis.
        //Also, return if ticksSinceIdle is <= 2, otherwise this client behavior would
        //set off false flags. Another check needs to analyze this behavior because this
        //can be abused to bypass this check.
        if(e.hasTeleported() || e.hasAcceptedKnockback() || collidingHorizontally(e) ||
                pp.isBlocking() || pp.isConsumingItem() || pp.isPullingBow() || pp.isSneaking() ||
                moveHoriz.length() < 0.15 || e.isJump() || ticksSinceIdle <= 2 || e.isInLiquid() ||
                pp.getCurrentTick() - pp.getLastVelocityAcceptTick() == 1) {
            prepareNextMove(wasOnGround, isOnGround, e, pp, pp.getCurrentTick());
            return;
        }

        //You aren't pressing a WASD key
        if(accelDir.lengthSquared() < 0.000001) {
            prepareNextMove(wasOnGround, isOnGround, e, pp, pp.getCurrentTick());
            return;
        }

        boolean vectorDir = accelDir.clone().crossProduct(yaw).dot(new Vector(0, 1, 0)) >= 0;
        //This dot step is necessary since Bukkit's angle implementation is broken. Sometimes the dot will
        //lie just slightly off the domain [-1, 1] and this causes Math.acos(double) to return NaN.
        double dot = Math.min(Math.max(accelDir.dot(yaw) / (accelDir.length() * yaw.length()), -1), 1);
        double angle = (vectorDir ? 1 : -1) * Math.acos(dot);

        if(!isValidStrafe(angle, friction)) {
            punishAndTryRubberband(pp, e, e.getPlayer().getLocation());
        }
        else
            reward(pp);

        prepareNextMove(wasOnGround, isOnGround, e, pp, pp.getCurrentTick());
    }

    private boolean collidingHorizontally(MoveEvent e) {
        for(Direction dir : e.getBoxSidesTouchingBlocks()) {
            if(dir == Direction.EAST || dir == Direction.NORTH || dir == Direction.SOUTH || dir == Direction.WEST)
                return true;
        }
        return false;
    }

    private boolean isValidStrafe(double angle, double friction) {
        double threshold;
        if(friction == 0.546)
            threshold = THRESHOLD_GROUND;
        else
            threshold = THRESHOLD_AIR;
        double multiple = angle / (Math.PI / 4);
        return Math.abs(multiple - Math.floor(multiple)) <= threshold ||
                Math.abs(multiple - Math.ceil(multiple)) <= threshold;
    }

    private void prepareNextMove(boolean wasOnGround, boolean isOnGround, MoveEvent event, HawkPlayer pp, long currentTick) {
        UUID uuid = pp.getUuid();
        if(isOnGround) {
            lastTickOnGround.put(uuid, currentTick);
        }
        //player touched the ground
        if(!wasOnGround && event.isOnGround()) {
            landingTick.put(uuid, currentTick);
        }

        if(!event.isUpdatePos())
            lastIdleTick.put(uuid, currentTick);
    }

    @Override
    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        landingTick.remove(uuid);
        sprintingJumpTick.remove(uuid);
        lastTickOnGround.remove(uuid);
        lastIdleTick.remove(uuid);
    }
}
