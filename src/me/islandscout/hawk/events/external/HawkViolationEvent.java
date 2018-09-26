package me.islandscout.hawk.events.external;

import me.islandscout.hawk.utils.Violation;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class HawkViolationEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private Violation violation;

    public HawkViolationEvent(Violation violation) {
        super(true); //TODO: make sure to check if the thread is async or not!
        this.violation = violation;
    }

    public Violation getViolation() {
        return violation;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
