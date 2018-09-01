package me.islandscout.hawk.checks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.events.BlockDigEvent;
import me.islandscout.hawk.utils.Placeholder;
import me.islandscout.hawk.utils.blocks.BlockNMS7;
import me.islandscout.hawk.utils.blocks.BlockNMS8;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class AsyncBlockDigCheck extends AsyncCheck<BlockDigEvent> {

    public AsyncBlockDigCheck(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelThreshold, flagThreshold, vlPassMultiplier, flagCooldown, flag, punishCommands);
    }

    public AsyncBlockDigCheck(String name, String flag) {
        super(name, true, 0, 5, 0.9, 1000, flag, null);
    }

    protected void punishAndTryCancelAndBlockRespawn(HawkPlayer offender, BlockDigEvent event, Placeholder... placeholders) {
        punish(offender, true, event, placeholders);
        if(offender.getVL(this) < cancelThreshold)
            return;
        if(Hawk.getServerVersion() == 7) {
            BlockNMS7.getBlockNMS(event.getBlock()).sendPacketToPlayer(offender.getPlayer());
        }
        else if(Hawk.getServerVersion() == 8) {
            BlockNMS8.getBlockNMS(event.getBlock()).sendPacketToPlayer(offender.getPlayer());
        }
    }
}
