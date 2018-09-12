package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.packets.WrappedPacket;
import org.bukkit.entity.Player;

public class AbilitiesEvent extends Event {

    private final boolean flying;

    public AbilitiesEvent(Player p, HawkPlayer pp, boolean flying, WrappedPacket packet) {
        super(p, pp, packet);
        this.flying = flying;
    }

    public boolean isFlying() {
        return flying;
    }
}
