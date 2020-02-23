package me.islandscout.hawk.event.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class HawkAsyncPlayerAbilitiesEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean invulnerable;
    private boolean allowedToFly;
    private boolean flying;
    private boolean creativeMode;
    private float flySpeed;
    private float walkSpeed;
    private Player p;
    private boolean cancelled;

    public HawkAsyncPlayerAbilitiesEvent(Player p, boolean invulnerable, boolean allowedToFly, boolean flying,
                                         boolean creativeMode, float flySpeed, float walkSpeed) {
        super(true);
        this.invulnerable = invulnerable;
        this.allowedToFly = allowedToFly;
        this.flying = flying;
        this.creativeMode = creativeMode;
        this.flySpeed = flySpeed;
        this.walkSpeed = walkSpeed;
        this.p = p;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public boolean isAllowedToFly() {
        return allowedToFly;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isCreativeMode() {
        return creativeMode;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public Player getPlayer() {
        return p;
    }
}
