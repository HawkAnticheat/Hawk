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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class Gravity extends MovementCheck {

    //TODO do not forget checkerclimb. Blocks within 0.3 should be treated as AIR unless they are in HawkPlayer's collision ignore list
    //TODO perhaps have a ground checking mode? clientside vs serverside? doing it server side can allow GroundSpoof to not cancel, yet Gravity will still catch groundspoof-based flys

    private static final float MIN_VELOCITY = 0.005F;
    private static final int MAX_NO_MOVES = 20;
    private static final double NO_MOVE_THRESHOLD = 0.03;
    private static final float DISCREPANCY_THRESHOLD = 0.0001F;

    private final Map<UUID, Float> estimatedPositionMap;
    private final Map<UUID, Float> estimatedVelocityMap;
    private final Map<UUID, Integer> noMovesMap;
    private final Map<UUID, Location> legitLoc;
    private final Set<UUID> wasFlyingSet;
    private final Set<UUID> wasInLavaSet;

    public Gravity() {
        super("gravity", true, 0, 10, 0.995, 5000, "&7%player% failed gravity, VL: %vl%", null);
        estimatedPositionMap = new HashMap<>();
        estimatedVelocityMap = new HashMap<>();
        noMovesMap = new HashMap<>();
        legitLoc = new HashMap<>();
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

        float estimatedPosition = estimatedPositionMap.getOrDefault(pp.getUuid(), (float) from.getY()),
                prevEstimatedVelocity = estimatedVelocityMap.getOrDefault(pp.getUuid(), (float) pp.getVelocity().getY());

        Set<Material> touchedBlocks = WrappedEntity.getWrappedEntity(p).getCollisionBox(from).getMaterials(pp.getWorld());

        boolean opposingForce = e.isJump() || e.hasAcceptedKnockback() || e.hasTeleported() || e.isStep();

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
            if (touchedBlocks.contains(Material.WEB)) { //web function
                estimatedVelocity = ((prevEstimatedVelocity * 0.98F) - (0.08F * 0.98F)) * 0.05F;
                estimatedVelocityAlt = estimatedVelocity;
            } else if(pp.isSwimming()) { //water functions
                estimatedVelocity = estimatedVelocityAlt = (prevEstimatedVelocity * 0.8F) + (float)(-0.02 + pp.getWaterFlowForce().getY());
                if(Math.abs(estimatedVelocity) < MIN_VELOCITY) {
                    estimatedVelocity = 0;
                }
                if(Math.abs(estimatedVelocityAlt) < MIN_VELOCITY) {
                    estimatedVelocityAlt = 0;
                }
                estimatedVelocityAlt += 0.04; //add swimming-up force
            } else if(wasInLava) { //lava functions
                estimatedVelocity = estimatedVelocityAlt = (prevEstimatedVelocity * 0.5F) - 0.02F;
                if(Math.abs(estimatedVelocity) < MIN_VELOCITY) {
                    estimatedVelocity = 0;
                }
                if(Math.abs(estimatedVelocityAlt) < MIN_VELOCITY) {
                    estimatedVelocityAlt = 0;
                }
                estimatedVelocityAlt += 0.04; //add swimming-up force
            } else { //air function
                estimatedVelocity = (prevEstimatedVelocity - 0.08F) * 0.98F;
                estimatedVelocityAlt = estimatedVelocity;
                if(Math.abs(estimatedVelocity) < MIN_VELOCITY) {
                    estimatedVelocity = 0;
                }
                if(Math.abs(estimatedVelocityAlt) < MIN_VELOCITY) {
                    estimatedVelocityAlt = 0;
                }
                if(pp.isInWater() || pp.isInLava()) { //Entering liquid. You could take two paths depending if you're holding the jump button or not.
                    estimatedVelocity = (prevEstimatedVelocity + (float)pp.getWaterFlowForce().getY() - 0.08F) * 0.98F;
                    estimatedVelocity += pp.getWaterFlowForce().getY();
                    estimatedVelocityAlt = estimatedVelocity;
                    if(Math.abs(estimatedVelocity) < MIN_VELOCITY) {
                        estimatedVelocity = 0;
                    }
                    if(Math.abs(estimatedVelocityAlt) < MIN_VELOCITY) {
                        estimatedVelocityAlt = 0;
                    }
                    estimatedVelocityAlt += 0.04;
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
            float estimatedPositionAlt = estimatedPosition + estimatedVelocityAlt;
            estimatedPosition += estimatedVelocity;


            //check if hit head
            boolean hitHead = e.getBoxSidesTouchingBlocks().contains(Direction.TOP),
                    hasHitHead = pp.getBoxSidesTouchingBlocks().contains(Direction.TOP);
            if(e.getTo().getY() < estimatedPosition && (hitHead && !hasHitHead)) { //standard pos/vel
                estimatedPosition = (float) e.getTo().getY();
                estimatedVelocity = 0;
                velResetA = true;
            }

            //check if hit head while swimming
            if(e.getTo().getY() < estimatedPositionAlt && e.getTo().getY() > estimatedPosition &&
                    (pp.isInWater() || pp.isInLava() || pp.isSwimming()) && (hitHead || hasHitHead)) { //alt. pos/vel
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

                float discrepancy = y - estimatedPosition;
                //y must be between last Y and estimatedPosition.
                if (Math.abs(discrepancy) > DISCREPANCY_THRESHOLD && !e.isPossiblePistonPush() &&
                        (y < Math.min(estimatedPosition, from.getY()) || y > Math.max(estimatedPosition, from.getY()))) {
                    punishAndTryRubberband(pp, e);
                } else {
                    reward(pp);
                }

                //If you've landed, then that must mean these should reset.
                estimatedPosition = y;

                if(e.isNextSlimeBlockBounce()) {
                    prevEstimatedVelocity = -estimatedVelocity;
                } else {
                    prevEstimatedVelocity = 0;
                }
            }

            //check for Y discrepancy in air
            else {

                //bool alt determines if we're using the alternate path for comparison
                boolean alt = false;

                if(moved || noMoves > MAX_NO_MOVES || (Math.abs(estimatedPosition - e.getTo().getY()) > NO_MOVE_THRESHOLD && Math.abs(estimatedPositionAlt - e.getTo().getY()) > NO_MOVE_THRESHOLD)) {
                    float discrepancyA = (float) e.getTo().getY() - estimatedPosition;
                    float discrepancyB = (float) e.getTo().getY() - estimatedPositionAlt;

                    //choose discrepancy closer to 0
                    alt = Math.abs(discrepancyA) - Math.abs(discrepancyB) > 0;
                    float discrepancy = alt ? discrepancyB : discrepancyA;

                    if(Math.abs(discrepancy) > DISCREPANCY_THRESHOLD && !e.isPossiblePistonPush()) {
                        punishAndTryRubberband(pp, e);
                    }
                    else {
                        reward(pp);
                    }

                    //we can use these for next move, since the client has sent a pos update and we have already checked it
                    estimatedPosition = (float) e.getTo().getY();
                    if(noMovesMap.getOrDefault(pp.getUuid(), 0) == 0) {
                        estimatedVelocity = velResetA ? estimatedVelocity : dY;
                        estimatedVelocityAlt = velResetB ? estimatedVelocityAlt : dY;
                    }
                }

                if (touchedBlocks.contains(Material.WEB)) {
                    prevEstimatedVelocity = 0;
                } else {
                    //keep estimating
                    prevEstimatedVelocity = alt ? estimatedVelocityAlt : estimatedVelocity;
                }
            }
        } else {
            if(moved) {
                estimatedPosition = (float) e.getTo().getY();
            } else {
                estimatedPosition = (float) pp.getPositionPredicted().getY();
            }

            if(e.isOnGround() || (e.getBoxSidesTouchingBlocks().contains(Direction.TOP) && dY > 0)) {
                prevEstimatedVelocity = 0;
            } else {
                prevEstimatedVelocity = dY;
            }
        }
        //Debug.broadcastMessage((moved ? ChatColor.GREEN : ChatColor.RED) + "" + e.getTo().getY() + " " + estimatedPosition + " " + pp.getPositionPredicted().getY() + " prevEstDY: " + prevEstimatedVelocity);

        estimatedVelocityMap.put(pp.getUuid(), prevEstimatedVelocity);
        noMovesMap.put(pp.getUuid(), noMoves);
        estimatedPositionMap.put(pp.getUuid(), estimatedPosition);
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

    @Override
    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        estimatedPositionMap.remove(uuid);
        estimatedVelocityMap.remove(uuid);
        noMovesMap.remove(uuid);
        legitLoc.remove(uuid);
        wasFlyingSet.remove(uuid);
    }
}
