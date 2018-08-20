package me.islandscout.hawk.utils.packets;

import me.islandscout.hawk.events.*;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.Debug;
import me.islandscout.hawk.utils.ServerUtils;
import me.islandscout.hawk.utils.blocks.BlockNMS;
import me.islandscout.hawk.utils.blocks.BlockNMS7;
import net.minecraft.server.v1_7_R4.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.*;

public class PacketConverter7 {

    public static Event packetToEvent(Object packet, Player p, HawkPlayer pp) {
        if(packet instanceof PacketPlayInFlying)  return packetToPosEvent((PacketPlayInFlying)packet, p, pp);
        if(packet instanceof PacketPlayInUseEntity) return packetToInterEvent((PacketPlayInUseEntity) packet, p, pp);
        if(packet instanceof PacketPlayInBlockDig) return packetToDigEvent((PacketPlayInBlockDig) packet, p, pp);
        if(packet instanceof PacketPlayInCustomPayload) return packetToPayloadEvent((PacketPlayInCustomPayload) packet, p);
        if(packet instanceof PacketPlayInAbilities) return packetToAbilitiesEvent((PacketPlayInAbilities) packet, p);
        if(packet instanceof PacketPlayInBlockPlace) return packetToBlockPlaceEvent((PacketPlayInBlockPlace) packet, p);
        if(packet instanceof PacketPlayInArmAnimation) return packetToArmSwingEvent((PacketPlayInArmAnimation) packet, p);
        return null;
    }

    private static PositionEvent packetToPosEvent(PacketPlayInFlying packet, Player p, HawkPlayer pp) {
        Location loc = new Location(pp.getLocation().getWorld(),
                pp.getLocation().getX(),
                pp.getLocation().getY(),
                pp.getLocation().getZ(),
                pp.getLocation().getYaw(),
                pp.getLocation().getPitch());

        //has look
        if(packet.k()) {
            loc.setYaw(packet.g());
            loc.setPitch(packet.h());
        }

        //has position
        if(packet.j()) {
            loc.setX(packet.c());
            loc.setY(packet.d());
            loc.setZ(packet.e());
        }
        return new PositionEvent(p, loc, packet.i(), pp);
    }

    private static InteractEntityEvent packetToInterEvent(PacketPlayInUseEntity packet, Player p, HawkPlayer pp) {
        if(packet.c() == null) return null;
        InteractAction action;
        if(packet.c() == EnumEntityUseAction.ATTACK) action = InteractAction.ATTACK;
        else action = InteractAction.INTERACT;
        //get interacted entity. phew.
        org.bukkit.entity.Entity entity = packet.a(((CraftWorld) pp.getLocation().getWorld()).getHandle()).getBukkitEntity();
        return new InteractEntityEvent(p, action, entity);
    }

    private static BlockDigEvent packetToDigEvent(PacketPlayInBlockDig packet, Player p, HawkPlayer pp) {
        Location loc = new Location(p.getWorld(), packet.c(), packet.d(), packet.e());

        org.bukkit.block.Block b = ServerUtils.getBlockAsync(loc);
        if(b == null || packet.f() == 255 || (packet.f() == 0 && loc.getBlockY() == 0))
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
        return new BlockDigEvent(p, action, b);
    }

    private static CustomPayLoadEvent packetToPayloadEvent(PacketPlayInCustomPayload packet, Player p) {
        return new CustomPayLoadEvent(packet.c(), packet.length, packet.e(), p);
    }

    private static AbilitiesEvent packetToAbilitiesEvent(PacketPlayInAbilities packet, Player p) {
        return new AbilitiesEvent(p, packet.isFlying() && p.getAllowFlight());
    }

    //it appears that this gets called when interacting with blocks too
    private static BlockPlaceEvent packetToBlockPlaceEvent(PacketPlayInBlockPlace packet, Player p) {
        Material mat;
        if(packet.getItemStack() != null && packet.getItemStack().getItem() != null) {
            Block block = Block.a(packet.getItemStack().getItem());
            //noinspection deprecation
            mat = Material.getMaterial(Block.getId(block));
        }
        else {
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
        return new BlockPlaceEvent(p, new Location(p.getWorld(), x, y, z), mat, face);
    }

    private static ArmSwingEvent packetToArmSwingEvent(PacketPlayInArmAnimation packet, Player p ) {
        return new ArmSwingEvent(p, packet.d());
    }
}
