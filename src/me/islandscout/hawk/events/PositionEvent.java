package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.AdjacentBlocks;
import me.islandscout.hawk.utils.packets.WrappedPacket;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class PositionEvent extends Event {

    //Remember that the client only updates position/rotation information if
    //it is significant enough. Use hasDeltaPos() hasDeltaRot() when necessary.

    //Position events will not pass through checks if the player is teleporting.

    private boolean onGround;
    private boolean onGroundReally;
    private boolean teleported;
    private Location cancelLocation;

    private static Map<UUID, Location> last = new HashMap<>();
    private static Map<UUID, Location> current = new HashMap<>();

    public PositionEvent(Player p, Location update, boolean onGround, HawkPlayer pp, WrappedPacket packet) {
        super(p, pp, packet);
        last.put(p.getUniqueId(), current.getOrDefault(p.getUniqueId(), pp.getLocation()));
        current.put(p.getUniqueId(), update);
        this.onGround = onGround;
        onGroundReally = AdjacentBlocks.onGroundReally(update, update.getY() - last.getOrDefault(p.getUniqueId(), pp.getLocation()).getY(), true);
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

    public boolean hasDeltaPos() {
        return getTo().getX() != getFrom().getX() || getTo().getY() != getFrom().getY() || getTo().getZ() != getFrom().getZ();
    }

    public boolean hasDeltaRot() {
        return getTo().getYaw() != getFrom().getYaw() || getTo().getPitch() != getFrom().getPitch();
    }

    public void cancelAndSetBack(Location setback) {
        if(!isCancelled()) {
            cancelLocation = setback;
            setCancelled(true);
            pp.setTeleporting(true);
            pp.setTeleportLoc(setback);
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
