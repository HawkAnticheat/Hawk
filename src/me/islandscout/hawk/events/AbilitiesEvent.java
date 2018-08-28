package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.entity.Player;

public class AbilitiesEvent extends Event {

    private final boolean flying;

    public AbilitiesEvent(Player p, HawkPlayer pp, boolean flying) {
        super(p, pp);
        this.flying = flying;
    }

    public boolean isFlying() {
        return flying;
    }
}
