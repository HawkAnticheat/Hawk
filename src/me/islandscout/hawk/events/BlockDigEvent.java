package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.packets.WrappedPacket;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockDigEvent extends Event {

    private DigAction digAction;
    private Block block;

    public BlockDigEvent(Player p, HawkPlayer pp, DigAction action, Block block, WrappedPacket packet) {
        super(p, pp, packet);
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
