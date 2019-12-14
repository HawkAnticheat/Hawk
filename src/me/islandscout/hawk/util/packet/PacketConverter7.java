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

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.event.*;
import me.islandscout.hawk.event.bukkit.HawkAsyncPlayerMetadataEvent;
import me.islandscout.hawk.event.bukkit.HawkAsyncPlayerVelocityChangeEvent;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.wrap.WrappedWatchableObject;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import me.islandscout.hawk.wrap.block.WrappedBlock7;
import me.islandscout.hawk.wrap.packet.WrappedPacket;
import me.islandscout.hawk.wrap.packet.WrappedPacket7;
import net.minecraft.server.v1_7_R4.*;
import net.minecraft.util.io.netty.buffer.Unpooled;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class PacketConverter7 {

    private PacketConverter7() {
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
        if (packet instanceof PacketPlayInWindowClick)
            return packetToWindowClickEvent((PacketPlayInWindowClick) packet, p, pp);
        if(packet instanceof PacketPlayInClientCommand)
            return packetToOpenWindowEvent((PacketPlayInClientCommand) packet, p, pp);
        if(packet instanceof PacketPlayInCloseWindow)
            return packetToCloseWindowEvent((PacketPlayInCloseWindow) packet, p, pp);
        return null;
    }

    public static org.bukkit.event.Event packetOutboundToEvent(Object packet, Player p) {
        if(packet instanceof PacketPlayOutEntityVelocity || packet instanceof PacketPlayOutExplosion)
            return packetToVelocityEvent((Packet)packet, p);
        if(packet instanceof PacketPlayOutEntityMetadata)
            return packetToPlayerMetadataEvent((PacketPlayOutEntityMetadata)packet, p);
        return null;
    }

    private static HawkAsyncPlayerMetadataEvent packetToPlayerMetadataEvent(PacketPlayOutEntityMetadata packet, Player p) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
        packet.b(serializer);
        int id = serializer.readInt();
        List metaData = DataWatcher.b(serializer);
        if(id != p.getEntityId() || metaData == null)
            return null;

        List<WrappedWatchableObject> wrappedMetaData = new ArrayList<>();
        for(Object object : metaData) {
            if(object instanceof WatchableObject) {

                WatchableObject wO = (WatchableObject) object;
                WrappedWatchableObject wwO = new WrappedWatchableObject(wO.c(), wO.a(), wO.b());
                wwO.setWatched(wO.d());

                wrappedMetaData.add(wwO);
            }
        }

        return new HawkAsyncPlayerMetadataEvent(p, wrappedMetaData);
    }

    private static HawkAsyncPlayerVelocityChangeEvent packetToVelocityEvent(Packet packet, Player p) {
        if(packet instanceof PacketPlayOutExplosion) {
            PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
            ((PacketPlayOutExplosion) packet).b(serializer);
            serializer.readerIndex(serializer.writerIndex() - 12);
            float x = serializer.readFloat();
            float y = serializer.readFloat();
            float z = serializer.readFloat();
            Vector velocity = new Vector(x, y, z);
            if(velocity.lengthSquared() == 0)
                return null;
            return new HawkAsyncPlayerVelocityChangeEvent(velocity, p, true);
        }
        else if(packet instanceof PacketPlayOutEntityVelocity) {
            PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
            ((PacketPlayOutEntityVelocity) packet).b(serializer);
            int id = serializer.readInt();
            if(id != p.getEntityId()) {
                return null;
            }
            double x = serializer.readShort() / 8000D;
            double y = serializer.readShort() / 8000D;
            double z = serializer.readShort() / 8000D;
            Vector velocity = new Vector(x, y, z);

            return new HawkAsyncPlayerVelocityChangeEvent(velocity, p, false);
        }
        return null;
    }

    private static Event packetToPosEvent(PacketPlayInFlying packet, Player p, HawkPlayer pp) {
        //default position
        Vector position = pp.getPosition();
        Location loc = new Location(pp.getWorld(), position.getX(), position.getY(), position.getZ(), pp.getYaw(), pp.getPitch());

        //There's an NPE here if someone teleports to another world using a dumb multi-world plugin (which sets the PlayerTeleportEvent#getTo() location to null)
        //I don't believe it is my responsibility to "fix" this. If there are enough complaints, I MIGHT consider looking into it.
        loc = new Location(pp.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        WrappedPacket.PacketType pType = WrappedPacket.PacketType.FLYING;


        //update if has look
        boolean updateRot = false;
        if (packet.k()) {
            updateRot = true;
            pType = WrappedPacket.PacketType.LOOK;
            loc.setYaw(packet.g());
            loc.setPitch(packet.h());
        }

        //update if has position
        boolean updatePos = false;
        if (packet.j()) {
            updatePos = true;
            if (packet.k())
                pType = WrappedPacket.PacketType.POSITION_LOOK;
            else
                pType = WrappedPacket.PacketType.POSITION;
            loc.setX(packet.c());
            loc.setY(packet.d());
            loc.setZ(packet.e());
        }

        if(Math.abs(loc.getX()) >= Integer.MAX_VALUE || Math.abs(loc.getY()) >= Integer.MAX_VALUE || Math.abs(loc.getZ()) >= Integer.MAX_VALUE ||
            Double.isNaN(loc.getX()) || Double.isNaN(loc.getY()) || Double.isNaN(loc.getZ())) {
            return new BadEvent(p, pp, new WrappedPacket7(packet, pType));
        }

        return new MoveEvent(p, loc, packet.i(), pp, new WrappedPacket7(packet, pType), updatePos, updateRot);
    }

    private static Event packetToInterEvent(PacketPlayInUseEntity packet, Player p, HawkPlayer pp) {
        if (packet.c() == null) return null;
        InteractAction action;
        if (packet.c() == EnumEntityUseAction.ATTACK) action = InteractAction.ATTACK;
        else action = InteractAction.INTERACT;
        //get interacted entity. phew.
        Entity nmsEntity = packet.a(((CraftWorld) pp.getWorld()).getHandle());
        if (nmsEntity == null)
            return new BadEvent(p, pp, new WrappedPacket7(packet, WrappedPacket.PacketType.USE_ENTITY)); //interacting with a non-existent entity
        org.bukkit.entity.Entity entity = nmsEntity.getBukkitEntity();
        return new InteractEntityEvent(p, pp, action, entity, new WrappedPacket7(packet, WrappedPacket.PacketType.USE_ENTITY));
    }

    private static Event packetToDigEvent(PacketPlayInBlockDig packet, Player p, HawkPlayer pp) {
        int status = packet.g();
        BlockDigEvent.DigAction digAction = null;
        InteractItemEvent.Type interactAction = null;
        switch (status) {
            case 0:
                digAction = BlockDigEvent.DigAction.START;
                break;
            case 1:
                digAction = BlockDigEvent.DigAction.CANCEL;
                break;
            case 2:
                digAction = BlockDigEvent.DigAction.COMPLETE;
                break;
            case 3:
                interactAction = InteractItemEvent.Type.DROP_HELD_ITEM_STACK;
                break;
            case 4:
                interactAction = InteractItemEvent.Type.DROP_HELD_ITEM;
                break;
            case 5:
                interactAction = InteractItemEvent.Type.RELEASE_USE_ITEM;
                break;
            default:
                return new BadEvent(p, pp, new WrappedPacket7(packet, WrappedPacket.PacketType.BLOCK_DIG));
        }
        if(interactAction == null) {
            Location loc = new Location(p.getWorld(), packet.c(), packet.d(), packet.e());

            org.bukkit.block.Block b = ServerUtils.getBlockAsync(loc);
            if(b == null)
                return null;
            WrappedBlock block = new WrappedBlock7(b, pp.getClientVersion());

            pp.setDigging(digAction == BlockDigEvent.DigAction.START && block.getStrength() != 0);
            return new BlockDigEvent(p, pp, digAction, b, new WrappedPacket7(packet, WrappedPacket.PacketType.BLOCK_DIG));
        }
        ItemStack item = p.getInventory().getItem(pp.getHeldItemSlot());
        if(item == null)
            return null;
        return new InteractItemEvent(p, pp, item, interactAction, new WrappedPacket7(packet, WrappedPacket.PacketType.BLOCK_DIG));

    }

    private static CustomPayLoadEvent packetToPayloadEvent(PacketPlayInCustomPayload packet, Player p, HawkPlayer pp) {
        return new CustomPayLoadEvent(packet.c(), packet.length, packet.e(), p, pp, new WrappedPacket7(packet, WrappedPacket.PacketType.CUSTOM_PAYLOAD));
    }

    private static AbilitiesEvent packetToAbilitiesEvent(PacketPlayInAbilities packet, Player p, HawkPlayer pp) {
        return new AbilitiesEvent(p, pp, packet.isFlying() && p.getAllowFlight(), new WrappedPacket7(packet, WrappedPacket.PacketType.ABILITIES));
    }

    private static Event packetToUseEvent(PacketPlayInBlockPlace packet, Player p, HawkPlayer pp) {
        Material mat;
        if (packet.getItemStack() != null && packet.getItemStack().getItem() != null) {
            Block block = Block.a(packet.getItemStack().getItem());
            //noinspection deprecation
            mat = Material.getMaterial(Block.getId(block));
        } else {
            mat = null;
        }

        int x = packet.c();
        int y = packet.d();
        int z = packet.e();
        Vector targetedPosition = new Vector(x, y, z);
        InteractWorldEvent.BlockFace face;
        //Debug.broadcastMessage("FACE: " + packet.getFace());
        //Debug.broadcastMessage(x + " " + y + " " + z);
        //Debug.broadcastMessage(mat + "");

        InteractWorldEvent.InteractionType interactionType;
        //first vector is for 1.8 clients, second is for 1.7
        if(!targetedPosition.equals(new Vector(-1, -1, -1)) && !targetedPosition.equals(new Vector(-1, 255, -1))) {
            if(mat != null && mat != Material.AIR) {
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
            return new InteractItemEvent(p, pp, item, InteractItemEvent.Type.START_USE_ITEM, new WrappedPacket7(packet, WrappedPacket.PacketType.BLOCK_PLACE));
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

        Vector cursorPos = new Vector(packet.h(), packet.i(), packet.j());

        Location placedLocation = new Location(p.getWorld(), x, y, z);
        return new InteractWorldEvent(p, pp, placedLocation, mat, face, cursorPos, interactionType, new WrappedPacket7(packet, WrappedPacket.PacketType.BLOCK_PLACE));
    }

    private static ArmSwingEvent packetToArmSwingEvent(PacketPlayInArmAnimation packet, Player p, HawkPlayer pp) {
        return new ArmSwingEvent(p, pp, packet.d(), new WrappedPacket7(packet, WrappedPacket.PacketType.ARM_ANIMATION));
    }

    private static ItemSwitchEvent packetToItemSwitchEvent(PacketPlayInHeldItemSlot packet, Player p, HawkPlayer pp) {
        return new ItemSwitchEvent(p, pp, packet.c(), new WrappedPacket7(packet, WrappedPacket.PacketType.HELD_ITEM_SLOT));
    }

    private static PlayerActionEvent packetToPlayerActionEvent(PacketPlayInEntityAction packet, Player p, HawkPlayer pp) {
        int id = packet.d();
        PlayerActionEvent.PlayerAction action;
        switch (id) {
            case 1:
                action = PlayerActionEvent.PlayerAction.SNEAK_START;
                break;
            case 2:
                action = PlayerActionEvent.PlayerAction.SNEAK_STOP;
                break;
            case 3:
                action = PlayerActionEvent.PlayerAction.BED_LEAVE;
                break;
            case 4:
                action = PlayerActionEvent.PlayerAction.SPRINT_START;
                break;
            case 5:
                action = PlayerActionEvent.PlayerAction.SPRINT_STOP;
                break;
            case 6:
                action = PlayerActionEvent.PlayerAction.HORSE_JUMP;
                break;
            default:
                action = PlayerActionEvent.PlayerAction.UNKNOWN;
        }
        return new PlayerActionEvent(p, pp, new WrappedPacket7(packet, WrappedPacket.PacketType.ENTITY_ACTION), action);
    }

    //BEGIN of addition by Havesta
    private static ClickInventoryEvent packetToWindowClickEvent(PacketPlayInWindowClick packet, Player p, HawkPlayer pp) {
        // packet.b() is the item slot, 0-4 is crafting, 5-8 is armor, 9-44 is the rest of the inventory, when not in inventory clicked = -999

        // Debug.broadcastMessage("a= " + packet.a() + " b= " + packet.b() + " c= " + packet.c() + " d= " + packet.d() + " e= " + packet.e() + " f= " + packet.f());

        int windowID = packet.c();
        int slot = packet.d();
        int button = packet.e();
        int mode = packet.h();
        //perhaps get clicked ItemStack too?

        return new ClickInventoryEvent(p, pp, slot, windowID, mode, button, new WrappedPacket7(packet, WrappedPacket.PacketType.ENTITY_ACTION));
    }

    private static OpenPlayerInventoryEvent packetToOpenWindowEvent(PacketPlayInClientCommand packet, Player p, HawkPlayer pp) {
        if(packet.c().equals(EnumClientCommand.OPEN_INVENTORY_ACHIEVEMENT)) { // sadly this works only for player inventory
            return new OpenPlayerInventoryEvent(p, pp, new WrappedPacket7(packet, WrappedPacket.PacketType.ENTITY_ACTION));
        }
        return null;
    }

    private static CloseInventoryEvent packetToCloseWindowEvent(PacketPlayInCloseWindow packet, Player p, HawkPlayer pp) {
        return new CloseInventoryEvent(p, pp, new WrappedPacket7(packet, WrappedPacket.PacketType.ENTITY_ACTION));
    }
    //END of addition by Havesta
}
