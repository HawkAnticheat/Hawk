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

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.util.packet.WrappedPacket;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class MoveEvent extends Event {

    //Remember that the client only updates position/rotation information if
    //it is significant enough. Use hasDeltaPos() hasDeltaRot() when necessary.

    //Position events will not pass through checks if the player is teleporting.

    private final boolean onGround;
    private final boolean onGroundReally;
    private boolean teleported;
    private Location cancelLocation;
    private boolean updatePos;
    private boolean updateRot;
    private boolean acceptedKnockback;
    //No, don't compute a delta vector during instantiation since teleports will affect it.

    //Not sure if these maps are necessary since you can determine the previous position using HawkPlayer#getLocation()
    private static final Map<UUID, Location> last = new HashMap<>();
    private static final Map<UUID, Location> current = new HashMap<>();

    public MoveEvent(Player p, Location update, boolean onGround, HawkPlayer pp, WrappedPacket packet, boolean updatePos, boolean updateRot) {
        super(p, pp, packet);
        last.put(p.getUniqueId(), current.getOrDefault(p.getUniqueId(), pp.getLocation()));
        current.put(p.getUniqueId(), update);
        onGroundReally = AdjacentBlocks.onGroundReally(update, update.getY() - getFrom().getY(), true, 0.02);
        this.updatePos = updatePos;
        this.updateRot = updateRot;
        this.onGround = onGround;
        this.acceptedKnockback = handlePendingVelocities();
    }

    private boolean handlePendingVelocities() {
        List<Pair<Vector, Long>> kbs = pp.getPendingVelocities();
        if (kbs.size() > 0) {
            double epsilon = 0.002;
            int kbIndex;
            int expiredKbs = 0;
            long currTime = System.currentTimeMillis();
            Vector currVelocity = new Vector(getTo().getX() - getFrom().getX(), getTo().getY() - getFrom().getY(), getTo().getZ() - getFrom().getZ());
            Set<PhysicsUtils.Direction> touchingBlocks = PhysicsUtils.checkTouchingBlock(new AABB(getTo().toVector().add(new Vector(-0.3, 0.000001, -0.3)), getTo().toVector().add(new Vector(0.3, 1.799999, 0.3))), getTo().getWorld());
            double speedPotMultiplier = 1;
            for (PotionEffect effect : p.getActivePotionEffects()) {
                if (!effect.getType().equals(PotionEffectType.SPEED))
                    continue;
                speedPotMultiplier = 1 + (effect.getAmplifier() + 1 * 0.2);
            }
            boolean flying          = p.isFlying();
            double sprintMultiplier = flying ? (p.isSprinting() ? 2 : 1) : (p.isSprinting() ? 1.3 : 1);
            double weirdConstant    = (p.isOnGround() ? 0.098 : (flying ? 0.049 : 0.0196));
            double baseMultiplier   = flying ? (10 * p.getFlySpeed()) : (5 * p.getWalkSpeed() * speedPotMultiplier);
            double total            = weirdConstant * baseMultiplier * sprintMultiplier;

            //pending knockbacks must be in order; get the first entry in the list.
            //if the first entry doesn't work (probably because they were fired on the same tick),
            //then work down the list until we find something
            for (kbIndex = 0; kbIndex < kbs.size(); kbIndex++) {
                Pair<Vector, Long> kb = kbs.get(kbIndex);
                if (currTime - kb.getValue() <= ServerUtils.getPing(p) + 200) {

                    Vector kbVelocity = kb.getKey();

                    //check Y component
                    //TODO: air, web, and liquid friction.
                    if (Math.abs((onGround ? 0 : kbVelocity.getY()) - currVelocity.getY()) > 0.01) {
                        continue;
                    }

                    double minThresX = kbVelocity.getX() - total - epsilon;
                    double maxThresX = kbVelocity.getX() + total + epsilon;
                    double minThresZ = kbVelocity.getZ() - total - epsilon;
                    double maxThresZ = kbVelocity.getZ() + total + epsilon;

                    //check X component
                    //only check if player is not pinned to a wall
                    if (!((touchingBlocks.contains(PhysicsUtils.Direction.EAST) && kbVelocity.getX() > 0) || (touchingBlocks.contains(PhysicsUtils.Direction.WEST) && kbVelocity.getX() < 0)) &&
                            !(currVelocity.getX() <= maxThresX && currVelocity.getX() >= minThresX)) {
                        continue;
                    }
                    //check Z component
                    //only check if player is not pinned to a wall
                    if (!((touchingBlocks.contains(PhysicsUtils.Direction.SOUTH) && kbVelocity.getZ() > 0) || (touchingBlocks.contains(PhysicsUtils.Direction.NORTH) && kbVelocity.getZ() < 0)) &&
                            !(currVelocity.getZ() <= maxThresZ && currVelocity.getZ() >= minThresZ)) {
                        continue;
                    }
                    kbs.subList(0, kbIndex + 1).clear();
                    return true;
                }
                else {
                    expiredKbs++;
                }
            }
            kbs.subList(0, expiredKbs).clear();
        }
        return false;
    }

    public Player getPlayer() {
        return p;
    }

    public Location getTo() {
        //how this can possibly ever return null, idek. here's a getOrDefault for now.
        return current.getOrDefault(p.getUniqueId(), pp.getLocation());
    }

    public Location getFrom() {
        return last.getOrDefault(p.getUniqueId(), pp.getLocation());
    }

    public void setTo(Location to) {
        current.put(p.getUniqueId(), to);
    }

    public void setFrom(Location from) {
        last.put(p.getUniqueId(), from);
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
        for(ClientBlock cBlock : pp.getClientBlocks()) {
            cube.translateTo(cBlock.getLocation().toVector());
            if(cBlock.getMaterial().isSolid() && feet.isColliding(cube) && !aboveFeet.isColliding(cube))
                return cBlock;
        }
        return null;
    }

    public boolean hasTeleported() {
        return teleported;
    }

    public void setTeleported(boolean teleported) {
        this.teleported = teleported;
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
    //doesn't mean the pos/rot actually changed.
    public boolean isUpdatePos() {
        return updatePos;
    }

    public boolean isUpdateRot() {
        return updateRot;
    }

    public boolean hasAcceptedKnockback() {
        return acceptedKnockback;
    }

    public void cancelAndSetBack(Location setback) {
        if (cancelLocation == null) {
            cancelLocation = setback;
            setCancelled(true);
            pp.setTeleporting(true);
            pp.setTeleportLoc(setback);
        }
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        if(!cancelled) {
            cancelLocation = null;
        }
    }

    public static void discardData() {
        last.clear();
        current.clear();
    }

    public static Location getLastPosition(HawkPlayer pp) {
        return current.getOrDefault(pp.getUuid(), pp.getLocation());
    }

}
