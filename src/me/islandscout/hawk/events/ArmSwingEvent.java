package me.islandscout.hawk.events;

import org.bukkit.entity.Player;

public class ArmSwingEvent extends Event {

    private int type;

    public ArmSwingEvent(Player p, int type) {
        super(p);
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
