package me.islandscout.hawk.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockDigEvent extends Event {

    private DigAction digAction;
    private Block block;

    public BlockDigEvent(Player p, DigAction action, Block block) {
        super(p);
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
