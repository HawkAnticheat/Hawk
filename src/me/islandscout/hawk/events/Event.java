package me.islandscout.hawk.events;

import org.bukkit.entity.Player;

public abstract class Event {

    protected boolean cancelled;
    protected Player p;

    public Event(Player p) {
        this.p = p;
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
}
