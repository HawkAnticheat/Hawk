package me.islandscout.hawk.checks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.events.BlockDigEvent;
import me.islandscout.hawk.utils.Placeholder;
import me.islandscout.hawk.utils.blocks.BlockNMS7;
import me.islandscout.hawk.utils.blocks.BlockNMS8;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class AsyncBlockDigCheck extends AsyncCheck<BlockDigEvent> {

    public AsyncBlockDigCheck(String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelByDefault, flagByDefault, vlPassMultiplier, minVlFlag, flagCooldown, flag, punishCommands);
    }

    public AsyncBlockDigCheck(String name, String flag) {
        super(name, true, true, true, 0.9, 5, 1000, flag, null);
    }

    protected void punishAndTryCancelAndBlockRespawn(Player offender, BlockDigEvent event, Placeholder... placeholders) {
        punish(offender, true, event, placeholders);
        if(Hawk.getServerVersion() == 7) {
            BlockNMS7.getBlockNMS(event.getBlock()).sendPacketToPlayer(offender);
        }
        else if(Hawk.getServerVersion() == 8) {
            BlockNMS8.getBlockNMS(event.getBlock()).sendPacketToPlayer(offender);
        }
    }
}
