package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.entity.Player;

public class ArmSwingEvent extends Event {

    private int type;

    public ArmSwingEvent(Player p, HawkPlayer pp, int type) {
        super(p, pp);
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
