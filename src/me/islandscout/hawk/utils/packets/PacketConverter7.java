/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.utils.packets;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.events.*;
import me.islandscout.hawk.utils.ServerUtils;
import me.islandscout.hawk.utils.blocks.BlockNMS;
import me.islandscout.hawk.utils.blocks.BlockNMS7;
import net.minecraft.server.v1_7_R4.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.Player;

public final class PacketConverter7 {

    private PacketConverter7() {
    }

    public static Event packetToEvent(Object packet, Player p, HawkPlayer pp) {
        if (packet instanceof PacketPlayInFlying) return packetToPosEvent((PacketPlayInFlying) packet, p, pp);
        if (packet instanceof PacketPlayInUseEntity) return packetToInterEvent((PacketPlayInUseEntity) packet, p, pp);
        if (packet instanceof PacketPlayInBlockDig) return packetToDigEvent((PacketPlayInBlockDig) packet, p, pp);
        if (packet instanceof PacketPlayInCustomPayload)
            return packetToPayloadEvent((PacketPlayInCustomPayload) packet, p, pp);
        if (packet instanceof PacketPlayInAbilities)
            return packetToAbilitiesEvent((PacketPlayInAbilities) packet, p, pp);
        if (packet instanceof PacketPlayInBlockPlace)
            return packetToBlockPlaceEvent((PacketPlayInBlockPlace) packet, p, pp);
        if (packet instanceof PacketPlayInArmAnimation)
            return packetToArmSwingEvent((PacketPlayInArmAnimation) packet, p, pp);
        return null;
    }

    private static PositionEvent packetToPosEvent(PacketPlayInFlying packet, Player p, HawkPlayer pp) {
        //default position
        Location loc = PositionEvent.getLastPosition(pp);

        //There's an NPE here if someone teleports to another world using a dumb multi-world plugin (which sets the PlayerTeleportEvent#getTo() location to null)
        //I don't believe it is my responsibility to "fix" this. If there are enough complaints, I MIGHT consider looking into it.
        loc = new Location(pp.getLocation().getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        WrappedPacket.PacketType pType = WrappedPacket.PacketType.FLYING;

        //update if has look
        if (packet.k()) {
            pType = WrappedPacket.PacketType.LOOK;
            loc.setYaw(packet.g());
            loc.setPitch(packet.h());
        }

        //update if has position
        if (packet.j()) {
            if (packet.k())
                pType = WrappedPacket.PacketType.POSITION_LOOK;
            else
                pType = WrappedPacket.PacketType.POSITION;
            loc.setX(packet.c());
            loc.setY(packet.d());
            loc.setZ(packet.e());
        }

        return new PositionEvent(p, loc, packet.i(), pp, new WrappedPacket7(packet, pType));
    }

    private static InteractEntityEvent packetToInterEvent(PacketPlayInUseEntity packet, Player p, HawkPlayer pp) {
        if (packet.c() == null) return null;
        InteractAction action;
        if (packet.c() == EnumEntityUseAction.ATTACK) action = InteractAction.ATTACK;
        else action = InteractAction.INTERACT;
        //get interacted entity. phew.
        Entity nmsEntity = packet.a(((CraftWorld) pp.getLocation().getWorld()).getHandle());
        if (nmsEntity == null) return null; //interacting with a non-existent entity
        org.bukkit.entity.Entity entity = nmsEntity.getBukkitEntity();
        return new InteractEntityEvent(p, pp, action, entity, new WrappedPacket7(packet, WrappedPacket.PacketType.USE_ENTITY));
    }

    private static BlockDigEvent packetToDigEvent(PacketPlayInBlockDig packet, Player p, HawkPlayer pp) {
        Location loc = new Location(p.getWorld(), packet.c(), packet.d(), packet.e());

        org.bukkit.block.Block b = ServerUtils.getBlockAsync(loc);
        if (b == null || packet.f() == 255 || (packet.f() == 0 && loc.getBlockY() == 0))
            return null;
        BlockNMS block = new BlockNMS7(b);

        int status = packet.g();
        DigAction action;
        switch (status) {
            case 0:
                action = DigAction.START;
                break;
            case 1:
                action = DigAction.CANCEL;
                break;
            case 2:
                action = DigAction.COMPLETE;
                break;
            default:
                action = DigAction.COMPLETE;
        }
        pp.setDigging(action == DigAction.START && block.getStrength() != 0);
        return new BlockDigEvent(p, pp, action, b, new WrappedPacket7(packet, WrappedPacket.PacketType.BLOCK_DIG));
    }

    private static CustomPayLoadEvent packetToPayloadEvent(PacketPlayInCustomPayload packet, Player p, HawkPlayer pp) {
        return new CustomPayLoadEvent(packet.c(), packet.length, packet.e(), p, pp, new WrappedPacket7(packet, WrappedPacket.PacketType.CUSTOM_PAYLOAD));
    }

    private static AbilitiesEvent packetToAbilitiesEvent(PacketPlayInAbilities packet, Player p, HawkPlayer pp) {
        return new AbilitiesEvent(p, pp, packet.isFlying() && p.getAllowFlight(), new WrappedPacket7(packet, WrappedPacket.PacketType.ABILITIES));
    }

    //it appears that this gets called when interacting with blocks too
    private static BlockPlaceEvent packetToBlockPlaceEvent(PacketPlayInBlockPlace packet, Player p, HawkPlayer pp) {
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
        BlockPlaceEvent.BlockFace face;
        //Debug.broadcastMessage("FACE: " + packet.getFace());
        //Debug.broadcastMessage(x + " " + y + " " + z);
        //Debug.broadcastMessage(packet.h() + "");
        switch (packet.getFace()) {
            case 0:
                face = BlockPlaceEvent.BlockFace.BOTTOM;
                y -= 1;
                break;
            case 1:
                face = BlockPlaceEvent.BlockFace.TOP;
                y += 1;
                break;
            case 2:
                face = BlockPlaceEvent.BlockFace.NORTH;
                z -= 1;
                break;
            case 3:
                face = BlockPlaceEvent.BlockFace.SOUTH;
                z += 1;
                break;
            case 4:
                face = BlockPlaceEvent.BlockFace.WEST;
                x -= 1;
                break;
            case 5:
                face = BlockPlaceEvent.BlockFace.EAST;
                x += 1;
                break;
            default:
                return null;
        }
        if (y < 0)
            return null;
        return new BlockPlaceEvent(p, pp, new Location(p.getWorld(), x, y, z), mat, face, new WrappedPacket7(packet, WrappedPacket.PacketType.BLOCK_PLACE));
    }

    private static ArmSwingEvent packetToArmSwingEvent(PacketPlayInArmAnimation packet, Player p, HawkPlayer pp) {
        return new ArmSwingEvent(p, pp, packet.d(), new WrappedPacket7(packet, WrappedPacket.PacketType.ARM_ANIMATION));
    }
}
