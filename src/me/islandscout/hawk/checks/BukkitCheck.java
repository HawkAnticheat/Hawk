package me.islandscout.hawk.checks;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.Placeholder;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import java.util.List;

//Any check extending BukkitCheck will listen to events via the EventHandler.
public abstract class BukkitCheck extends Check implements Listener {

    public BukkitCheck(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelThreshold, flagThreshold, vlPassMultiplier, flagCooldown, flag, punishCommands);
    }

    public BukkitCheck(String name, String flag) {
        super(name, true, 0, 5, 0.9,  1000, flag, null);
    }

    protected void punishAndTryCancel(HawkPlayer offender, Event e, Placeholder... placeholders) {
        if(offender.getVL(this) >= cancelThreshold && (e instanceof Cancellable)) ((Cancellable)e).setCancelled(true);
        super.punish(offender, placeholders);
    }

}
