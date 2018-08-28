package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.entity.Player;

public class CustomPayLoadEvent extends Event {

    private String tag;
    private int length;
    private byte[] data;

    public CustomPayLoadEvent(String tag, int length, byte[] data, Player p, HawkPlayer pp) {
        super(p, pp);
        this.tag = tag;
        this.length = length;
        this.data = data;
    }

    public String getTag() {
        return tag;
    }

    public int getLength() {
        return length;
    }

    public byte[] getData() {
        return data;
    }
}
