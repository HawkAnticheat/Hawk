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
import me.islandscout.hawk.wrap.block.WrappedBlock;
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

    //Represents a queue of commands sent from server to client. Simulates latency as it is ticked.
    //Can be used to simulate commands that the client doesn't show any clear acknowledgement for,
    //such as inventory window opening/closing.
    private final List<Pair<Runnable, Long>> simulatedCmds;

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

    private Vector predictedPosition; //ticked by client
    private Vector predictedVelocity;

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
    private byte inventoryOpen; //0 for closed; 1 for own inventory; 2 for any other inventory
    private boolean inLiquid;
    private boolean swimming;
    private long itemUseTick;
    private long lastAttackedPlayerTick;
    private long lastEntityInteractTick;
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
    private final Set<Direction> boxSidesTouchingBlocks;
    private Vector waterFlowForce;
    private Map<Location, List<AABB>> trackedBlockCollisions;
    private Set<Location> ignoredBlockCollisions;

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
        this.predictedPosition = defaultLocation.toVector();
        this.predictedVelocity = new Vector();
        this.onGround = ((Entity) p).isOnGround();
        this.hawk = hawk;
        this.ping = ServerUtils.getPing(p);
        this.heldItemSlot = p.getInventory().getHeldItemSlot();
        clientBlocks = new ConcurrentHashMap<>();
        pendingVelocities = new ArrayList<>();
        boxSidesTouchingBlocks = new HashSet<>();
        this.waterFlowForce = new Vector();
        trackedBlockCollisions = new HashMap<>();
        ignoredBlockCollisions = new HashSet<>();
        simulatedCmds = new CopyOnWriteArrayList<>();
    }

    public void tick() {
        this.currentTick++;
        manageClientBlocks();
        executeTasks();
        //TODO this doesn't need to be called so often
        if(predictedVelocity.lengthSquared() > 0)
            predictNextPosition();
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

    /*TODO no no no no no... this is NOT how we handle teleports. THE MAIN THREAD SHOULD BE THE ONLY THREAD TOUCHING THIS.
    TODO No wonder why it's so fkin difficult to do /spawn while getting rubberbanded back and forth. REDO THIS SYSTEM.
    */
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

    public Vector getHeadPosition() {
        Vector add = new Vector(0, 0, 0);
        add.setY(isSneaking() ? 1.54 : 1.62);
        return position.clone().add(add);
    }

    public Vector getPositionCloned() {
        return position.clone();
    }

    //Returns the predicted position of the HawkPlayer, assuming no user input.
    public Vector getPositionPredicted() {
        return predictedPosition;
    }

    public void setPosition(Vector position) {
        this.position = position;
    }

    public void updatePositionYawPitch(Vector position, float yaw, float pitch, boolean isPosUpdate) {
        this.previousVelocity = this.velocity;
        this.velocity = new Vector(
                position.getX() - this.position.getX(),
                position.getY() - this.position.getY(),
                position.getZ() - this.position.getZ());
        this.deltaYaw = yaw - this.yaw;
        this.deltaPitch = pitch - this.pitch;

        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        if(isPosUpdate) {
            predictedPosition = position.clone();
            if(previousVelocity.lengthSquared() > 0)
                predictedVelocity = velocity.clone();
        }
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

    public Vector getVelocityPredicted() {
        return predictedVelocity;
    }

    public Vector getPreviousVelocity() {
        return previousVelocity;
    }

    public float getDeltaYaw() {
        return deltaYaw;
    }

    public float getDeltaPitch() {
        return deltaPitch;
    }

    public long getCurrentTick() {
        return currentTick;
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

    public byte hasInventoryOpen() {
        return inventoryOpen;
    }

    public void setInventoryOpen(byte status) {
        this.inventoryOpen = status;
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

    public long getLastEntityInteractTick() {
        return lastEntityInteractTick;
    }

    public void updateLastEntityInteractTick() {
        this.lastEntityInteractTick = currentTick;
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

    //TODO shrink this code
    private void predictNextPosition() {
        Vector move = predictedVelocity.clone();

        //compute new predicted motion values
        double pdX = move.getX() * getFriction();
        double pdY = move.getY() * 0.98;
        double pdZ = move.getZ() * getFriction();

        if(Math.abs(pdX) < 0.005) {
            pdX = 0;
        }
        if(Math.abs(pdY) < 0.005) {
            pdY = 0;
        }
        if(Math.abs(pdZ) < 0.005) {
            pdZ = 0;
        }

        pdY += -0.0784;

        //TODO don't forget sneaking & how it prevents you from falling off blocks

        AABB box = WrappedEntity.getWrappedEntity(p).getCollisionBox(predictedPosition);
        AABB preBox = box.clone();
        preBox.expand(-0.0001, -0.0001, -0.0001);
        List<AABB> collidedBlocksBefore = preBox.getBlockAABBs(world);

        //clipping order: X, Z, Y
        //X
        box.expand(-0.00000001, -0.00000001, -0.00000001);
        boolean positive = pdX > 0;
        box.translate(new Vector(pdX, 0, 0));
        List<AABB> collidedBlocks = box.getBlockAABBs(world);
        collidedBlocks.removeAll(collidedBlocksBefore);

        double highestPoint = positive ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        for(AABB aabb : collidedBlocks) {
            double point = positive ? aabb.getMin().getX() : aabb.getMax().getX();
            if(positive && point < highestPoint)
                highestPoint = point;
            else if(!positive && point > highestPoint)
                highestPoint = point;
        }
        if(Double.isFinite(highestPoint)) {
            double invPenetrationDist = positive ? highestPoint - box.getMax().getX() - 0.00000001 : highestPoint - box.getMin().getX() + 0.00000001;
            box.translate(new Vector(invPenetrationDist, 0, 0));
            pdX += invPenetrationDist;
        }

        //Z
        positive = pdZ > 0;
        box.translate(new Vector(0, 0, pdZ));
        collidedBlocks = box.getBlockAABBs(world);
        collidedBlocks.removeAll(collidedBlocksBefore);

        highestPoint = positive ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        for(AABB aabb : collidedBlocks) {
            double point = positive ? aabb.getMin().getZ() : aabb.getMax().getZ();
            if(positive && point < highestPoint)
                highestPoint = point;
            else if(!positive && point > highestPoint)
                highestPoint = point;
        }
        if(Double.isFinite(highestPoint)) {
            double invPenetrationDist = positive ? highestPoint - box.getMax().getZ() - 0.00000001 : highestPoint - box.getMin().getZ() + 0.00000001;
            box.translate(new Vector(0, 0, invPenetrationDist));
            pdZ += invPenetrationDist;
        }

        //Y
        positive = pdY > 0;
        box.translate(new Vector(0, pdY, 0));
        collidedBlocks = box.getBlockAABBs(world);
        collidedBlocks.removeAll(collidedBlocksBefore);

        highestPoint = positive ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        for(AABB aabb : collidedBlocks) {
            double point = positive ? aabb.getMin().getY() : aabb.getMax().getY();
            if(positive && point < highestPoint)
                highestPoint = point;
            else if(!positive && point > highestPoint)
                highestPoint = point;
        }
        if(Double.isFinite(highestPoint)) {
            double invPenetrationDist = positive ? highestPoint - box.getMax().getY() - 0.00000001 : highestPoint - box.getMin().getY() + 0.00000001;
            box.translate(new Vector(0, invPenetrationDist, 0));
            pdY += invPenetrationDist;
        }

        //move predicted position
        move.setX(pdX);
        move.setY(pdY);
        move.setZ(pdZ);
        predictedPosition.add(move);
        predictedVelocity = move;
    }

    public void updateIgnoredBlockCollisions(Vector position, boolean teleported) {
        AABB bbox = WrappedEntity.getWrappedEntity(p).getCollisionBox(position);
        bbox.expand(-0.0001, -0.0001, -0.0001);

        if(teleported) {
            Set<Location> ignored = new HashSet<>();
            for(Block b : bbox.getBlocks(world)) {
                Location loc = b.getLocation();
                for(AABB aabb : WrappedBlock.getWrappedBlock(b).getCollisionBoxes()) {
                    if(aabb.isColliding(bbox)) {
                        ignored.add(loc);
                        break;
                    }
                }
            }
            ignoredBlockCollisions = ignored;
        }
        else {
            Map<Location, List<AABB>> blocksInBBOld = trackedBlockCollisions;
            Map<Location, List<AABB>> blocksInBBNew = new HashMap<>();
            for(Block b : bbox.getBlocks(world)) {
                Location loc = b.getLocation();
                List<AABB> aabbs = Arrays.asList(WrappedBlock.getWrappedBlock(b).getCollisionBoxes());
                blocksInBBNew.put(loc, aabbs);
            }
            Set<Location> ignored = new HashSet<>();
            for(Location entry : blocksInBBNew.keySet()) {
                if (blocksInBBOld.containsKey(entry) && !blocksInBBOld.get(entry).equals(blocksInBBNew.get(entry))) {
                    for(AABB aabb : blocksInBBNew.get(entry)) {
                        if(aabb.isColliding(bbox)) {
                            ignored.add(entry);
                            break;
                        }
                    }
                }
            }
            trackedBlockCollisions = blocksInBBNew;
            Set<Location> ignoredOld = ignoredBlockCollisions;
            for(Location loc : ignoredOld) {
                Block b = ServerUtils.getBlockAsync(loc);
                if(b == null)
                    continue;
                for(AABB aabb : WrappedBlock.getWrappedBlock(b).getCollisionBoxes()) {
                    if(aabb.isColliding(bbox)) {
                        ignored.add(loc);
                        break;
                    }
                }
            }
            ignoredBlockCollisions = ignored;
        }
    }

    //Get locations of blocks that had their AABB updated within the player's AABB
    public Set<Location> getIgnoredBlockCollisions() {
        return ignoredBlockCollisions;
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

    //This method simulates the reaction of the client by a server packet.
    //This is accomplished by simulating latency.
    //This can be used to simulate sprint-update or inventory-open packets
    public void sendSimulatedAction(Runnable action) {
        simulatedCmds.add(new Pair<>(action, System.currentTimeMillis()));
    }

    private void executeTasks() {
        if(simulatedCmds.size() == 0)
            return;
        int ping = ServerUtils.getPing(p);
        long currTime = System.currentTimeMillis();
        while(simulatedCmds.size() > 0 && currTime - simulatedCmds.get(0).getValue() >= ping) {
            simulatedCmds.get(0).getKey().run();
            simulatedCmds.remove(0);
        }
    }

    public AABB getCollisionBox() {
        return WrappedEntity.getWrappedEntity(p).getCollisionBox(position);
    }

    public AABB getHitBox() {
        return WrappedEntity.getWrappedEntity(p).getHitbox(position);
    }

}
