package me.islandscout.hawk.utils.packets;

import me.islandscout.hawk.events.*;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.blocks.BlockNMS;
import me.islandscout.hawk.utils.blocks.BlockNMS8;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.Player;

public class PacketConverter8 {

    public static Event packetToEvent(Object packet, Player p, HawkPlayer pp) {
        if(packet instanceof PacketPlayInFlying)  return packetToPosEvent((PacketPlayInFlying)packet, p, pp);
        if(packet instanceof PacketPlayInUseEntity) return packetToInterEvent((PacketPlayInUseEntity)packet, p, pp);
        if(packet instanceof PacketPlayInBlockDig) return packetToDigEvent((PacketPlayInBlockDig) packet, p, pp);
        if(packet instanceof PacketPlayInCustomPayload) return packetToPayloadEvent((PacketPlayInCustomPayload) packet, p);
        if(packet instanceof PacketPlayInAbilities) return packetToAbilitiesEvent((PacketPlayInAbilities) packet, p);

        else return null;
    }

    private static PositionEvent packetToPosEvent(PacketPlayInFlying packet, Player p, HawkPlayer pp) {
        Location loc = new Location(pp.getLocation().getWorld(),
                pp.getLocation().getX(),
                pp.getLocation().getY(),
                pp.getLocation().getZ(),
                pp.getLocation().getYaw(),
                pp.getLocation().getPitch());

        //has look
        if(packet.h()) {
            loc.setYaw(packet.d());
            loc.setPitch(packet.e());
        }

        //has position
        if(packet.g()) {
            loc.setX(packet.a());
            loc.setY(packet.b());
            loc.setZ(packet.c());
        }
        return new PositionEvent(p, loc, packet.f(), pp);
    }

    private static InteractEntityEvent packetToInterEvent(PacketPlayInUseEntity packet, Player p, HawkPlayer pp) {
        if(packet.a() == null) return null;
        InteractAction action;
        if(packet.a() == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) action = InteractAction.ATTACK;
        else action = InteractAction.INTERACT;
        //get interacted entity. phew.
        org.bukkit.entity.Entity entity = packet.a(((CraftWorld) pp.getLocation().getWorld()).getHandle()).getBukkitEntity();
        return new InteractEntityEvent(p, action, entity);
    }

    private static BlockDigEvent packetToDigEvent(PacketPlayInBlockDig packet, Player p, HawkPlayer pp) {
        BlockPosition pos = packet.a();
        Location loc = new Location(p.getWorld(), pos.getX(), pos.getY(), pos.getZ());
        BlockNMS block;

        //This is stupid
        //TODO: perhaps replace this with the shiny async block getting utility in ServerUtils
        if(loc.distanceSquared(p.getLocation()) > 64) return null;
        try {
            org.bukkit.block.Block b = loc.getBlock();
            block = new BlockNMS8(b);
        }
        catch (IllegalStateException e) {
            return null;
        }

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
        return new BlockDigEvent(p, action, loc.getBlock());
    }

    //TODO: work on this
    private static CustomPayLoadEvent packetToPayloadEvent(PacketPlayInCustomPayload packet, Player p) {
        return null;
    }

    private static AbilitiesEvent packetToAbilitiesEvent(PacketPlayInAbilities packet, Player p) {
        return new AbilitiesEvent(p, packet.isFlying() && p.getAllowFlight());
    }
}
