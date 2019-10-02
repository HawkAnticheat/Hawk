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

package me.islandscout.hawk;

import me.islandscout.hawk.check.Check;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a client. Provides essential and additional tools
 * to analyze players during packet interception. Recommended
 * to use this rather than Bukkit's implementation.
 *
 * Fields here should be faithful to the client they represent
 * as much as possible, regardless of any corrections that the
 * anti-cheat makes to the incoming packets. Caution: that means
 * some of these fields can be spoofed!
 */
public class HawkPlayer {

    private final UUID uuid;
    private final Map<Check, Double> vl;
    private boolean digging;
    private boolean receiveNotifications;
    private boolean online;
    private final Player p;
    private boolean teleporting;
    private Location teleportLoc;
    private long lastTeleportSendTick;
    private long lastTeleportAcceptTick;
    private final Hawk hawk;

    //Be careful when playing with these fields.
    //World is not sync'd to position, yaw, nor pitch.
    private World world; //updated by server
    private Vector position; //updated by client
    private float yaw; //updated by client
    private float pitch; //updated by client

    private Vector velocity;
    private Vector previousVelocity;
    private float deltaYaw;
    private float deltaPitch;
    private boolean onGround;
    private boolean prevTickOnGround;
    private boolean onGroundReally;
    private float friction;
    private int ping;
    private short pingJitter;
    private long lastMoveTime;
    private long currentTick;
    private boolean sneaking;
    private boolean sprinting;
    private boolean blocking;
    private boolean pullingBow;
    private boolean consumingItem;
    private boolean inLiquid;
    private boolean swimming;
    private long itemUseTick;
    private long lastAttackedPlayerTick;
    private long lastInLiquidToggleTick;
    private long lastMoveTick;
    private long hitSlowdownTick;
    private long lastVelocityAcceptTick;
    private long lastLandTick;
    private ItemStack itemUsedForAttack;
    private double maxY;
    private double jumpedHeight;
    private long flyPendingTime;
    private int heldItemSlot;
    private final Map<Location, ClientBlock> clientBlocks;
    private final List<Pair<Vector, Long>> pendingVelocities;
    private final List<Pair<Boolean, Long>> pendingSprintChange;
    private final Set<Direction> boxSidesTouchingBlocks;
    private Vector waterFlowForce;

    HawkPlayer(Player p, Hawk hawk) {
        this.uuid = p.getUniqueId();
        vl = new ConcurrentHashMap<>();
        receiveNotifications = true;
        this.p = p;
        Location defaultLocation = p.getLocation();
        this.world = p.getWorld();
        this.position = defaultLocation.toVector();
        this.yaw = defaultLocation.getYaw();
        this.pitch = defaultLocation.getPitch();
        this.velocity = new Vector();
        this.previousVelocity = new Vector();
        this.onGround = ((Entity) p).isOnGround();
        this.hawk = hawk;
        this.ping = ServerUtils.getPing(p);
        this.heldItemSlot = p.getInventory().getHeldItemSlot();
        clientBlocks = new ConcurrentHashMap<>();
        pendingVelocities = new ArrayList<>();
        pendingSprintChange = new ArrayList<>();
        boxSidesTouchingBlocks = new HashSet<>();
        this.waterFlowForce = new Vector();
    }

    public int getVL(Check check) {
        return (int) (double) vl.getOrDefault(check, 0D);
    }

    public Map<Check, Double> getVLs() {
        return vl;
    }

    public void setVL(Check check, double vl) {
        this.vl.put(check, vl);
    }

    public void incrementVL(Check check) {
        this.vl.put(check, vl.getOrDefault(check, 0D) + 1D);
    }

    public void addVL(Check check, double amnt) {
        this.vl.put(check, vl.getOrDefault(check, 0D) + amnt);
    }

    public void multiplyVL(Check check, double factor) {
        vl.put(check, vl.getOrDefault(check, 0D) * factor);
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isDigging() {
        return digging;
    }

    //this should really not be public
    public void setDigging(boolean digging) {
        this.digging = digging;
    }

    public boolean canReceiveAlerts() {
        return receiveNotifications && p.hasPermission(Hawk.BASE_PERMISSION + ".alerts");
    }

    public void setReceiveNotifications(boolean status) {
        receiveNotifications = status;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isTeleporting() {
        return teleporting;
    }

    //this should really not be public
    public void setTeleporting(boolean status) {
        teleporting = status;
    }

    public Location getTeleportLoc() {
        return teleportLoc;
    }

    public void setTeleportLoc(Location teleportLoc) {
        this.teleportLoc = teleportLoc;
    }

    public long getLastTeleportSendTick() {
        return lastTeleportSendTick;
    }

    public void setLastTeleportSendTick(long lastTeleportTime) {
        this.lastTeleportSendTick = lastTeleportTime;
    }

    public long getLastTeleportAcceptTick() {
        return lastTeleportAcceptTick;
    }

    public void setLastTeleportAcceptTick(long lastTeleportAcceptTick) {
        this.lastTeleportAcceptTick = lastTeleportAcceptTick;
    }

    public boolean isOnGround() {
        return onGround;
    }

    //this should really not be public
    public void setOnGround(boolean onGround) {
        this.prevTickOnGround = this.onGround;
        this.onGround = onGround;
    }

    //why do I need to make this???
    public boolean wasOnGround() {
        return prevTickOnGround;
    }

    public boolean isOnGroundReally() {
        return onGroundReally;
    }

    public void setOnGroundReally(boolean onGroundReally) {
        this.onGroundReally = onGroundReally;
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public short getPingJitter() {
        return pingJitter;
    }

    public void setPingJitter(short pingJitter) {
        this.pingJitter = pingJitter;
    }

    public long getLastMoveTime() {
        return lastMoveTime;
    }

    public void setLastMoveTime(long lastMoveTime) {
        this.lastMoveTime = lastMoveTime;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Vector getPosition() {
        return position;
    }

    public Vector getPositionCloned() {
        return position.clone();
    }

    //Returns the predicted position of the HawkPlayer (in the case that the
    //latest flying packet was not an update-position). It simulates player
    //movement without user input.
    public Vector getPositionPredicted() {
        return null; //TODO
    }

    public void setPosition(Vector position) {
        this.position = position;
    }

    public void setPositionYawPitch(Vector position, float yaw, float pitch) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector velocity) {
        this.previousVelocity = this.velocity;
        this.velocity = velocity;
    }

    public Vector getPreviousVelocity() {
        return previousVelocity;
    }

    public float getDeltaYaw() {
        return deltaYaw;
    }

    public void setDeltaYaw(float deltaYaw) {
        this.deltaYaw = deltaYaw;
    }

    public float getDeltaPitch() {
        return deltaPitch;
    }

    public void setDeltaPitch(float deltaPitch) {
        this.deltaPitch = deltaPitch;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void incrementCurrentTick() {
        this.currentTick++;
        manageClientBlocks();
        //handlePendingSprints();
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public boolean isPullingBow() {
        return pullingBow;
    }

    public void setPullingBow(boolean pullingBow) {
        this.pullingBow = pullingBow;
    }

    public double getFallDistance() {
        return maxY - position.getY();
    }

    public int getHeldItemSlot() {
        return heldItemSlot;
    }

    public void setHeldItemSlot(int heldItemSlot) {
        this.heldItemSlot = heldItemSlot;
    }

    public boolean isConsumingItem() {
        return consumingItem;
    }

    public void setConsumingItem(boolean consumingItem) {
        this.consumingItem = consumingItem;
        if(consumingItem)
            itemUseTick = currentTick;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public void setInLiquid(boolean inLiquid) {
        if(this.inLiquid != inLiquid) {
            lastInLiquidToggleTick = currentTick;
            this.inLiquid = inLiquid;
        }
    }

    public boolean isFlyingClientside() {
        return (p.getAllowFlight() && hasFlyPending()) || p.isFlying();
    }

    public long getItemUseTick() {
        return itemUseTick;
    }

    public long getLastAttackedPlayerTick() {
        return lastAttackedPlayerTick;
    }

    public void updateLastAttackedPlayerTick() {
        this.lastAttackedPlayerTick = currentTick;
    }

    public long getLastInLiquidToggleTick() {
        return lastInLiquidToggleTick;
    }

    public long getHitSlowdownTick() {
        return hitSlowdownTick;
    }

    public void updateHitSlowdownTick() {
        this.hitSlowdownTick = currentTick;
    }

    public long getLastVelocityAcceptTick() {
        return lastVelocityAcceptTick;
    }

    public void updateLastVelocityAcceptTick() {
        this.lastVelocityAcceptTick = currentTick;
    }

    public boolean hasHitSlowdown() {
        return hitSlowdownTick == currentTick;
    }

    public boolean isSwimming() {
        return swimming;
    }

    public void setSwimming(boolean swimming) {
        this.swimming = swimming;
    }

    public ItemStack getItemUsedForAttack() {
        return itemUsedForAttack;
    }

    public void updateItemUsedForAttack() {
        this.itemUsedForAttack = getHeldItem();
    }

    public ItemStack getHeldItem() {
        return p.getInventory().getItem(heldItemSlot);
    }

    //call this before updating whether on ground or not
    public void updateFallDistance(Location loc) {
        if (onGround)
            maxY = loc.getY();
        else
            maxY = Math.max(loc.getY(), maxY);

    }

    public double getTotalAscensionSinceGround() {
        return jumpedHeight;
    }

    //call this before updating whether on ground or not
    public void updateTotalAscensionSinceGround(double y1, double y2) {
        if (onGround) {
            jumpedHeight = 0;
            return;
        }
        double deltaY = y2 - y1;
        if(deltaY > 0)
            jumpedHeight += deltaY;
    }

    public boolean hasFlyPending() {
        return System.currentTimeMillis() - flyPendingTime <= 100;
    }

    public void setFlyPendingTime(long time) {
        flyPendingTime = time;
    }

    public Player getPlayer() {
        return p;
    }

    public Map<Location, ClientBlock> getClientBlocks() {
        return clientBlocks;
    }

    public void addClientBlock(Location loc, ClientBlock pBlock) {
        if (clientBlocks.size() >= ClientBlock.MAX_PER_PLAYER)
            return;
        clientBlocks.put(loc, pBlock);
    }

    private void manageClientBlocks() {
        for (Location loc : clientBlocks.keySet()) {
            ClientBlock clientBlock = clientBlocks.get(loc);
            if (currentTick - clientBlock.getInitTick() > ClientBlock.CLIENTTICKS_UNTIL_EXPIRE) {
                clientBlocks.remove(loc);
            }
        }
    }

    public Set<Direction> getBoxSidesTouchingBlocks() {
        return boxSidesTouchingBlocks;
    }

    public Vector getWaterFlowForce() {
        return waterFlowForce;
    }

    public void setWaterFlowForce(Vector waterFlowForce) {
        this.waterFlowForce = waterFlowForce;
    }

    public long getLastMoveTick() {
        return lastMoveTick;
    }

    public long getClientTicksSinceLastMove() {
        return getCurrentTick() - lastMoveTick;
    }

    public void setHasMoved() {
        this.lastMoveTick = getCurrentTick();
    }

    public long getLastLandTick() {
        return lastLandTick;
    }

    public void updateLastLandTick() {
        this.lastLandTick = currentTick;
    }

    public List<Pair<Vector, Long>> getPendingVelocities() {
        return pendingVelocities;
    }

    public List<Pair<Boolean, Long>> getPendingSprintChange() {
        return pendingSprintChange;
    }

    public float getFriction() {
        return friction;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    //safely kill the connection
    public void kickPlayer(String reason) {
        online = false;
        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, () -> p.kickPlayer(reason), 0L);
    }

    //safely teleport player
    public void teleport(Location location, PlayerTeleportEvent.TeleportCause teleportCause) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, () -> p.teleport(location, teleportCause), 0L);
    }

    /*private void handlePendingSprints() {
        if(pendingSprintChange.size() > 0) {
            long currTime = System.currentTimeMillis();

            //iterate from the most recent entry to the oldest
            for (int i = pendingSprintChange.size() - 1; i >= 0; i--) {
                Pair<Boolean, Long> sprint = pendingSprintChange.get(i);

                if (currTime - sprint.getValue() >= ServerUtils.getPing(p)) {
                    setSprinting(sprint.getKey());
                    //if the player isn't moving forwards or is slowing down or is colliding horizontally,
                    //then set sprint to false by the next tick.
                    //oh, and this should be a special sprint only used by the hitSlowDown detection code. should
                    //function like the default sprint except that this overrides the current state
                    pendingSprintChange.subList(0, i + 1).clear();
                    break;
                }
            }
        }
    }*/

    public AABB getCollisionBox() {
        return WrappedEntity.getWrappedEntity(p).getCollisionBox(position);
    }

    public AABB getHitBox() {
        return WrappedEntity.getWrappedEntity(p).getHitbox(position);
    }

    public long getCurrentValidatedTick() {
        //TODO
        return 0;
    }

}
