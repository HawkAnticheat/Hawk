package me.islandscout.hawk.modules;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.events.AbilitiesEvent;
import me.islandscout.hawk.events.BlockPlaceEvent;
import me.islandscout.hawk.events.Event;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.listener.packets.PacketListener7;
import me.islandscout.hawk.listener.packets.PacketListener8;
import me.islandscout.hawk.utils.Debug;
import me.islandscout.hawk.utils.PhantomBlock;
import me.islandscout.hawk.utils.packets.PacketConverter7;
import me.islandscout.hawk.utils.packets.PacketConverter8;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * This class is mainly used to process packets that are intercepted from the Netty channels.
 * Remember, caution is advised when accessing the Bukkit API from the Netty thread.
 */
public class PacketCore {

    //TODO: Process incoming packets as well as outgoing packets (example: player abilities. This is to know async if a player is flying)
    //TODO: Multiple teleports in succession may screw up the teleport handler. Might have to make a list of all active teleport locations
    //TODO: Process valid events (after they've been processed by hawk) and make an event system

    //Welcome to TCP damnation.

    private int serverVersion;
    private final Hawk hawk;
    private PacketListener7 packetListener7;
    private PacketListener8 packetListener8;

    public PacketCore(int serverVersion, Hawk hawk) {
        this.serverVersion = serverVersion;
        this.hawk = hawk;
        try {
            if(serverVersion == 7) {
                packetListener7 = new PacketListener7(this);
                hawk.getLogger().info("Using MC 1.7_R4 Netty libraries for packet interception.");
            }
            else if(serverVersion == 8) {
                packetListener8 = new PacketListener8(this);
                hawk.getLogger().info("Using MC 1.8_R3 Netty libraries for packet interception.");
            }
            else warnConsole(hawk);
        }
        catch (NoClassDefFoundError e) {
            e.printStackTrace();
            warnConsole(hawk);
        }
    }

    private void warnConsole(Hawk hawk) {
        hawk.getLogger().warning("!!!!!!!!!!");
        hawk.getLogger().warning("It appears that you are not running Hawk on a 1.7.10 or 1.8.8 server.");
        hawk.getLogger().warning("Hawk will NOT work. Please run Hawk on a 1.7_R4 or 1.8_R3 server.");
        hawk.getLogger().warning("!!!!!!!!!!");
        Bukkit.getPluginManager().disablePlugin(hawk);
    }

    public boolean process(Object packet, Player p) {
        HawkPlayer pp = hawk.getHawkPlayer(p);

        //ignore packets while player is no longer registered in Hawk
        if(!pp.isOnline())
            return false;

        Event event;
        if(serverVersion == 8)
            event = PacketConverter8.packetToEvent(packet, p, pp);
        else if(serverVersion == 7)
            event = PacketConverter7.packetToEvent(packet, p, pp);
        else
            return true;
        if(event == null)
            return true;

        //handle teleports
        if(event instanceof PositionEvent) {
            pp.setLastMoveTime(System.currentTimeMillis());
            ((PositionEvent) event).setTeleported(false);
            if(pp.isTeleporting()) {
                if(pp.getTeleportLoc().getWorld().equals(((PositionEvent) event).getFrom().getWorld())) {
                    if(((PositionEvent) event).getTo().distanceSquared(pp.getTeleportLoc()) < 0.000001) {
                        ((PositionEvent) event).setFrom(((PositionEvent) event).getTo());
                        pp.setTeleporting(false);
                        ((PositionEvent) event).setTeleported(true);
                    }
                    else {
                        //TODO: If a player doesn't accept a teleport, kick em.
                        return false;
                    }
                }
            }
            //handle illegal move
            if(((PositionEvent) event).getTo().distanceSquared(((PositionEvent) event).getFrom()) > 64) {
                hawk.getLogger().warning(p.getName() + " may have tried to crash the server by moving too far! Distance: " + (((PositionEvent) event).getTo().distance(((PositionEvent) event).getFrom())));
                ((PositionEvent) event).cancelAndSetBack(p.getLocation(), hawk);
                pp.kickPlayer("Illegal move");
                return false;
            }
        }

        //handle block placing
        if(event instanceof BlockPlaceEvent) {
            BlockPlaceEvent bPlaceEvent = (BlockPlaceEvent)event;
            if(bPlaceEvent.getLocation().distanceSquared(pp.getLocation()) < 36) {
                PhantomBlock phantomBlock = new PhantomBlock(bPlaceEvent.getLocation(), bPlaceEvent.getMaterial());
                pp.addPhantomBlock(phantomBlock);
            }
        }

        hawk.getCheckManager().dispatchEvent(event);

        //update HawkPlayer
        if(event instanceof PositionEvent) {
            if(event.isCancelled() && ((PositionEvent) event).getCancelLocation() != null) {
                //setTo(setback);
                ((PositionEvent) event).setTo(((PositionEvent) event).getCancelLocation());
                pp.setTeleporting(true);
            }
            else {
                Location to = ((PositionEvent) event).getTo();
                Location from = ((PositionEvent) event).getFrom();
                pp.setVelocity(new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ()));
                pp.setDeltaYaw(to.getYaw() - from.getYaw());
                pp.setDeltaPitch(to.getPitch() - from.getPitch());
                pp.setLocation(to);
                pp.setOnGround(((PositionEvent) event).isOnGround());
            }

        }
        if(event instanceof AbilitiesEvent && !event.isCancelled()) {
            pp.setFlying(((AbilitiesEvent) event).isFlying());
        }

        return !event.isCancelled();
    }

    public void killListener() {
        if(serverVersion == 8) {
            packetListener8.stop();
        }
        else if(serverVersion == 7) {
            packetListener7.stop();
        }
    }

    public void setupListenerOnlinePlayers() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            hawk.getHawkPlayer(p).setOnline(true);
            setupListenerForPlayer(p);
        }
    }

    public void setupListenerForPlayer(Player p) {
        if(serverVersion == 8) {
            packetListener8.start(p);
        }
        else if(serverVersion == 7) {
            packetListener7.start(p);
        }
    }
}
