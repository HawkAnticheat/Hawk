package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.packets.WrappedPacket;
import org.bukkit.entity.Player;

public class ArmSwingEvent extends Event {

    private int type;

    public ArmSwingEvent(Player p, HawkPlayer pp, int type, WrappedPacket packet) {
        super(p, pp, packet);
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
