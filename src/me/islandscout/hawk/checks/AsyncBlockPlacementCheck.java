package me.islandscout.hawk.checks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.events.BlockPlaceEvent;
import me.islandscout.hawk.utils.Placeholder;
import me.islandscout.hawk.utils.ServerUtils;
import me.islandscout.hawk.utils.blocks.BlockNMS7;
import me.islandscout.hawk.utils.blocks.BlockNMS8;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class AsyncBlockPlacementCheck extends AsyncCheck<BlockPlaceEvent> {

    public AsyncBlockPlacementCheck(Hawk hawk, String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        super(hawk, name, enabled, cancelByDefault, flagByDefault, vlPassMultiplier, minVlFlag, flagCooldown, flag, punishCommands);
    }

    public AsyncBlockPlacementCheck(Hawk hawk, String name, String flag) {
        super(hawk, name, true, true, true, 0.9, 5, 1000, flag, null);
    }

    protected void punishAndTryCancelAndBlockDestroy(Player offender, BlockPlaceEvent event, Placeholder... placeholders) {
        punishAndTryCancel(offender, event, placeholders);
        Block b = ServerUtils.getBlockAsync(event.getLocation());
        if(b == null)
            return;
        if(Hawk.getServerVersion() == 7) {
            BlockNMS7.getBlockNMS(b).sendPacketToPlayer(offender);
        }
        else if(Hawk.getServerVersion() == 8) {
            BlockNMS8.getBlockNMS(b).sendPacketToPlayer(offender);
        }
    }
}
