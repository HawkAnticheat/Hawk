package me.islandscout.hawk.checks.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncBlockDigCheck;
import me.islandscout.hawk.events.DigAction;
import me.islandscout.hawk.events.BlockDigEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WrongBlock extends AsyncBlockDigCheck {

    //PASSED (9/11/18)

    private Map<UUID, Block> blockinteracted;

    public WrongBlock() {
        super("wrongblock", "&7%player% failed wrong block. VL: %vl%");
        blockinteracted = new HashMap<>();
    }

    public void check(BlockDigEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Block b = e.getBlock();
        if(e.getDigAction() == DigAction.START) {
            blockinteracted.put(p.getUniqueId(), e.getBlock());
        }
        else if(e.getDigAction() == DigAction.COMPLETE) {
            if((!blockinteracted.containsKey(p.getUniqueId()) || !b.equals(blockinteracted.get(p.getUniqueId())))) {
                punishAndTryCancelAndBlockRespawn(pp, e);
            }
            else
                reward(pp);
        }

    }

    public void removeData(Player p) {
        blockinteracted.remove(p.getUniqueId());
    }
}
