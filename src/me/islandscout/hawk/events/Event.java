package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.entity.Player;

public abstract class Event {

    protected boolean cancelled;
    protected Player p;
    protected HawkPlayer pp;

    public Event(Player p, HawkPlayer pp) {
        this.p = p;
        this.pp = pp;
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
}
