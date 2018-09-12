package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.packets.WrappedPacket;
import org.bukkit.entity.Player;

public abstract class Event {

    protected boolean cancelled;
    protected Player p;
    protected HawkPlayer pp;
    protected WrappedPacket wPacket;

    public Event(Player p, HawkPlayer pp, WrappedPacket wPacket) {
        this.p = p;
        this.pp = pp;
        this.wPacket = wPacket;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Player getPlayer() {
        return p;
    }

    public HawkPlayer getHawkPlayer() {
        return pp;
    }

    public WrappedPacket getWrappedPacket() {
        return wPacket;
    }
}
