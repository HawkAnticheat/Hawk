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
import me.islandscout.hawk.util.block.BlockNMS;
import me.islandscout.hawk.util.packet.WrappedPacket;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
    private boolean failedKnockback;
    private boolean hitSlowdown; //Idk, it's weird. Sprinting or hitting with kb enchant will multiply horizontal speed by 0.6.
    private Set<Direction> boxSidesTouchingBlocks;
    private boolean inLiquid;
    private boolean jumped;
    private boolean slimeBlockBounce;
    private Vector waterFlowForce;
    private List<Pair<Block, Vector>> liquidsAndDirections;
    private Set<Material> liquidTypes;
    //No, don't compute a delta vector during instantiation since teleports will affect it.

    //Not sure if these maps are necessary since you can determine the previous position using HawkPlayer#getLocation()
    private static final Map<UUID, Location> last = new HashMap<>();
    private static final Map<UUID, Location> current = new HashMap<>();

    private static final List<Material> liquidDefs = Arrays.asList(Material.WATER, Material.STATIONARY_WATER, Material.LAVA, Material.STATIONARY_LAVA);

    public MoveEvent(Player p, Location update, boolean onGround, HawkPlayer pp, WrappedPacket packet, boolean updatePos, boolean updateRot) {
        super(p, pp, packet);
        last.put(p.getUniqueId(), current.getOrDefault(p.getUniqueId(), pp.getLocation()));
        current.put(p.getUniqueId(), update);
        onGroundReally = AdjacentBlocks.onGroundReally(update, update.getY() - getFrom().getY(), true, 0.02);
        this.updatePos = updatePos;
        this.updateRot = updateRot;
        this.onGround = onGround;
        this.liquidTypes = new HashSet<>();
        ItemStack heldItem = pp.getItemUsedForAttack();
        hitSlowdown = pp.getLastAttackedPlayerTick() == pp.getCurrentTick() && (pp.isSprinting() || (heldItem != null && heldItem.getEnchantmentLevel(Enchantment.KNOCKBACK) > 0));
        boxSidesTouchingBlocks = AdjacentBlocks.checkTouchingBlock(new AABB(getTo().toVector().add(new Vector(-0.299999, 0.000001, -0.299999)), getTo().toVector().add(new Vector(0.299999, 1.799999, 0.299999))), getTo().getWorld(), 0.0001);
        acceptedKnockback = handlePendingVelocities();
        liquidsAndDirections = testLiquids();
        inLiquid = liquidsAndDirections.size() > 0;
        jumped = testJumped();
        slimeBlockBounce = testSlimeBlockBounce();
        waterFlowForce = computeWaterFlowForce();
    }

    //Good thing I have MCP to figure this one out
    private List<Pair<Block, Vector>> testLiquids() {
        AABB liquidTest = AABB.playerWaterCollisionBox.clone();
        liquidTest.translate(getTo().toVector());
        List<Pair<Block, Vector>> liquids = new ArrayList<>();
        List<Block> blocks = liquidTest.getBlocks(p.getWorld());
        for(Block b : blocks) {
            if(liquidDefs.contains(b.getType())) {
                Vector direction = BlockNMS.getBlockNMS(b).getFlowDirection();
                liquids.add(new Pair<>(b, direction));
                this.liquidTypes.add(b.getType());
            }
        }
        return liquids;
    }

    //May return true if player is knocked up against a very low ceiling, but who cares?
    private boolean testJumped() {
        float deltaY = (float)(getTo().getY() - getFrom().getY());
        return (pp.isOnGroundReally() && !isOnGround()) && (deltaY == 0.42F || boxSidesTouchingBlocks.contains(Direction.TOP));
    }

    //Again, kudos to MCP for guiding me to the right direction
    private boolean testSlimeBlockBounce() {
        if(Hawk.getServerVersion() < 8)
            return false;
        float deltaY = (float)(getTo().getY() - getFrom().getY());
        Block staningOn = ServerUtils.getBlockAsync(getFrom().clone().add(0, -0.01, 0));
        if(staningOn == null || staningOn.getType() != Material.SLIME_BLOCK)
            return false;
        float prevPrevDeltaY = (float)pp.getPreviousVelocity().getY();
        float expected = -0.96F * prevPrevDeltaY;
        return !pp.isSneaking() &&
                pp.getVelocity().getY() < 0 &&
                deltaY > 0 &&
                deltaY > (prevPrevDeltaY < -0.1F ? expected - 0.003 : 0) &&
                deltaY <= expected;
    }

    private Vector computeWaterFlowForce() {
        Vector finalForce = new Vector();
        for(Pair<Block, Vector> liquid : liquidsAndDirections) {
            Material mat = liquid.getKey().getType();
            if(mat == Material.STATIONARY_WATER || mat == Material.WATER) {
                finalForce.add(liquid.getValue());
            }
        }
        if(finalForce.lengthSquared() > 0 && !pp.isFlyingClientside()) {
            finalForce.normalize();
            finalForce.multiply(Physics.WATER_FLOW_FORCE_MULTIPLIER);
            return finalForce;
        }
        return finalForce;
    }

    //This literally makes me want to punch a wall.
    private boolean handlePendingVelocities() {
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
            boolean flying          = p.isFlying();
            double sprintMultiplier = flying ? (pp.isSprinting() ? 2 : 1) : (pp.isSprinting() ? 1.3 : 1);
            double weirdConstant    = (jump && pp.isSprinting() ? 0.2518462 : (pp.isSwimming() ? 0.0196 : 0.098)); //(pp.isOnGround() ? 0.098 : (flying ? 0.049 : 0.0196));
            double baseMultiplier   = flying ? (10 * p.getFlySpeed()) : (5 * p.getWalkSpeed() * speedPotMultiplier);
            double maxDiscrepancy   = weirdConstant * baseMultiplier * sprintMultiplier + epsilon;

            //pending knockbacks must be in order; get the first entry in the list.
            //if the first entry doesn't work (probably because they were fired on the same tick),
            //then work down the list until we find something
            for (kbIndex = 0; kbIndex < kbs.size(); kbIndex++) {
                Pair<Vector, Long> kb = kbs.get(kbIndex);
                if (currTime - kb.getValue() <= ServerUtils.getPing(p) + 200) {

                    Vector kbVelocity = kb.getKey();
                    double x = hitSlowdown ? 0.6 * kbVelocity.getX() : kbVelocity.getX();
                    double y = kbVelocity.getY();
                    double z = hitSlowdown ? 0.6 * kbVelocity.getZ() : kbVelocity.getZ();

                    //check Y component
                    //skip to next kb if...
                    if (!((boxSidesTouchingBlocks.contains(Direction.TOP) && y > 0) || (boxSidesTouchingBlocks.contains(Direction.BOTTOM) && y < 0)) && /*...player isn't colliding...*/
                            Math.abs(y - currVelocity.getY()) > 0.01 && /*...and velocity is nowhere close to kb velocity...*/
                            !jump && !pp.isSwimming()/*...and did not jump and is not swimming*/) {
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
                    return true;
                }
                else {
                    failedKnockback = true;
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
    //doesn't mean that the pos/rot actually changed.
    public boolean isUpdatePos() {
        return updatePos;
    }

    public boolean isUpdateRot() {
        return updateRot;
    }

    public boolean hasAcceptedKnockback() {
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

    public boolean isInLiquid() {
        return inLiquid;
    }

    public Set<Material> getLiquidTypes() {
        return liquidTypes;
    }

    public boolean hasJumped() {
        return jumped;
    }

    public boolean isSlimeBlockBounce() {
        return slimeBlockBounce;
    }

    public Vector getWaterFlowForce() {
        return waterFlowForce;
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
