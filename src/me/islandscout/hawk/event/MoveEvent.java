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

package me.islandscout.hawk.event;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import me.islandscout.hawk.wrap.packet.WrappedPacket;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class MoveEvent extends Event {

    //Remember that the client only updates position/rotation information if
    //it is significant enough. Use hasDeltaPos() hasDeltaRot() when necessary.

    //Position events will not pass through checks if the player is teleporting.

    private final boolean onGround;
    private boolean onGroundReally;
    private boolean teleportAccept;
    private Location toLocation;
    private Location cancelLocation;
    private final boolean updatePos;
    private final boolean updateRot;
    private Vector acceptedKnockback;
    private boolean failedKnockback;
    //Idk, it's weird. Hitting while sprinting or with kb enchant will multiply horizontal speed by 0.6.
    //Also, movement checks utilizing hit slowdown must use this field and not the one in HawkPlayer
    //since HawkPlayer's will always be false because HawkPlayer will tick on MoveEvent#preProcess()
    private boolean hitSlowdown;
    private Set<Direction> boxSidesTouchingBlocks;
    private boolean inWater;
    private boolean jumped;
    private boolean slimeBlockBounce;
    private boolean nextIsSlimeBlockBounce;
    private boolean step;
    private boolean liquidExit;
    private boolean glidingInUnloadedChunk;
    private boolean possiblePistonPush;
    private boolean touchingCeiling;
    private float newFriction; //This is the friction that is used to compute this move's initial force.
    private float oldFriction; //This is the friction that affects this move's velocity.
    private float maxExpectedInputForce;
    private float maxExpectedInputForceNoItemUse;
    private Vector waterFlowForce;
    private List<Pair<Block, Vector>> liquidsAndDirections;
    //No, don't compute a delta vector during instantiation since it won't respond to teleports.

    public MoveEvent(Player p, Location update, boolean onGround, HawkPlayer pp, WrappedPacket packet, boolean updatePos, boolean updateRot) {
        super(p, pp, packet);
        toLocation = update;
        this.updatePos = updatePos;
        this.updateRot = updateRot;
        this.onGround = onGround;
    }

    @Override
    public boolean preProcess() {
        onGroundReally = AdjacentBlocks.onGroundReally(getTo(), getTo().getY() - getFrom().getY(), true, 0.02, pp);
        step = testStep();
        hitSlowdown = pp.hasHitSlowdown();
        boxSidesTouchingBlocks = AdjacentBlocks.checkTouchingBlock(new AABB(getTo().toVector().add(new Vector(-0.299999, 0.000001, -0.299999)), getTo().toVector().add(new Vector(0.299999, 1.799999, 0.299999))), getTo().getWorld(), 0.0001, pp.getClientVersion());
        acceptedKnockback = handlePendingVelocities();
        liquidsAndDirections = testWater();
        inWater = liquidsAndDirections.size() > 0;
        touchingCeiling = testTouchCeiling();
        jumped = testJumped();
        oldFriction = pp.getFriction();
        newFriction = computeFriction();
        slimeBlockBounce = testSlimeBlockBounce();
        waterFlowForce = computeWaterFlowForce();
        maxExpectedInputForce = computeMaximumInputForce(true);
        maxExpectedInputForceNoItemUse = computeMaximumInputForce(false); //of course I have to do this twice...
        possiblePistonPush = testPistonPush(getFrom().toVector(), getTo().toVector(), pp);
        double dy = getTo().getY() - getFrom().getY();
        double waterYForce = pp.getWaterFlowForce().getY();
        liquidExit = pp.isExitingLiquidTemp() && (Math.abs(dy - waterYForce - 0.3) < 0.00001 || Math.abs(dy - 0.04 - waterYForce - 0.3) < 0.00001);
        double dyRaw = getTo().getY() - pp.getPositionRaw().getY();

        pp.tick();
        if(isUpdatePos())
            pp.setHasMoved();

        nextIsSlimeBlockBounce = testSlimeBlockBounceNext();
        glidingInUnloadedChunk = Math.abs(dyRaw - -0.098) < 0.0000001 && (acceptedKnockback == null || Math.abs(acceptedKnockback.getY() - -0.098) > 0.0000001);

        Vector lastPos = pp.getPosition(); //set this now, because HawkPlayer#getPosition() will change after teleport

        //handle moves while we are waiting for the player to accept teleport
        //Discard moves while we are waiting. We don't want to spam teleports while
        //someone is hacking their ass off. If you spam a bunch of teleport packets,
        //the client will gracefully respond to all of them with flying packets, and
        //if the player is standing still, NMS won't be able to differentiate the
        //teleport from a normal move since the expected teleport location keeps
        //changing too quickly. Thus NMS will tick the player for all of those
        //packets, opening up a fasteat/fastbow/regen/etc. bypass.
        if (pp.isTeleporting() && pp.getPendingTeleports().size() > 0) {

            Pair<Location, Long> tpPair = pp.getPendingTeleports().get(0);
            Location tpLoc = tpPair.getKey();
            int elapsedTicks = (int)(pp.getCurrentTick() - tpPair.getValue());
            int ping = ServerUtils.getPing(p);

            if (!onGround && updatePos && updateRot && tpLoc.equals(getTo())) {
                //move matched teleport location
                //if(elapsedTicks > Math.max((ping / 50) - 1, 0)) { //-1 is an arbitrary constant to keep things smooth
                    //most likely accepted teleport, unless this move is a coincidence (edge case & insanely difficult to reproduce without computer assistance)
                    pp.updatePositionYawPitch(tpLoc.toVector(), tpLoc.getYaw(), tpLoc.getPitch(), true);
                    pp.setVelocity(new Vector(0, 0, 0));
                    pp.getPendingTeleports().remove(0);
                    pp.setLastTeleportAcceptTick(pp.getCurrentTick());
                    teleportAccept = true;
                    if(pp.getPendingTeleports().size() == 0) {
                        pp.setTeleporting(false);
                    } else {
                        return false;
                    }
                //}
                //else {
                //    pp.setPositionRaw(getTo().toVector());
                //    return false;
                //}
            } else if(!pp.getPlayer().isSleeping()) {
                if (elapsedTicks > (ping / 50) + 5) { //5 is an arbitrary constant to keep things smooth
                    //didn't accept teleport, so help guide the confused client back to the last tp location
                    Location tp = pp.getPendingTeleports().get(pp.getPendingTeleports().size() - 1).getKey();
                    pp.getPendingTeleports().clear();
                    pp.teleport(tp, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
                pp.setPositionRaw(getTo().toVector());
                return false;
            }
        }

        //handle illegal move or discrepancy
        else if (getFrom().getWorld().equals(getTo().getWorld()) && getTo().distanceSquared(getFrom()) > 64) {
            resync();
            pp.setPositionRaw(getTo().toVector());
            return false;
        }

        //handle gliding in unloaded chunk
        if(!teleportAccept && glidingInUnloadedChunk && Math.abs(pp.getVelocityPredicted().getY() - -0.098) > 0.000001) {
            resync();
            pp.setPositionRaw(getTo().toVector());
            return false;
        }

        //set override rubberband location
        Location serverSideLoc = p.getLocation().clone();
        if(pp.isFlying() || pp.isInLava() || pp.isInWater() ||
                AdjacentBlocks.onGroundReally(serverSideLoc, -1, true, 0.03, pp))
            pp.setAltSetbackLoc(null);
        if(teleportAccept)
            pp.setAltSetbackLoc(serverSideLoc);

        pp.updateIgnoredBlockCollisions(getTo().toVector(), lastPos, teleportAccept);
        return true;
    }

    @Override
    public void postProcess() {
        pp.setLastMoveTime(System.currentTimeMillis());
        pp.setPositionRaw(getTo().toVector());
        boolean inLava = testLava();

        if(isCancelled() && getCancelLocation() != null) {
            //handle rubberband if applicable
            setTo(getCancelLocation());
            pp.setTeleporting(true);
            pp.teleport(getCancelLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        }

        //handle item consumption
        if(pp.getItemConsumeTicks() > 31 && pp.isConsumingItem()) {
            pp.setConsumingItem(false);
        }

        //handle liquid exit
        pp.setLiquidExit(testLiquidExit());

        //handle swimming
        pp.setInWater(isInWater());
        pp.setInLava(inLava);
        if(pp.getCurrentTick() < 2)
            pp.setSwimming(pp.isInWater());
        long ticksSinceSwimToggle = pp.getCurrentTick() - pp.getLastInLiquidToggleTick();
        pp.setSwimming(!pp.isFlying() && ((pp.isInWater() && ticksSinceSwimToggle > 0) || (!pp.isInWater() && ticksSinceSwimToggle < 1)));

        if(isOnGround() && !pp.isOnGround())
            pp.updateLastLandTick();

        Location to = getTo();
        Location from = getFrom();
        pp.updatePositionYawPitch(to.toVector(), to.getYaw(), to.getPitch(), isUpdatePos());
        pp.updateFallDistance(to);
        pp.updateTotalAscensionSinceGround(from.getY(), to.getY());
        pp.setOnGround(isOnGround());
        pp.setOnGroundReally(isOnGroundReally());
        pp.getBoxSidesTouchingBlocks().clear();
        pp.getBoxSidesTouchingBlocks().addAll(getBoxSidesTouchingBlocks());
        pp.setWaterFlowForce(getWaterFlowForce());
        pp.setFriction(newFriction);
        pp.setSentPosUpdate(isUpdatePos());
        if(hasAcceptedKnockback())
            pp.updateLastVelocityAcceptTick();
    }

    private boolean testLiquidExit() {
        //seriously?
        boolean collidingHorizontally = getBoxSidesTouchingBlocks().contains(Direction.EAST) ||
                                        getBoxSidesTouchingBlocks().contains(Direction.WEST) ||
                                        getBoxSidesTouchingBlocks().contains(Direction.NORTH) ||
                                        getBoxSidesTouchingBlocks().contains(Direction.SOUTH);

        Vector offset = getTo().toVector().subtract(getFrom().toVector());
        offset.setY(offset.getY() * (pp.isInWater() ? 0.8 : 0.5) - 0.02);

        AABB aabb = AABB.playerCollisionBox.clone();
        aabb.translate(getTo().toVector().add(offset).add(new Vector(0, 0.6 - getTo().getY() + getFrom().getY(), 0)));

        return (pp.isInWater() || pp.isInLava()) && collidingHorizontally && !aabb.isLiquidPresent(pp.getWorld());
    }

    private boolean testStep() {
        Vector prevPos = getFrom().toVector();
        Vector extrapolate = getFrom().toVector();
        //when on ground, Y velocity is inherently 0; no need to do pointless math.
        extrapolate.setY(extrapolate.getY() + (pp.isOnGroundReally() ? -0.0784 : ((pp.getVelocity().getY() - 0.08) * 0.98)));

        AABB box = AABB.playerCollisionBox.clone();
        box.translate(extrapolate);
        List<AABB> verticalCollision = box.getBlockAABBs(pp.getPlayer().getWorld(), pp.getClientVersion(), Material.WEB);

        if(verticalCollision.isEmpty() && !pp.isOnGround()) {
            return false;
        }

        double highestVertical = extrapolate.getY();
        for (AABB blockAABB : verticalCollision) {
            double aabbMaxY = blockAABB.getMax().getY();
            if(aabbMaxY > highestVertical) {
                highestVertical = aabbMaxY;
            }
        }

        //move to this position, but with clipped Y (moving horizontally)
        box = AABB.playerCollisionBox.clone();
        box.translate(getTo().toVector().clone().setY(highestVertical));
        box.expand(0, -0.00000000001, 0);

        List<AABB> horizontalCollision = box.getBlockAABBs(pp.getPlayer().getWorld(), pp.getClientVersion(), Material.WEB);

        if(horizontalCollision.isEmpty()) {
            return false;
        }

        double expectedY = prevPos.getY();
        double highestPointOnAABB = -1;
        for (AABB blockAABB : horizontalCollision) {
            double blockAABBY = blockAABB.getMax().getY();
            if(blockAABBY - prevPos.getY() > 0.6) {
                return false;
            }
            if(blockAABBY > expectedY) {
                expectedY = blockAABBY;
            }
            if(blockAABBY > highestPointOnAABB) {
                highestPointOnAABB = blockAABBY;
            }
        }

        return (onGround || onGroundReally) && Math.abs(prevPos.getY() - highestPointOnAABB) > 0.0001 && Math.abs(getTo().getY() - expectedY) < 0.0001;
    }

    //Good thing I have MCP to figure this one out
    private List<Pair<Block, Vector>> testWater() {
        AABB liquidTest = AABB.playerWaterCollisionBox.clone();
        liquidTest.translate(getTo().toVector());
        List<Pair<Block, Vector>> liquids = new ArrayList<>();
        List<Block> blocks = liquidTest.getBlocks(p.getWorld());
        for(Block b : blocks) {
            if(Physics.waterDefs.contains(b.getType())) {
                Vector direction = WrappedBlock.getWrappedBlock(b, pp.getClientVersion()).getFlowDirection();
                liquids.add(new Pair<>(b, direction));
            }
        }
        return liquids;
    }

    private boolean testLava() {
        AABB lavaTest = AABB.playerLavaCollisionBox.clone();
        lavaTest.translate(getTo().toVector());
        List<Block> blocks = lavaTest.getBlocks(p.getWorld());
        for(Block b : blocks) {
            if(Physics.lavaDefs.contains(b.getType())) {
                return true;
            }
        }
        return false;
    }

    //checks if the player's dY matches the expected dY
    private boolean testJumped() {
        int jumpBoostLvl = 0;
        for (PotionEffect pEffect : p.getActivePotionEffects()) {
            if (pEffect.getType().equals(PotionEffectType.JUMP)) {
                byte amp = (byte)pEffect.getAmplifier();
                jumpBoostLvl = amp + 1;
                break;
            }
        }
        float expectedDY = Math.max(0.42F + jumpBoostLvl * 0.1F, 0F);
        boolean leftGround = (pp.isOnGround() && !isOnGround());
        Vector from = pp.hasSentPosUpdate() ? getFrom().toVector() : pp.getPositionPredicted();
        float dY = (float)(getTo().getY() - from.getY());

        //Jumping right as you enter a 2-block-high space will not change your motY.
        //When these conditions are met, we'll give them the benefit of the doubt and say that they jumped.
        {
            AABB box = AABB.playerCollisionBox.clone();
            box.expand(-0.000001, -0.000001, -0.000001);
            box.translate(getTo().toVector().add(new Vector(0, expectedDY, 0)));
            boolean collidedNow = !box.getBlockAABBs(getTo().getWorld(), pp.getClientVersion()).isEmpty();

            box = AABB.playerCollisionBox.clone();
            box.expand(-0.000001, -0.000001, -0.000001);
            box.translate(getFrom().toVector().add(new Vector(0, expectedDY, 0)));
            boolean collidedBefore = !box.getBlockAABBs(getTo().getWorld(), pp.getClientVersion()).isEmpty();

            if(collidedNow && !collidedBefore && leftGround && dY == 0) {
                expectedDY = 0;
            }
        }

        boolean kbSimilarToJump = acceptedKnockback != null &&
                (Math.abs(acceptedKnockback.getY() - expectedDY) < 0.001 || touchingCeiling);
        return !kbSimilarToJump && ((expectedDY == 0 && pp.isOnGround()) || leftGround) && (dY == expectedDY || touchingCeiling);
    }

    private boolean testTouchCeiling() {
        //Change by Havesta to more accurately handle Y collision
        Vector from = pp.hasSentPosUpdate() ? getFrom().toVector() : pp.getPositionPredicted();
        Vector pos = from.clone().setY(getTo().getY());
        AABB collisionBox = AABB.playerCollisionBox.clone();
        collisionBox.expand(-0.000001, -0.000001, -0.000001);
        collisionBox.translate(pos);
        return AdjacentBlocks.checkTouchingBlock(collisionBox, getTo().getWorld(), 0.0001, pp.getClientVersion()).contains(Direction.TOP);
    }

    //Again, kudos to MCP for guiding me to the right direction
    private boolean testSlimeBlockBounce() {
        if(Hawk.getServerVersion() < 8)
            return false;
        Vector from = pp.hasSentPosUpdate() ? getFrom().toVector() : pp.getPositionPredicted();
        float deltaY = (float)(getTo().getY() - from.getY());
        Block staningOn = ServerUtils.getBlockAsync(from.toLocation(pp.getWorld()).add(0, -0.2, 0));
        if(staningOn == null || staningOn.getType() != Material.SLIME_BLOCK)
            return false;
        float prevPrevDeltaY = (float)pp.getPreviousVelocity().getY();
        float expected = (-((prevPrevDeltaY - 0.08F) * 0.98F) - 0.08F) * 0.98F;
        //TODO you're supposed to get the sneaking from the LAST tick, not this one
        return !pp.isSneaking() && pp.isOnGround() && !isOnGround() && deltaY >= 0 && Math.abs(expected - deltaY) < 0.00001;
    }

    private boolean testSlimeBlockBounceNext() {
        if(Hawk.getServerVersion() < 8)
            return false;
        Vector to = isUpdatePos() ? getTo().toVector() : pp.getPositionPredicted();
        Block staningOn = ServerUtils.getBlockAsync(to.toLocation(pp.getWorld()).add(0, -0.2, 0));
        if(staningOn == null || staningOn.getType() != Material.SLIME_BLOCK)
            return false;

        float prevDeltaY = (float)pp.getPreviousPredictedVelocity().getY();
        return !pp.isSneaking() && isOnGround() && !pp.isOnGround() && prevDeltaY < 0;
    }

    private boolean testPistonPush(Vector from, Vector to, HawkPlayer pp) {
        Vector delta = to.clone().subtract(from);

        //TODO: put another condition in these so that it wont trigger on practically every move?
        boolean x = Math.abs(delta.getX()) < 1.5;
        boolean y = Math.abs(delta.getY()) < 1.5;
        boolean z = Math.abs(delta.getZ()) < 1.5;

        if(x || y || z) {
            return hawk.getLagCompensator().testNearPiston(to, pp.getWorld(), ServerUtils.getPing(pp.getPlayer()));
        }
        return false;
    }

    private float computeFriction() {
        float friction = 0.91F;

        //patch some inconsistencies
        boolean teleportBug = pp.getCurrentTick() == pp.getLastTeleportAcceptTick();
        boolean onGround = teleportBug ? pp.isOnGroundReally() : pp.isOnGround();

        if (onGround) {
            Vector pos = pp.getPosition();
            Block b = ServerUtils.getBlockAsync(new Location(pp.getWorld(), pos.getX(), pos.getY() - 1, pos.getZ()));
            if(b != null) {
                friction *= WrappedBlock.getWrappedBlock(b, pp.getClientVersion()).getSlipperiness();
            }
        }
        return friction;
    }

    private float computeMaximumInputForce(boolean considerItemUse) {
        //"initForce" is the value of "strafe" or "forward" in MCP's moveEntityWithHeading(float, float) in
        //EntityLivingBase. When the WASD keys are polled, these are either incremented or decremented by 1.
        //Before reaching the method, the "strafe" and "forward" values are multiplied by 0.98,
        //and if sneaking, they are also multiplied by 0.3, and if using an item, they are also multiplied by 0.2.
        float initForce = 0.98F;
        if(pp.isSneaking())
            initForce *= 0.3;

        boolean usingItem = considerItemUse && (pp.isConsumingOrPullingBowMetadataIncluded() || pp.isBlocking());
        if(usingItem)
            initForce *= 0.2;
        boolean sprinting = pp.isSprinting() && !usingItem && !pp.isSneaking();
        boolean flying = pp.isFlying();

        float speedEffectMultiplier = 1;
        for (PotionEffect effect : p.getActivePotionEffects()) {
            if (!effect.getType().equals(PotionEffectType.SPEED))
                continue;
            int level = effect.getAmplifier() + 1;
            speedEffectMultiplier += (level * 0.2F);
        }

        //patch some inconsistencies
        boolean teleportBug = pp.getCurrentTick() == pp.getLastTeleportAcceptTick();
        boolean onGround = teleportBug ? pp.isOnGroundReally() : pp.isOnGround();

        //Skidded from MCP's moveEntityWithHeading(float, float) in EntityLivingBase
        float multiplier;
        if (onGround) {
            //0.16277136 technically should be 0.162771336. But this is what was written in MCP.
            //0.162771336 is not a magic number. It is 0.546^3
            multiplier = 0.1F * 0.16277136F / (newFriction * newFriction * newFriction);
            float groundMultiplier = 5 * p.getWalkSpeed() * speedEffectMultiplier;
            multiplier *= groundMultiplier;
        }
        else {
            float flyMultiplier = 10 * p.getFlySpeed();
            multiplier = (flying ? 0.05F : 0.02F) * flyMultiplier;
        }

        //Assume moving diagonally, since sometimes it's faster to move diagonally.
        //Skidded from MCP's moveFlying(float, float, float) in Entity
        float diagonal = (float)Math.sqrt(2 * initForce * initForce);
        if (diagonal < 1.0F) {
            diagonal = 1.0F;
        }
        //Force for each component of the diagonal vector.
        //Division by "diagonal" pretty much normalizes it... most of the time.
        float componentForce = initForce * multiplier / diagonal;

        //now find the hypotenuse i.e. magnitude of this diagonal vector
        float finalForce = (float)Math.sqrt(2 * componentForce * componentForce);

        return (float) (finalForce * (sprinting ? (flying ? 2 : 1.3) : 1));
    }

    private Vector computeWaterFlowForce() {
        Vector finalForce = new Vector();
        for(Pair<Block, Vector> liquid : liquidsAndDirections) {
            Material mat = liquid.getKey().getType();
            if(mat == Material.STATIONARY_WATER || mat == Material.WATER) {
                finalForce.add(liquid.getValue());
            }
        }
        if(finalForce.lengthSquared() > 0 && !pp.isFlying()) {
            finalForce.normalize();
            finalForce.multiply(Physics.WATER_FLOW_FORCE_MULTIPLIER);
            return finalForce;
        }
        return finalForce;
    }

    //This literally makes me want to punch a wall.
    //TODO make this process simpler and more accruate by handling key combo presses during KB.
    private Vector handlePendingVelocities() {
        List<Pair<Vector, Long>> kbs = pp.getPendingVelocities();
        if (kbs.size() > 0) {
            double epsilon = 0.003;
            int kbIndex;
            int expiredKbs = 0;
            long currTime = System.currentTimeMillis();
            Vector currVelocity = new Vector(getTo().getX() - getFrom().getX(), getTo().getY() - getFrom().getY(), getTo().getZ() - getFrom().getZ());
            boolean jump = pp.isOnGround() && Math.abs(0.42 - currVelocity.getY()) < 0.00001;
            double speedPotMultiplier = 1;
            for (PotionEffect effect : p.getActivePotionEffects()) {
                if (!effect.getType().equals(PotionEffectType.SPEED))
                    continue;
                speedPotMultiplier = 1 + (effect.getAmplifier() + 1 * 0.2);
            }
            boolean flying          = pp.isFlying();
            double sprintMultiplier = flying ? (pp.isSprinting() ? 2 : 1) : (pp.isSprinting() ? 1.3 : 1);
            double weirdConstant    = (jump && pp.isSprinting() ? 0.2518462 : (pp.isSwimming() ? 0.0196 : 0.098)); //(pp.isOnGround() ? 0.098 : (flying ? 0.049 : 0.0196));
            double baseMultiplier   = flying ? (10 * p.getFlySpeed()) : (5 * p.getWalkSpeed() * speedPotMultiplier);
            double maxDiscrepancy   = weirdConstant * baseMultiplier * sprintMultiplier + epsilon;

            //pending knockbacks must be in order; get the first entry in the list.
            //if the first entry doesn't work (probably because they were fired on the same tick),
            //then work down the list until we find something
            for (kbIndex = 0; kbIndex < kbs.size(); kbIndex++) {
                Pair<Vector, Long> kb = kbs.get(kbIndex);

                //replace the following if-statement with this sometime? You'll have to worry about the else-statement, though.
                //int timeDiff = (int)(currTime - kb.getValue());
                //int ping = ServerUtils.getPing(p);
                //int lowerBound = 100;
                //int upperBound = 300;
                //if (timeDiff >= ping - lowerBound && timeDiff <= ping + upperBound) { //400ms window to allow for network jitter

                if (currTime - kb.getValue() <= ServerUtils.getPing(p) + 200) { //add 200 just in case the player's ping jumps a bit

                    Vector kbVelocity = kb.getKey();
                    double x = hitSlowdown ? 0.6 * kbVelocity.getX() : kbVelocity.getX();
                    double y = kbVelocity.getY();
                    double z = hitSlowdown ? 0.6 * kbVelocity.getZ() : kbVelocity.getZ();

                    //check Y component
                    //skip to next kb if...
                    if (!((boxSidesTouchingBlocks.contains(Direction.TOP) && y > 0) || (boxSidesTouchingBlocks.contains(Direction.BOTTOM) && y < 0)) && /*...player isn't colliding...*/
                            Math.abs(y - currVelocity.getY()) > 0.01 && /*...and velocity is nowhere close to kb velocity...*/
                            !jump && !pp.isSwimming() && !step /*...and did not jump and is not swimming and did not "step"*/) {
                        continue;
                    }

                    double minThresX = x - maxDiscrepancy;
                    double maxThresX = x + maxDiscrepancy;
                    double minThresZ = z - maxDiscrepancy;
                    double maxThresZ = z + maxDiscrepancy;

                    //check X component
                    //skip to next kb if...
                    if (!((boxSidesTouchingBlocks.contains(Direction.EAST) && x > 0) || (boxSidesTouchingBlocks.contains(Direction.WEST) && x < 0)) && /*...player isn't colliding...*/
                            !(currVelocity.getX() <= maxThresX && currVelocity.getX() >= minThresX)) { /*...and velocity is nowhere close to kb velocity...*/
                        continue;
                    }
                    //check Z component
                    //skip to next kb if...
                    if (!((boxSidesTouchingBlocks.contains(Direction.SOUTH) && z > 0) || (boxSidesTouchingBlocks.contains(Direction.NORTH) && z < 0)) && /*...player isn't colliding...*/
                            !(currVelocity.getZ() <= maxThresZ && currVelocity.getZ() >= minThresZ)) { /*...and velocity is nowhere close to kb velocity...*/
                        continue;
                    }
                    kbs.subList(0, kbIndex + 1).clear();
                    return kbVelocity;
                }
                else {
                    failedKnockback = true;
                    expiredKbs++;
                }
            }
            kbs.subList(0, expiredKbs).clear();
        }
        return null;
    }

    public Player getPlayer() {
        return p;
    }

    public Location getTo() {
        return toLocation;
    }

    public Location getFrom() {
        Vector position = pp.getPosition();
        return new Location(pp.getWorld(), position.getX(), position.getY(), position.getZ(), pp.getYaw(), pp.getPitch());
    }

    public void setTo(Location to) {
        toLocation = to;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean isOnGroundReally() {
        return onGroundReally;
    }

    public ClientBlock isOnClientBlock() {
        AABB feet = new AABB(getTo().toVector().add(new Vector(-0.3, -0.02, -0.3)), getTo().toVector().add(new Vector(0.3, 0, 0.3)));
        AABB aboveFeet = feet.clone();
        aboveFeet.translate(new Vector(0, 0.020001, 0));
        AABB cube = new AABB(new Vector(0, 0, 0), new Vector(1, 1, 1));
        for(Location loc : pp.getClientBlocks().keySet()) {
            if(!getTo().getWorld().equals(loc.getWorld()))
                continue;
            ClientBlock cBlock = pp.getClientBlocks().get(loc);
            cube.translateTo(loc.toVector());
            if(cBlock.getMaterial().isSolid() && feet.isColliding(cube) && !aboveFeet.isColliding(cube))
                return cBlock;
        }
        return null;
    }

    public boolean isTeleportAccept() {
        return teleportAccept;
    }

    public Location getCancelLocation() {
        return cancelLocation;
    }

    public boolean hasDeltaPos() {
        return getTo().getX() != getFrom().getX() || getTo().getY() != getFrom().getY() || getTo().getZ() != getFrom().getZ();
    }

    public boolean hasDeltaRot() {
        return getTo().getYaw() != getFrom().getYaw() || getTo().getPitch() != getFrom().getPitch();
    }

    //Remember: even though these methods indicate whether this move has an updated pos/rot, that
    //doesn't mean that the pos/rot actually changed.
    public boolean isUpdatePos() {
        return updatePos;
    }

    public boolean isUpdateRot() {
        return updateRot;
    }

    public boolean hasAcceptedKnockback() {
        return acceptedKnockback != null;
    }

    public Vector getAcceptedKnockback() {
        return acceptedKnockback;
    }

    public boolean hasFailedKnockback() {
        return failedKnockback;
    }

    public boolean hasHitSlowdown() {
        return hitSlowdown;
    }

    public boolean isTouchingBlocks() {
        return boxSidesTouchingBlocks.size() > 0;
    }

    public Set<Direction> getBoxSidesTouchingBlocks() {
        return boxSidesTouchingBlocks;
    }

    public boolean isInWater() {
        return inWater;
    }

    //WARNING: can be spoofed (i.e. hackers can make this return true even while in mid-air)
    //Let GroundSpoof take care of these hackers.
    public boolean isJump() {
        return jumped;
    }

    //this can be trusted
    public boolean isStep() {
        return step;
    }

    public boolean isSlimeBlockBounce() {
        return slimeBlockBounce;
    }

    public boolean isLiquidExit() {
        return liquidExit;
    }

    public Vector getWaterFlowForce() {
        return waterFlowForce;
    }

    //This is the friction that affects this move's velocity.
    //WARNING: can be spoofed (i.e. hackers can make this return ground friction while in air)
    //Let GroundSpoof take care of these hackers.
    public float getFriction() {
        return oldFriction;
    }

    public float getMaxExpectedInputForce() {
        return maxExpectedInputForce;
    }

    public float getMaxExpectedInputForceNoItemUse() {
        return maxExpectedInputForceNoItemUse;
    }

    public boolean isNextSlimeBlockBounce() {
        return nextIsSlimeBlockBounce;
    }

    public boolean isPossiblePistonPush() {
        return possiblePistonPush;
    }

    public boolean isTouchingCeiling() {
        return touchingCeiling;
    }

    //Resync permits only a maximum of 1 rubberband per move
    @Override
    public void resync() {
        if (cancelLocation == null) {
            if(isOnGroundReally()) {
                pp.setAltSetbackLoc(null);
                cancelLocation = p.getLocation();
            }
            else if(pp.getAltSetbackLoc() == null) {
                pp.setAltSetbackLoc(p.getLocation());
                cancelLocation = p.getLocation();
            }
            else {
                cancelLocation = pp.getAltSetbackLoc();
            }
            setCancelled(true);
            pp.setTeleporting(true);
            pp.setTeleportLoc(cancelLocation);
        }
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        if(!cancelled) {
            cancelLocation = null;
        }
    }

}
