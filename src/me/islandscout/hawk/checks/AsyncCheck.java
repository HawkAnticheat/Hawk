package me.islandscout.hawk.checks;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.events.Event;
import me.islandscout.hawk.utils.Placeholder;
import org.bukkit.entity.Player;

import java.util.List;

//This should extend Check. Subcategories should extend this. Checks should not extend this.
abstract class AsyncCheck<E extends Event> extends Check {

    AsyncCheck(String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelByDefault, flagByDefault, vlPassMultiplier, minVlFlag, flagCooldown, flag, punishCommands);
    }

    void checkEvent(E e) {
        if(e.getPlayer().hasPermission(permission) || hawk.getCheckManager().getExemptList().containsPlayer(e.getPlayer().getUniqueId()) || !enabled)
            return;
        check(e);
    }

    //assume player does not have permission to bypass and this check is enabled.
    protected abstract void check(E e);

    protected void punish(HawkPlayer offender, boolean tryCancel, E e, Placeholder... placeholders) {
        if(cancel && tryCancel)
            e.setCancelled(true);
        super.punish(offender, placeholders);
    }

    @Override
    protected void punish(HawkPlayer offender, Placeholder... placeholders) {
        super.punish(offender, placeholders);
    }
}
