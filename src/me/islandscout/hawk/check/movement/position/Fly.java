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

import me.islandscout.hawk.util.*;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.entity.EntityNMS;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * In vanilla Minecraft, a free-falling player must fall a
 * specific distance for every succeeding move. Hawk's flight
 * check attempts to enforce this vanilla mechanic to prevent
 * players from using flight modifications.
 * <p>
 * For every succeeding move a free-falling player is in the
 * air, the player's vertical velocity is:
 * <p>
 * (v_(n-1) - 0.08) * 0.98
 * <p>
 * A continuous function which describes a free-falling player's
 * vertical velocity given the amount of ticks passed is:
 * <p>
 * v(x) = (3.92 + v_i) * 0.98^x - 3.92
 * <p>
 * A continuous function which describes a free-falling player's
 * vertical position given the amount of ticks passed is:
 * <p>
 * p(x) = -3.92(x+1) - 0.98^(x+1) * 50(3.92 + v_i) + 50(3.92 + v_i) + p_i
 */
public class Fly extends MovementCheck implements Listener {

    //TODO: Please. Just rewrite this.

    //TODO: I suggest getting rid of map lastDeltaY and instead use HawkPlayer#getVelocity(). Should keep things consistent, especially when moving from liquids to air.

    //TODO: false flag with pistons
    //TODO: false flag while jumping down stairs
    //TO DO: false flag when jumping on edge of block. Perhaps extrapolate next "noPos" moves until they touch the block, then reset expectedDeltaY
    //TODO: BYPASS! You can fly over fences. Jump, then toggle fly, then walk straight.
    //Don't change how you determine if on ground, even though that's what caused this. Instead, check when landing when deltaY > 0
    //perhaps check if player's jump height is great enough?
    //TODO: You need to support "insignificant" moves

    private final Map<UUID, Double> lastDeltaY;
    private final Map<UUID, Location> legitLoc;
    private final Set<UUID> inAir;
    private final Map<UUID, Integer> stupidMoves;
    //private Map<UUID, List<Location>> locsOnPBlocks;
    private final Map<UUID, List<Pair<Double, Long>>> velocities; //launch velocities
    private final Set<UUID> failedSoDontUpdateRubberband; //Update rubberband loc until someone fails. In this case, do not update until they touch the ground.
    private static final int STUPID_MOVES = 1; //Apparently you can jump in midair right as you fall off the edge of a block. You need to time it right.

    public Fly() {
        super("fly", true, 0, 10, 0.995, 5000, "%player% failed fly. VL: %vl%", null);
        lastDeltaY = new HashMap<>();
        inAir = new HashSet<>();
        legitLoc = new HashMap<>();
        stupidMoves = new HashMap<>();
        //locsOnPBlocks = new HashMap<>();
        velocities = new HashMap<>();
        failedSoDontUpdateRubberband = new HashSet<>();
    }

    @Override
    protected void check(MoveEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        double deltaY = event.getTo().getY() - event.getFrom().getY();
        if (pp.hasFlyPending() && p.getAllowFlight())
            return;
        if (!event.isOnGroundReally() && !p.isFlying() && !p.isInsideVehicle() && !pp.isSwimming() && !p.isSleeping() &&
                !isInClimbable(event.getTo()) && !isOnBoat(p, event.getTo())) {

            if (!inAir.contains(p.getUniqueId()) && deltaY > 0)
                lastDeltaY.put(p.getUniqueId(), 0.42 + getJumpBoostLvl(p) * 0.1);

            //handle any pending knockbacks
            if(event.hasAcceptedKnockback())
                lastDeltaY.put(p.getUniqueId(), deltaY);

            if(event.isSlimeBlockBounce())
                lastDeltaY.put(p.getUniqueId(), deltaY);

            double expectedDeltaY = lastDeltaY.getOrDefault(p.getUniqueId(), 0D);
            double epsilon = 0.03;

            //lastDeltaY.put(p.getUniqueId(), (lastDeltaY.getOrDefault(p.getUniqueId(), 0D) - 0.025) * 0.8); //water function
            if (AdjacentBlocks.matIsAdjacent(event.getTo(), Material.WEB)) {
                lastDeltaY.put(p.getUniqueId(), -0.007);
                epsilon = 0.000001;
                if (AdjacentBlocks.onGroundReally(event.getTo().clone().add(0, -0.03, 0), -1, false, 0.02))
                    return;
            } else if(!pp.isInLiquid() && event.isInLiquid()) {
                //entering liquid
                lastDeltaY.put(p.getUniqueId(), (lastDeltaY.getOrDefault(p.getUniqueId(), 0D) * 0.98) - 0.038399);
            } else {
                //in air
                lastDeltaY.put(p.getUniqueId(), (lastDeltaY.getOrDefault(p.getUniqueId(), 0D) - 0.08) * 0.98);
            }


            //handle teleport
            if (event.hasTeleported()) {
                lastDeltaY.put(p.getUniqueId(), 0D);
                expectedDeltaY = 0;
                legitLoc.put(p.getUniqueId(), event.getTo());
            }

            if (deltaY - expectedDeltaY > epsilon && event.hasDeltaPos()) { //oopsie daisy. client made a goof up

                //wait one little second: minecraft is being a pain in the ass and it wants to play tricks when you parkour on the very edge of blocks
                //we need to check this first...
                if (deltaY < 0) {
                    Location checkLoc = event.getFrom().clone();
                    checkLoc.setY(event.getTo().getY());
                    if (AdjacentBlocks.onGroundReally(checkLoc, deltaY, false, 0.02)) {
                        onGroundStuff(p);
                        return;
                    }
                    //extrapolate move BEFORE getFrom, then check
                    checkLoc.setY(event.getFrom().getY());
                    checkLoc.setX(checkLoc.getX() - (event.getTo().getX() - event.getFrom().getX()));
                    checkLoc.setZ(checkLoc.getZ() - (event.getTo().getZ() - event.getFrom().getZ()));
                    if (AdjacentBlocks.onGroundReally(checkLoc, deltaY, false, 0.02)) {
                        onGroundStuff(p);
                        return;
                    }
                }

                if(event.isOnClientBlock() != null) {
                    onGroundStuff(p);
                    return;
                }

                //scold the child
                punish(pp, false, event);
                tryRubberband(event, legitLoc.getOrDefault(p.getUniqueId(), p.getLocation()));
                lastDeltaY.put(p.getUniqueId(), canCancel() ? 0 : deltaY);
                failedSoDontUpdateRubberband.add(p.getUniqueId());
                return;
            }

            reward(pp);

            //the player is in air now, since they have a positive Y velocity and they're not on the ground
            if (inAir.contains(p.getUniqueId()))
                //upwards now
                stupidMoves.put(p.getUniqueId(), 0);

            //handle stupid moves, because the client tends to want to jump a little late if you jump off the edge of a block
            if (stupidMoves.getOrDefault(p.getUniqueId(), 0) >= STUPID_MOVES || (deltaY > 0 && AdjacentBlocks.onGroundReally(event.getFrom(), -1, true, 0.02)))
                //falling now
                inAir.add(p.getUniqueId());
            stupidMoves.put(p.getUniqueId(), stupidMoves.getOrDefault(p.getUniqueId(), 0) + 1);
        } else {
            onGroundStuff(p);
        }

        if (!failedSoDontUpdateRubberband.contains(p.getUniqueId()) || event.isOnGroundReally()) {
            legitLoc.put(p.getUniqueId(), p.getLocation());
            failedSoDontUpdateRubberband.remove(p.getUniqueId());
        }

    }

    private void onGroundStuff(Player p) {
        lastDeltaY.put(p.getUniqueId(), 0D);
        inAir.remove(p.getUniqueId());
        stupidMoves.put(p.getUniqueId(), 0);
    }

    private boolean isOnBoat(Player p, Location loc) {
        Set<Entity> trackedEntities = hawk.getLagCompensator().getPositionTrackedEntities();
        int ping = ServerUtils.getPing(p);
        for(Entity entity : trackedEntities) {
            if (entity instanceof Boat) {
                AABB boatBB = EntityNMS.getEntityNMS(entity).getCollisionBox(hawk.getLagCompensator().getHistoryLocation(ping, entity).toVector());
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

    private int getJumpBoostLvl(Player p) {
        for (PotionEffect pEffect : p.getActivePotionEffects()) {
            if (pEffect.getType().equals(PotionEffectType.JUMP)) {
                return pEffect.getAmplifier() + 1;
            }
        }
        return 0;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        legitLoc.put(pp.getUuid(), e.getTo());
    }

    @Override
    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        lastDeltaY.remove(uuid);
        inAir.remove(uuid);
        legitLoc.remove(uuid);
        stupidMoves.remove(uuid);
        velocities.remove(uuid);
        failedSoDontUpdateRubberband.remove(uuid);
    }
}
