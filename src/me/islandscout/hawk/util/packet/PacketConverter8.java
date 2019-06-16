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

package me.islandscout.hawk.util.packet;

import io.netty.buffer.Unpooled;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.event.*;
import me.islandscout.hawk.event.bukkit.HawkPlayerAsyncVelocityChangeEvent;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.util.block.BlockNMS;
import me.islandscout.hawk.util.block.BlockNMS8;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.IOException;

public final class PacketConverter8 {

    private PacketConverter8() {
    }

    public static Event packetInboundToEvent(Object packet, Player p, HawkPlayer pp) {
        if (packet instanceof PacketPlayInFlying) return packetToPosEvent((PacketPlayInFlying) packet, p, pp);
        if (packet instanceof PacketPlayInUseEntity) return packetToInterEvent((PacketPlayInUseEntity) packet, p, pp);
        if (packet instanceof PacketPlayInBlockDig) return packetToDigEvent((PacketPlayInBlockDig) packet, p, pp);
        if (packet instanceof PacketPlayInCustomPayload)
            return packetToPayloadEvent((PacketPlayInCustomPayload) packet, p, pp);
        if (packet instanceof PacketPlayInAbilities)
            return packetToAbilitiesEvent((PacketPlayInAbilities) packet, p, pp);
        if (packet instanceof PacketPlayInBlockPlace)
            return packetToUseEvent((PacketPlayInBlockPlace) packet, p, pp);
        if (packet instanceof PacketPlayInArmAnimation)
            return packetToArmSwingEvent((PacketPlayInArmAnimation) packet, p, pp);
        if (packet instanceof PacketPlayInHeldItemSlot)
            return packetToItemSwitchEvent((PacketPlayInHeldItemSlot) packet, p, pp);
        if (packet instanceof PacketPlayInEntityAction)
            return packetToPlayerActionEvent((PacketPlayInEntityAction) packet, p, pp);
        return null;
    }

    public static org.bukkit.event.Event packetOutboundToEvent(Object packet, Player p) {
        if(packet instanceof PacketPlayOutEntityVelocity || packet instanceof PacketPlayOutExplosion)
            return packetToVelocityEvent((Packet)packet, p);
        return null;
    }

    private static HawkPlayerAsyncVelocityChangeEvent packetToVelocityEvent(Packet packet, Player p) {
        if(packet instanceof PacketPlayOutExplosion) {
            PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
            try {
                packet.b(serializer);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            serializer.readerIndex(serializer.writerIndex() - 12);
            float x = serializer.readFloat();
            float y = serializer.readFloat();
            float z = serializer.readFloat();
            Vector velocity = new Vector(x, y, z);
            if(velocity.lengthSquared() == 0)
                return null;
            return new HawkPlayerAsyncVelocityChangeEvent(velocity, p, true);
        }
        else if(packet instanceof PacketPlayOutEntityVelocity) {
            PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
            try {
                packet.b(serializer);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            int id = serializer.e();
            if(id != p.getEntityId()) {
                return null;
            }
            double x = serializer.readShort() / 8000D;
            double y = serializer.readShort() / 8000D;
            double z = serializer.readShort() / 8000D;
            Vector velocity = new Vector(x, y, z);

            return new HawkPlayerAsyncVelocityChangeEvent(velocity, p, false);
        }
        return null;
    }

    private static Event packetToPosEvent(PacketPlayInFlying packet, Player p, HawkPlayer pp) {
        //default position
        Location loc = pp.getLocation();

        //There's an NPE here if someone teleports to another world using a dumb multi-world plugin (which sets the getTo location to null)
        //I don't believe it is my responsibility to "fix" this. If there are enough complaints, I MIGHT consider looking into it.
        loc = new Location(pp.getLocation().getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        WrappedPacket.PacketType pType = WrappedPacket.PacketType.FLYING;

        //update if has look
        boolean updateRot = false;
        if (packet.h()) {
            updateRot = true;
            pType = WrappedPacket.PacketType.LOOK;
            loc.setYaw(packet.d());
            loc.setPitch(packet.e());
        }

        //update if has position
        boolean updatePos = false;
        if (packet.g()) {
            updatePos = true;
            if (packet.h())
                pType = WrappedPacket.PacketType.POSITION_LOOK;
            else
                pType = WrappedPacket.PacketType.POSITION;
            loc.setX(packet.a());
            loc.setY(packet.b());
            loc.setZ(packet.c());
        }

        if(Math.abs(loc.getX()) >= Integer.MAX_VALUE || Math.abs(loc.getY()) >= Integer.MAX_VALUE || Math.abs(loc.getZ()) >= Integer.MAX_VALUE ||
                Double.isNaN(loc.getX()) || Double.isNaN(loc.getY()) || Double.isNaN(loc.getZ())) {
            return new BadEvent(p, pp, new WrappedPacket8(packet, pType));
        }

        return new MoveEvent(p, loc, packet.f(), pp, new WrappedPacket8(packet, pType), updatePos, updateRot);
    }

    private static InteractEntityEvent packetToInterEvent(PacketPlayInUseEntity packet, Player p, HawkPlayer pp) {
        if (packet.a() == null) return null;
        InteractAction action;
        if (packet.a() == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) action = InteractAction.ATTACK;
        else action = InteractAction.INTERACT;
        //get interacted entity. phew.
        Entity nmsEntity = packet.a(((CraftWorld) pp.getLocation().getWorld()).getHandle());
        if (nmsEntity == null) return null; //interacting with a non-existent entity
        org.bukkit.entity.Entity entity = nmsEntity.getBukkitEntity();

        return new InteractEntityEvent(p, pp, action, entity, new WrappedPacket8(packet, WrappedPacket.PacketType.USE_ENTITY));
    }

    private static Event packetToDigEvent(PacketPlayInBlockDig packet, Player p, HawkPlayer pp) {
        PacketPlayInBlockDig.EnumPlayerDigType digType = packet.c();
        BlockDigEvent.DigAction digAction = null;
        InteractItemEvent.Type interactAction = null;
        switch (digType) {
            case START_DESTROY_BLOCK:
                digAction = BlockDigEvent.DigAction.START;
                break;
            case ABORT_DESTROY_BLOCK:
                digAction = BlockDigEvent.DigAction.CANCEL;
                break;
            case STOP_DESTROY_BLOCK:
                digAction = BlockDigEvent.DigAction.COMPLETE;
                break;
            case RELEASE_USE_ITEM:
                interactAction = InteractItemEvent.Type.RELEASE_USE_ITEM;
                break;
            case DROP_ITEM:
                interactAction = InteractItemEvent.Type.DROP_HELD_ITEM;
                break;
            case DROP_ALL_ITEMS:
                interactAction = InteractItemEvent.Type.DROP_HELD_ITEM_STACK;
                break;
            default:
                return null;
        }
        if(interactAction == null) {
            BlockPosition pos = packet.a();
            Location loc = new Location(p.getWorld(), pos.getX(), pos.getY(), pos.getZ());

            org.bukkit.block.Block b = ServerUtils.getBlockAsync(loc);
            if(b == null)
                return null;
            BlockNMS block = new BlockNMS8(b);

            pp.setDigging(digAction == BlockDigEvent.DigAction.START && block.getStrength() != 0);
            return new BlockDigEvent(p, pp, digAction, b, new WrappedPacket8(packet, WrappedPacket.PacketType.BLOCK_DIG));
        }
        ItemStack item = p.getInventory().getItem(pp.getHeldItemSlot());
        if(item == null)
            return null;
        return new InteractItemEvent(p, pp, item, interactAction, new WrappedPacket8(packet, WrappedPacket.PacketType.BLOCK_DIG));

    }

    //TODO: work on this
    private static CustomPayLoadEvent packetToPayloadEvent(PacketPlayInCustomPayload packet, Player p, HawkPlayer pp) {
        return null;
    }

    private static AbilitiesEvent packetToAbilitiesEvent(PacketPlayInAbilities packet, Player p, HawkPlayer pp) {
        return new AbilitiesEvent(p, pp, packet.isFlying() && p.getAllowFlight(), new WrappedPacket8(packet, WrappedPacket.PacketType.ABILITIES));
    }

    //it appears that this gets called when interacting with blocks too
    private static Event packetToUseEvent(PacketPlayInBlockPlace packet, Player p, HawkPlayer pp) {
        org.bukkit.Material mat;
        if (packet.getItemStack() != null && packet.getItemStack().getItem() != null) {
            Block block = Block.asBlock(packet.getItemStack().getItem());
            //noinspection deprecation
            mat = org.bukkit.Material.getMaterial(Block.getId(block));
        } else {
            mat = null;
        }

        BlockPosition bPos = packet.a();
        int x = bPos.getX();
        int y = bPos.getY();
        int z = bPos.getZ();
        Vector targetedPosition = new Vector(x, y, z);
        InteractWorldEvent.BlockFace face;
        //Debug.broadcastMessage("FACE: " + packet.getFace());
        //Debug.broadcastMessage(x + " " + y + " " + z);
        //Debug.broadcastMessage(mat + "");

        InteractWorldEvent.InteractionType interactionType;
        //first vector is for 1.8 clients, second is for 1.7
        if(!targetedPosition.equals(new Vector(-1, -1, -1)) && !targetedPosition.equals(new Vector(-1, 255, -1))) {
            if(mat != null && mat != org.bukkit.Material.AIR) {
                interactionType = InteractWorldEvent.InteractionType.PLACE_BLOCK;
            }
            else {
                interactionType = InteractWorldEvent.InteractionType.INTERACT_BLOCK;
            }
        }
        else {
            ItemStack item = p.getInventory().getItem(pp.getHeldItemSlot());
            if(item == null)
                return null;
            return new InteractItemEvent(p, pp, item, InteractItemEvent.Type.START_USE_ITEM, new WrappedPacket8(packet, WrappedPacket.PacketType.BLOCK_PLACE));
        }

        switch (packet.getFace()) {
            case 0:
                face = InteractWorldEvent.BlockFace.BOTTOM;
                y -= 1;
                break;
            case 1:
                face = InteractWorldEvent.BlockFace.TOP;
                y += 1;
                break;
            case 2:
                face = InteractWorldEvent.BlockFace.NORTH;
                z -= 1;
                break;
            case 3:
                face = InteractWorldEvent.BlockFace.SOUTH;
                z += 1;
                break;
            case 4:
                face = InteractWorldEvent.BlockFace.WEST;
                x -= 1;
                break;
            case 5:
                face = InteractWorldEvent.BlockFace.EAST;
                x += 1;
                break;
            default:
                face = InteractWorldEvent.BlockFace.INVALID;
                break;
        }

        Location placedLocation = new Location(p.getWorld(), x, y, z);
        return new InteractWorldEvent(p, pp, placedLocation, mat, face, interactionType, new WrappedPacket8(packet, WrappedPacket.PacketType.BLOCK_PLACE));
    }

    private static ArmSwingEvent packetToArmSwingEvent(PacketPlayInArmAnimation packet, Player p, HawkPlayer pp) {
        return new ArmSwingEvent(p, pp, 0, new WrappedPacket8(packet, WrappedPacket.PacketType.ARM_ANIMATION));
    }

    private static ItemSwitchEvent packetToItemSwitchEvent(PacketPlayInHeldItemSlot packet, Player p, HawkPlayer pp) {
        return new ItemSwitchEvent(p, pp, packet.a(), new WrappedPacket8(packet, WrappedPacket.PacketType.HELD_ITEM_SLOT));
    }

    private static PlayerActionEvent packetToPlayerActionEvent(PacketPlayInEntityAction packet, Player p, HawkPlayer pp) {
        PacketPlayInEntityAction.EnumPlayerAction nmsAction = packet.b();
        PlayerActionEvent.PlayerAction action;
        switch (nmsAction) {
            case START_SNEAKING:
                action = PlayerActionEvent.PlayerAction.SNEAK_START;
                break;
            case STOP_SNEAKING:
                action = PlayerActionEvent.PlayerAction.SNEAK_STOP;
                break;
            case STOP_SLEEPING:
                action = PlayerActionEvent.PlayerAction.BED_LEAVE;
                break;
            case START_SPRINTING:
                action = PlayerActionEvent.PlayerAction.SPRINT_START;
                break;
            case STOP_SPRINTING:
                action = PlayerActionEvent.PlayerAction.SPRINT_STOP;
                break;
            case RIDING_JUMP:
                action = PlayerActionEvent.PlayerAction.HORSE_JUMP;
                break;
            case OPEN_INVENTORY:
                action = PlayerActionEvent.PlayerAction.INVENTORY_OPEN;
                break;
            default:
                action = PlayerActionEvent.PlayerAction.UNKNOWN;
        }
        return new PlayerActionEvent(p, pp, new WrappedPacket8(packet, WrappedPacket.PacketType.ENTITY_ACTION), action);
    }
}
