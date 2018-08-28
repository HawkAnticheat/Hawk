package me.islandscout.hawk.checks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.events.BlockPlaceEvent;
import me.islandscout.hawk.utils.Placeholder;
import me.islandscout.hawk.utils.ServerUtils;
import me.islandscout.hawk.utils.blocks.BlockNMS7;
import me.islandscout.hawk.utils.blocks.BlockNMS8;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class AsyncBlockPlacementCheck extends AsyncCheck<BlockPlaceEvent> {

    public AsyncBlockPlacementCheck(String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelByDefault, flagByDefault, vlPassMultiplier, minVlFlag, flagCooldown, flag, punishCommands);
    }

    public AsyncBlockPlacementCheck(String name, String flag) {
        super(name, true, true, true, 0.9, 5, 1000, flag, null);
    }

    protected void punishAndTryCancelAndBlockDestroy(HawkPlayer offender, BlockPlaceEvent event, Placeholder... placeholders) {
        punish(offender, true, event, placeholders);
        Block b = ServerUtils.getBlockAsync(event.getLocation());
        if(b == null)
            return;
        if(Hawk.getServerVersion() == 7) {
            BlockNMS7.getBlockNMS(b).sendPacketToPlayer(offender.getPlayer());
        }
        else if(Hawk.getServerVersion() == 8) {
            BlockNMS8.getBlockNMS(b).sendPacketToPlayer(offender.getPlayer());
        }
    }
}
