package me.islandscout.hawk.events;

import org.bukkit.entity.Player;

public class AbilitiesEvent extends Event {

    private final boolean flying;

    public AbilitiesEvent(Player p, boolean flying) {
        super(p);
        this.flying = flying;
    }

    public boolean isFlying() {
        return flying;
    }
}
