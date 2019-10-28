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
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NewFly extends MovementCheck {

    //WATER (if inWater):
    //move Y position by dY
    //SEND POSITION UPDATE
    //update inWater
    //dY *= 0.8
    //dY -= 0.02

    //LAVA (if in lava):
    //move Y position by dY
    //dY *= 0.5
    //dY -= 0.02

    //AIR:
    //if inWeb, dY *= 0.05
    //if kb, dY = kb_Y
    //if jump, dY = 0.42
    //move Y position by dY
    //if inWeb, dY = 0
    //SEND POSITION UPDATE
    //test for block collision (update inWeb status, cactus damage, soulsand velocity multiplier, etc)
    //dY -= 0.08
    //dY *= 0.98

    private static final float MIN_VELOCITY = 0.005F;
    private static final int MAX_NO_MOVES = 8;
    private static final float DISCREPANCY_THRESHOLD = 0.0001F;

    private final Map<UUID, Float> estimatedPositionMap;
    private final Map<UUID, Float> estimatedVelocityMap;
    private final Map<UUID, Integer> noMovesMap;

    public NewFly() {
        super("newfly", "&7%player% failed fly, VL: %vl%");
        estimatedPositionMap = new HashMap<>();
        estimatedVelocityMap = new HashMap<>();
        noMovesMap = new HashMap<>();
    }

    @Override
    protected void check(MoveEvent e) {

        HawkPlayer pp = e.getHawkPlayer();
        Player p = e.getPlayer();
        float dY = (float) (e.getTo().getY() - e.getFrom().getY());
        boolean moved = dY != 0F;
        int noMoves = noMovesMap.getOrDefault(pp.getUuid(), 0);
        float estimatedPosition = estimatedPositionMap.getOrDefault(pp.getUuid(), (float)e.getFrom().getY());
        float prevEstimatedVelocity = estimatedVelocityMap.getOrDefault(pp.getUuid(), (float) pp.getVelocity().getY());
        Set<Material> touchedBlocks = WrappedEntity.getWrappedEntity(p).getCollisionBox(e.getFrom().toVector()).getMaterials(pp.getWorld());

        //TODO false flag when toggling off fly
        if(!e.isOnGround() && !e.isJump() && !e.hasAcceptedKnockback() && !e.hasTeleported() && !e.isStep() &&
                !p.isInsideVehicle() && !(pp.hasFlyPending() && p.getAllowFlight()) &&
                !p.isFlying() && !pp.isSwimming() && !p.isSleeping() && !isInClimbable(e.getFrom()) && //TODO: uh oh! make sure to have a fastladder check, otherwise hackers can "pop" off them
                !isOnBoat(p, e.getTo()) && !e.isSlimeBlockBounce()) { //TODO validate onGround w/ GroundSpoof check & dont forget to check when player lands

            //count "no-moves"
            if(!moved) {
                noMoves++;
            }
            else {
                noMoves = 0;
            }

            //compute next expected velocity
            float estimatedVelocity;
            if (touchedBlocks.contains(Material.WEB)) {
                estimatedVelocity = -0.00392F; //TODO: find the function
            }
            else if(pp.isInLiquid()) { //TODO fix this. (entering liquid)
                estimatedVelocity = (prevEstimatedVelocity * 0.98F) - 0.0784F;
            }
            else {
                estimatedVelocity = (prevEstimatedVelocity - 0.08F) * 0.98F;
            }

            //add expected velocity to expected position
            if(Math.abs(estimatedVelocity) < MIN_VELOCITY || pp.getCurrentTick() - pp.getLastTeleportAcceptTick() < 2)
                estimatedVelocity = 0F;
            estimatedPosition += estimatedVelocity;

            //check if hit head
            boolean hitHead = e.getBoxSidesTouchingBlocks().contains(Direction.TOP);
            boolean hasHitHead = pp.getBoxSidesTouchingBlocks().contains(Direction.TOP);
            if(e.getTo().getY() < estimatedPosition && (hitHead && !hasHitHead)) {
                estimatedPosition = (float) e.getTo().getY();
                estimatedVelocity = 0;
            }

            //finally, check for discrepancy
            if(moved || noMoves > MAX_NO_MOVES) {
                float discrepancy = (float) e.getTo().getY() - estimatedPosition;
                if(Math.abs(discrepancy) > DISCREPANCY_THRESHOLD) {
                    punishAndTryRubberband(pp, e, e.getPlayer().getLocation());
                }
                else {
                    reward(pp);
                }
            }

            prevEstimatedVelocity = estimatedVelocity;
        }
        else {
            estimatedPosition = (float) e.getTo().getY();
            if(e.isOnGround() || (e.getBoxSidesTouchingBlocks().contains(Direction.TOP) && dY > 0)) {
                prevEstimatedVelocity = 0;
            }
            else {
                prevEstimatedVelocity = dY;
            }
        }

        estimatedVelocityMap.put(pp.getUuid(), prevEstimatedVelocity);
        noMovesMap.put(pp.getUuid(), noMoves);
        estimatedPositionMap.put(pp.getUuid(), estimatedPosition);
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
}
