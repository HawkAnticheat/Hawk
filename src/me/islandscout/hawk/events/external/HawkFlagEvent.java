package me.islandscout.hawk.events.external;

import me.islandscout.hawk.utils.Violation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class HawkFlagEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Violation violation;

    public HawkFlagEvent(Violation violation) {
        this.violation = violation;

    }

    public Player getPlayer() {
        return Bukkit.getPlayer(violation.getPlayerUUID());
    }

    public long getTimeMillis() {
        return violation.getTime();
    }

    public short getPing() {
        return violation.getPing();
    }

    public short getVl() {
        return violation.getVl();
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
