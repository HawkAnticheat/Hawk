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
import me.islandscout.hawk.wrap.block.WrappedBlock;
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class Fly extends MovementCheck {

    //TODO perhaps have a ground checking mode? clientside vs serverside? doing it server side can allow GroundSpoof to not cancel, yet Gravity will still catch groundspoof-based flys

    private static final float MIN_VELOCITY = 0.005F;
    private static final int MAX_NO_MOVES = 20;
    private static final double NO_MOVE_THRESHOLD = 0.03;
    private static final float DISCREPANCY_THRESHOLD = 0.0001F;

    private final Map<UUID, Float> estimatedPositionMap;
    private final Map<UUID, Float> estimatedVelocityMap;
    private final Map<UUID, Float> estimatedPositionAltMap;
    private final Map<UUID, Float> estimatedVelocityAltMap;
    private final Map<UUID, Integer> noMovesMap;
    private final Map<UUID, Integer> ticksSinceNoPosUpdateMap;
    private final Set<UUID> wasFlyingSet;
    private final Set<UUID> wasInLavaSet;

    public Fly() {
        super("fly", true, 0, 10, 0.995, 5000, "&7%player% failed fly, VL: %vl%", null);
        estimatedPositionMap = new HashMap<>();
        estimatedVelocityMap = new HashMap<>();
        estimatedPositionAltMap = new HashMap<>();
        estimatedVelocityAltMap = new HashMap<>();
        noMovesMap = new HashMap<>();
        ticksSinceNoPosUpdateMap = new HashMap<>();
        wasFlyingSet = new HashSet<>();
        wasInLavaSet = new HashSet<>();
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        Player p = e.getPlayer();

        Vector from = pp.hasSentPosUpdate() ? e.getFrom().toVector() : pp.getPositionPredicted(); //must predict where a player was previously if they didn't send a pos update then.

        float dY = (float) (e.getTo().getY() - from.getY());

        boolean moved = e.isUpdatePos();

        int noMoves = noMovesMap.getOrDefault(pp.getUuid(), 0);
        int ticksSinceNoPosUpdate = ticksSinceNoPosUpdateMap.getOrDefault(pp.getUuid(), 0);

        float estimatedPosition = estimatedPositionMap.getOrDefault(pp.getUuid(), (float) from.getY()),
              prevEstimatedVelocity = estimatedVelocityMap.getOrDefault(pp.getUuid(), (float) pp.getVelocity().getY()),
              estimatedPositionAlt = estimatedPositionAltMap.getOrDefault(pp.getUuid(), (float) from.getY()),
              prevEstimatedVelocityAlt = estimatedVelocityAltMap.getOrDefault(pp.getUuid(), (float) pp.getVelocity().getY());

        Set<Material> touchedBlocks = WrappedEntity.getWrappedEntity(p).getCollisionBox(from).getMaterials(pp.getWorld());

        boolean opposingForce = e.isJump() || e.hasAcceptedKnockback() || e.isTeleportAccept() || e.isStep();

        boolean wasInLava = wasInLavaSet.contains(p.getUniqueId());

        if((!e.isOnGround() || !pp.isOnGround()) && !opposingForce && !e.isLiquidExit() &&
                !p.isInsideVehicle() && !pp.isFlying() && !wasFlyingSet.contains(p.getUniqueId()) &&
                !p.isSleeping() && !isInClimbable(from.toLocation(pp.getWorld())) && //TODO: uh oh! make sure to have a fastladder check, otherwise hackers can "pop" off them
                !isOnBoat(p, e.getTo())) {

            //count "no-moves"
            if(!moved) {
                noMoves++;
            } else {
                noMoves = 0;
            }

            float estimatedVelocity;
            float estimatedVelocityAlt;
            boolean velResetA = false;
            boolean velResetB = false;

            //compute next expected velocity
            //TODO make this prediction better, i.e. do liquid/web collision checks. It shouldn't be using the liquid functions when the predicted position gets out of water.
            if(pp.isSwimming()) { //water functions

                estimatedVelocity = (prevEstimatedVelocity * 0.8F) + (float)(-0.02 + computeVerticalWaterFlowForce(estimatedPosition, pp));
                estimatedVelocityAlt = (prevEstimatedVelocityAlt * 0.8F) + (float)(-0.02 + computeVerticalWaterFlowForce(estimatedPositionAlt, pp));

                if(Math.abs(estimatedVelocity) < MIN_VELOCITY) {
                    estimatedVelocity = 0;
                }
                if(Math.abs(estimatedVelocityAlt) < MIN_VELOCITY) {
                    estimatedVelocityAlt = 0;
                }
                estimatedVelocityAlt += 0.04; //add swimming-up force
                if(touchedBlocks.contains(Material.WEB)) {
                    estimatedVelocity *= 0.05;
                    estimatedVelocityAlt *= 0.05;
                }
            } else if(wasInLava) { //lava functions

                estimatedVelocity = (prevEstimatedVelocity * 0.5F) - 0.02F;
                estimatedVelocityAlt = (prevEstimatedVelocityAlt * 0.5F) - 0.02F;

                if(Math.abs(estimatedVelocity) < MIN_VELOCITY) {
                    estimatedVelocity = 0;
                }
                if(Math.abs(estimatedVelocityAlt) < MIN_VELOCITY) {
                    estimatedVelocityAlt = 0;
                }
                estimatedVelocityAlt += 0.04; //add swimming-up force
                if(touchedBlocks.contains(Material.WEB)) {
                    estimatedVelocity *= 0.05;
                    estimatedVelocityAlt *= 0.05;
                }
            } else { //air function

                estimatedVelocity = estimatedVelocityAlt = (prevEstimatedVelocity - 0.08F) * 0.98F;

                if(Math.abs(estimatedVelocity) < MIN_VELOCITY) {
                    estimatedVelocity = 0;
                }
                if(Math.abs(estimatedVelocityAlt) < MIN_VELOCITY) {
                    estimatedVelocityAlt = 0;
                }
                if(touchedBlocks.contains(Material.WEB)) {
                    estimatedVelocity *= 0.05;
                    estimatedVelocityAlt *= 0.05;
                }

                if(pp.isInWater() || pp.isInLava()) { //Entering liquid. You could take two paths depending if you're holding the jump button or not.

                    estimatedVelocity = (prevEstimatedVelocity + (float)computeVerticalWaterFlowForce(estimatedPosition, pp) - 0.08F) * 0.98F;
                    estimatedVelocity += computeVerticalWaterFlowForce(estimatedPosition, pp);

                    estimatedVelocityAlt = (prevEstimatedVelocityAlt + (float)computeVerticalWaterFlowForce(estimatedPositionAlt, pp) - 0.08F) * 0.98F;
                    estimatedVelocityAlt += computeVerticalWaterFlowForce(estimatedPositionAlt, pp);

                    if(Math.abs(estimatedVelocity) < MIN_VELOCITY) {
                        estimatedVelocity = 0;
                    }
                    if(Math.abs(estimatedVelocityAlt) < MIN_VELOCITY) {
                        estimatedVelocityAlt = 0;
                    }
                    estimatedVelocityAlt += 0.04; //add swimming-up force
                    if(touchedBlocks.contains(Material.WEB)) {
                        estimatedVelocity *= 0.05;
                        estimatedVelocityAlt *= 0.05;
                    }
                }
            }

            //handle teleport
            if (pp.getCurrentTick() - pp.getLastTeleportAcceptTick() < 2) {
                estimatedVelocity = 0;
                estimatedVelocityAlt = 0;
                velResetA = true;
                velResetB = true;
            }

            //add velocities to expected positions
            estimatedPositionAlt += estimatedVelocityAlt;
            estimatedPosition += estimatedVelocity;

            //check if hit head
            boolean hasHitHead = pp.getBoxSidesTouchingBlocks().contains(Direction.TOP);
            if(e.getTo().getY() < estimatedPosition && (e.isTouchingCeiling() && !hasHitHead)) { //standard pos/vel
                estimatedPosition = (float) e.getTo().getY();
                estimatedVelocity = 0;
                velResetA = true;
            }

            //check if hit head while swimming
            if(e.getTo().getY() < estimatedPositionAlt && e.getTo().getY() > estimatedPosition &&
                    (pp.isInWater() || pp.isInLava() || pp.isSwimming()) && (e.isTouchingCeiling() || hasHitHead)) { //alt. pos/vel
                estimatedPositionAlt = (float) e.getTo().getY();
                estimatedVelocityAlt = 0;
                velResetB = true;
            }

            //check landing
            if(e.isOnGround() && !pp.isOnGround()) {
                //Pretty much check if the Y is within reasonable bounds.
                //Doesn't need to be so precise i.e. 0.0001 within bounds, leave that for GroundSpoof and Phase.
                //However, if it becomes a problem, you know how to expand this code.
                float y;

                if(moved) {
                    y = (float) e.getTo().getY();
                } else {
                    y = (float) pp.getPositionPredicted().getY();
                }

                double fromY = MathPlus.round(from.getY(), 10);

                float discrepancy = y - estimatedPosition;
                //y must be between last Y and estimatedPosition.
                if (Math.abs(discrepancy) > DISCREPANCY_THRESHOLD && !e.isPossiblePistonPush() &&
                        (y < Math.min(estimatedPosition, fromY) || y > Math.max(estimatedPosition, fromY))) {
                    punishAndTryRubberband(pp, e);
                } else {
                    reward(pp);
                }

                //If you've landed, then that must mean these should reset.
                estimatedPosition = y;
                estimatedPositionAlt = y;

                if(e.isNextSlimeBlockBounce()) {
                    prevEstimatedVelocity = -estimatedVelocity;
                } else {
                    prevEstimatedVelocity = 0;
                }

                prevEstimatedVelocityAlt = prevEstimatedVelocity;
            }

            //check for Y discrepancy in air
            else {

                //bool alt determines if we're using the alternate path for comparison
                boolean alt;

                if(moved || noMoves > MAX_NO_MOVES || (Math.abs(estimatedPosition - e.getTo().getY()) > NO_MOVE_THRESHOLD && Math.abs(estimatedPositionAlt - e.getTo().getY()) > NO_MOVE_THRESHOLD)) {

                    float discrepancy;

                    if(ticksSinceNoPosUpdate < 2 && noMoves <= MAX_NO_MOVES) {
                        //Handle stupid-moves with a range check.
                        //Honestly, I don't care about an error of 0.03 in liquids while the client isn't updating its position.
                        float y = (float) e.getTo().getY();
                        float max = Math.max(estimatedPositionAlt, estimatedPosition);
                        float min = Math.min(estimatedPositionAlt, estimatedPosition);
                        if(y > max) {
                            discrepancy = y - max;
                        } else if(y < min) {
                            discrepancy = y - min;
                        } else {
                            discrepancy = 0;
                        }
                    } else {
                        float discrepancyA = (float) e.getTo().getY() - estimatedPosition;
                        float discrepancyB = (float) e.getTo().getY() - estimatedPositionAlt;
                        //choose discrepancy closer to 0
                        alt = Math.abs(discrepancyA) - Math.abs(discrepancyB) > 0;
                        discrepancy = alt ? discrepancyB : discrepancyA;
                    }

                    boolean onSlimeBlock;
                    if(Hawk.getServerVersion() > 7) {
                        //-0.1 is arbitrary
                        Block block = ServerUtils.getBlockAsync(e.getFrom().clone().add(0, -0.2, 0));
                        onSlimeBlock = block != null && block.getType() == Material.SLIME_BLOCK
                                && Math.abs(dY) < 0.1;
                    } else {
                        onSlimeBlock = false;
                    }

                    if(Math.abs(discrepancy) > DISCREPANCY_THRESHOLD && !e.isPossiblePistonPush() && !onSlimeBlock) {
                        punishAndTryRubberband(pp, e);
                    }
                    else {
                        reward(pp);
                    }
                }

                //TODO limit these estimatedVelocities to 0.03 if this is a no-move. Likewise, limit the estimated positions, too.

                if(moved) {
                    //we can use these for next move, since the client has sent a pos update
                    estimatedPosition = estimatedPositionAlt = (float) e.getTo().getY();
                    if(ticksSinceNoPosUpdate > 0) {
                        estimatedVelocity = velResetA ? estimatedVelocity : dY;
                        estimatedVelocityAlt = velResetB ? estimatedVelocityAlt : dY;
                    }
                }

                //keep estimating
                if (touchedBlocks.contains(Material.WEB)) {
                    prevEstimatedVelocity = prevEstimatedVelocityAlt = 0;
                } else {
                    prevEstimatedVelocity = estimatedVelocity;
                    prevEstimatedVelocityAlt = estimatedVelocityAlt;
                }
            }
        } else {
            if(moved) {
                estimatedPosition = estimatedPositionAlt = (float) e.getTo().getY();
            } else {
                estimatedPosition = estimatedPositionAlt = (float) pp.getPositionPredicted().getY();
            }

            if(e.isOnGround() || (e.isTouchingCeiling() && dY > 0) || touchedBlocks.contains(Material.WEB)) {
                prevEstimatedVelocity = prevEstimatedVelocityAlt = 0;
            } else {
                prevEstimatedVelocity = prevEstimatedVelocityAlt = dY;
            }
        }
        //Debug.broadcastMessage((moved ? ChatColor.GREEN : ChatColor.RED) + "" + e.getTo().getY() + " " + estimatedPosition + " " + pp.getPositionPredicted().getY() + " prevEstDY: " + prevEstimatedVelocity);

        estimatedVelocityMap.put(pp.getUuid(), prevEstimatedVelocity);
        estimatedVelocityAltMap.put(pp.getUuid(), prevEstimatedVelocityAlt);
        estimatedPositionMap.put(pp.getUuid(), estimatedPosition);
        estimatedPositionAltMap.put(pp.getUuid(), estimatedPositionAlt);

        noMovesMap.put(pp.getUuid(), noMoves);

        if(!moved) {
            ticksSinceNoPosUpdate = 0;
        } else {
            ticksSinceNoPosUpdate++;
        }

        ticksSinceNoPosUpdateMap.put(p.getUniqueId(), ticksSinceNoPosUpdate);

        if(pp.isFlying()) {
            wasFlyingSet.add(p.getUniqueId());
        } else {
            wasFlyingSet.remove(p.getUniqueId());
        }

        if(pp.isInLava()) {
            wasInLavaSet.add(p.getUniqueId());
        } else {
            wasInLavaSet.remove(p.getUniqueId());
        }
    }

    private boolean isOnBoat(Player p, Location loc) {
        Set<Entity> trackedEntities = hawk.getLagCompensator().getPositionTrackedEntities();
        int ping = ServerUtils.getPing(p);
        for(Entity entity : trackedEntities) {
            if (entity instanceof Boat) {
                AABB boatBB = WrappedEntity.getWrappedEntity(entity).getCollisionBox(hawk.getLagCompensator().getHistoryLocation(ping, entity).toVector());
                AABB feet = new AABB(
                        new Vector(-0.3, -0.4, -0.3).add(loc.toVector()),
                        new Vector(0.3, 0, 0.3).add(loc.toVector()));
                feet.expand(0.5, 0.5, 0.5);
                if (feet.isColliding(boatBB))
                    return true;
            }
        }
        return false;
    }

    private boolean isInClimbable(Location loc) {
        Block b = ServerUtils.getBlockAsync(loc);
        return b != null && (b.getType() == Material.VINE || b.getType() == Material.LADDER);
    }

    private double computeVerticalWaterFlowForce(double y, HawkPlayer pp) {
        Vector vec = new Vector();
        AABB liquidTest = AABB.playerWaterCollisionBox.clone();
        liquidTest.translate(new Vector(pp.getPosition().getX(), y, pp.getPosition().getZ()));
        List<Block> blocks = liquidTest.getBlocks(pp.getPlayer().getWorld());
        Vector waterFlow = new Vector();
        for(Block b : blocks) {
            if(b.getType() == Material.WATER || b.getType() == Material.STATIONARY_WATER) {
                //game version doesn't really matter for this
                Vector direction = WrappedBlock.getWrappedBlock(b, 8).getFlowDirection();
                waterFlow.add(direction);
            }
        }
        if(waterFlow.lengthSquared() > 0 && !pp.isFlying()) {
            waterFlow.normalize();
            waterFlow.multiply(0.014);
            vec.add(waterFlow);
        }
        return vec.getY();
    }


    @Override
    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        estimatedPositionMap.remove(uuid);
        estimatedVelocityMap.remove(uuid);
        estimatedPositionAltMap.remove(uuid);
        estimatedVelocityAltMap.remove(uuid);
        noMovesMap.remove(uuid);
        ticksSinceNoPosUpdateMap.remove(uuid);
        wasFlyingSet.remove(uuid);
    }
}
