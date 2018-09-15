package me.islandscout.hawk.utils.packets;

import me.islandscout.hawk.events.*;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.ServerUtils;
import me.islandscout.hawk.utils.blocks.BlockNMS;
import me.islandscout.hawk.utils.blocks.BlockNMS8;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;

public final class PacketConverter8 {

    private PacketConverter8() {}

    public static Event packetToEvent(Object packet, Player p, HawkPlayer pp) {
        if(packet instanceof PacketPlayInFlying)  return packetToPosEvent((PacketPlayInFlying)packet, p, pp);
        if(packet instanceof PacketPlayInUseEntity) return packetToInterEvent((PacketPlayInUseEntity)packet, p, pp);
        if(packet instanceof PacketPlayInBlockDig) return packetToDigEvent((PacketPlayInBlockDig) packet, p, pp);
        if(packet instanceof PacketPlayInCustomPayload) return packetToPayloadEvent((PacketPlayInCustomPayload) packet, p, pp);
        if(packet instanceof PacketPlayInAbilities) return packetToAbilitiesEvent((PacketPlayInAbilities) packet, p, pp);
        if(packet instanceof PacketPlayInBlockPlace) return packetToBlockPlaceEvent((PacketPlayInBlockPlace) packet, p, pp);
        if(packet instanceof PacketPlayInArmAnimation) return packetToArmSwingEvent((PacketPlayInArmAnimation) packet, p, pp);
        else return null;
    }

    private static PositionEvent packetToPosEvent(PacketPlayInFlying packet, Player p, HawkPlayer pp) {
        //default position
        Location loc = PositionEvent.getLastPosition(pp);

        //There's an NPE here if someone teleports to another world using a dumb multi-world plugin (which sets the getTo location to null)
        //I don't believe it is my responsibility to "fix" this. If there are enough complaints, I MIGHT consider looking into it.
        loc = new Location(pp.getLocation().getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        WrappedPacket.PacketType pType = WrappedPacket.PacketType.FLYING;

        //update if has look
        if(packet.h()) {
            pType = WrappedPacket.PacketType.LOOK;
            loc.setYaw(packet.d());
            loc.setPitch(packet.e());
        }

        //update if has position
        if(packet.g()) {
            if(packet.h())
                pType = WrappedPacket.PacketType.POSITION_LOOK;
            else
                pType = WrappedPacket.PacketType.POSITION;
            loc.setX(packet.a());
            loc.setY(packet.b());
            loc.setZ(packet.c());
        }

        return new PositionEvent(p, loc, packet.f(), pp, new WrappedPacket8(packet, pType));
    }

    private static InteractEntityEvent packetToInterEvent(PacketPlayInUseEntity packet, Player p, HawkPlayer pp) {
        if(packet.a() == null) return null;
        InteractAction action;
        if(packet.a() == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) action = InteractAction.ATTACK;
        else action = InteractAction.INTERACT;
        //get interacted entity. phew.
        Entity nmsEntity = packet.a(((CraftWorld) pp.getLocation().getWorld()).getHandle());
        if(nmsEntity == null) return null; //interacting with a non-existent entity
        org.bukkit.entity.Entity entity = nmsEntity.getBukkitEntity();

        return new InteractEntityEvent(p, pp, action, entity, new WrappedPacket8(packet, WrappedPacket.PacketType.USE_ENTITY));
    }

    private static BlockDigEvent packetToDigEvent(PacketPlayInBlockDig packet, Player p, HawkPlayer pp) {
        BlockPosition pos = packet.a();
        Location loc = new Location(p.getWorld(), pos.getX(), pos.getY(), pos.getZ());
        if(loc.distanceSquared(p.getLocation()) > 64)
            return null;

        BlockNMS block = new BlockNMS8(ServerUtils.getBlockAsync(loc));

        PacketPlayInBlockDig.EnumPlayerDigType digType = packet.c();
        DigAction action;
        switch (digType) {
            case START_DESTROY_BLOCK:
                action = DigAction.START;
                break;
            case ABORT_DESTROY_BLOCK:
                action = DigAction.CANCEL;
                break;
            case STOP_DESTROY_BLOCK:
                action = DigAction.COMPLETE;
                break;
            default:
                action = DigAction.COMPLETE;
        }

        pp.setDigging(action == DigAction.START && block.getStrength() != 0);
        return new BlockDigEvent(p, pp, action, loc.getBlock(), new WrappedPacket8(packet, WrappedPacket.PacketType.BLOCK_DIG));
    }

    //TODO: work on this
    private static CustomPayLoadEvent packetToPayloadEvent(PacketPlayInCustomPayload packet, Player p, HawkPlayer pp) {
        return null;
    }

    private static AbilitiesEvent packetToAbilitiesEvent(PacketPlayInAbilities packet, Player p, HawkPlayer pp) {
        return new AbilitiesEvent(p, pp, packet.isFlying() && p.getAllowFlight(), new WrappedPacket8(packet, WrappedPacket.PacketType.ABILITIES));
    }

    //it appears that this gets called when interacting with blocks too
    private static BlockPlaceEvent packetToBlockPlaceEvent(PacketPlayInBlockPlace packet, Player p, HawkPlayer pp) {
        org.bukkit.Material mat;
        if(packet.getItemStack() != null && packet.getItemStack().getItem() != null) {
            Block block = Block.asBlock(packet.getItemStack().getItem());
            //noinspection deprecation
            mat = org.bukkit.Material.getMaterial(Block.getId(block));
        }
        else {
            mat = null;
        }

        BlockPosition bPos = packet.a();
        int x = bPos.getX();
        int y = bPos.getY();
        int z = bPos.getZ();
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
                y+= 1;
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
        if(y < 0)
            return null;
        return new BlockPlaceEvent(p, pp, new Location(p.getWorld(), x, y, z), mat, face, new WrappedPacket8(packet, WrappedPacket.PacketType.BLOCK_PLACE));
    }

    private static ArmSwingEvent packetToArmSwingEvent(PacketPlayInArmAnimation packet, Player p, HawkPlayer pp) {
        return new ArmSwingEvent(p, pp, 0, new WrappedPacket8(packet, WrappedPacket.PacketType.ARM_ANIMATION));
    }
}
