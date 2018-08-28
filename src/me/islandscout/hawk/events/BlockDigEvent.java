package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockDigEvent extends Event {

    private DigAction digAction;
    private Block block;

    public BlockDigEvent(Player p, HawkPlayer pp, DigAction action, Block block) {
        super(p, pp);
        digAction = action;
        this.block = block;
    }

    public DigAction getDigAction() {
        return digAction;
    }

    public Block getBlock() {
        return block;
    }
}
