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
import me.islandscout.hawk.util.AABB;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.Direction;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NewFly extends MovementCheck {

    private static final float MIN_VELOCITY = 0.005F;
    private static final int MAX_NO_MOVES = 2;

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
        float prevDY = (float) pp.getVelocity().getY();
        float dY = (float) (e.getTo().getY() - e.getFrom().getY());
        boolean moved = dY != 0F;
        int noMoves = noMovesMap.getOrDefault(pp.getUuid(), 0);
        float estimatedPosition = estimatedPositionMap.getOrDefault(pp.getUuid(), (float)e.getFrom().getY());
        float prevEstimatedVelocity = estimatedVelocityMap.getOrDefault(pp.getUuid(), (float) pp.getVelocity().getY());

        //TODO wtf? teleports cause false flags?

        if(!e.isOnGround() && !e.isJump() && !e.hasAcceptedKnockback() && !e.hasTeleported() && !e.isStep()) { //TODO validate onGround & dont forget to check when player lands & dont forget liquids & dont forget climbables & dont forget to make step check

            //Debug.broadcastMessage("---");

            //TODO handle when player hits their head on roof
            if(pp.getBoxSidesTouchingBlocks().contains(Direction.TOP)) {
                prevEstimatedVelocity = 0;
                estimatedPosition = (float) e.getFrom().getY();
            }

            if(!moved) {
                noMoves++;
                //Debug.broadcastMessage(ChatColor.RED + "nomove");
            }
            else {
                noMoves = 0;
            }

            float estimatedVelocity = (prevEstimatedVelocity - 0.08F) * 0.98F;
            if(Math.abs(estimatedVelocity) < MIN_VELOCITY)
                estimatedVelocity = 0F;

            //Debug.broadcastMessage(ChatColor.YELLOW + "" + estimatedVelocity);

            estimatedPosition += estimatedVelocity;

            if(moved || noMoves > MAX_NO_MOVES) {
                float discrepancy = (float) e.getTo().getY() - estimatedPosition;
                Debug.broadcastMessage(discrepancy);
            }

            //Debug.broadcastMessage(dY);


            prevEstimatedVelocity = estimatedVelocity;
        }
        else {
            estimatedPosition = (float) e.getTo().getY();
            prevEstimatedVelocity = e.getBoxSidesTouchingBlocks().contains(Direction.TOP) ? 0 : dY;
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
