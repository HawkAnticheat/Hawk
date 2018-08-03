package me.islandscout.hawk.checks;

import me.islandscout.hawk.utils.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import java.util.List;

//Any check extending BukkitCheck will listen to events via the EventHandler.
public abstract class BukkitCheck extends Check implements Listener {

    public BukkitCheck(String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelByDefault, flagByDefault, vlPassMultiplier, minVlFlag, flagCooldown, flag, punishCommands);
    }

    public BukkitCheck(String name, String flag) {
        super(name, true, true, true, 0.9, 5, 1000, flag, null);
    }

    protected void punishAndTryCancel(Player offender, Event e, Placeholder... placeholders) {
        if(cancel && (e instanceof Cancellable)) ((Cancellable)e).setCancelled(true);
        super.punish(offender, placeholders);
    }

}
