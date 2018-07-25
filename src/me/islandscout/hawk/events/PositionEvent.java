package me.islandscout.hawk.events;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.AdjacentBlocks;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public class PositionEvent extends Event {

    private boolean onGround;
    private boolean onGroundReally;
    private boolean teleported;
    private Location cancelLocation;
    private HawkPlayer pp;
    private static Map<UUID, Location> last = new HashMap<>();
    private static Map<UUID, Location> current = new HashMap<>();

    public PositionEvent(Player p, Location update, boolean onGround, HawkPlayer pp) {
        super(p);
        last.put(p.getUniqueId(), current.getOrDefault(p.getUniqueId(), pp.getLocation()));
        current.put(p.getUniqueId(), update);
        this.onGround = onGround;
        this.pp = pp;
        onGroundReally = AdjacentBlocks.onGroundReally(update, update.getY() - last.get(p.getUniqueId()).getY());
    }

    public Player getPlayer() {
        return p;
    }

    public Location getTo() {
        return current.get(p.getUniqueId());
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

    public boolean hasTeleported() {
        return teleported;
    }

    public void setTeleported(boolean teleported) {
        this.teleported = teleported;
    }

    public Location getCancelLocation() {
        return cancelLocation;
    }

    //If there are issues with setbacks, then this is probably it. 6/26/18
    public void cancelAndSetBack(Location setback, Hawk hawk) {
        if(!isCancelled()) { //added this recently to work around the comment below this line. This should somewhat fix movement checks interfering with each other.
            //setTo(setback); //This is causing problems with setbacks and false pos and "Illegal move". Moved to PacketCore 6/27/18
            cancelLocation = setback; //added 6/27/18
            setCancelled(true);
            HawkPlayer pp = hawk.getHawkPlayer(p);
            pp.setTeleporting(true);
            pp.setTeleportLoc(setback);
            pp.teleportPlayer(setback, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
    }

}
