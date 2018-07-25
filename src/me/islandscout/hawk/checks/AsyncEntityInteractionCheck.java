package me.islandscout.hawk.checks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.events.InteractEntityEvent;

import java.util.List;

public abstract class AsyncEntityInteractionCheck extends AsyncCheck<InteractEntityEvent> {

    public AsyncEntityInteractionCheck(Hawk hawk, String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        super(hawk, name, enabled, cancelByDefault, flagByDefault, vlPassMultiplier, minVlFlag, flagCooldown, flag, punishCommands);
    }

    public AsyncEntityInteractionCheck(Hawk hawk, String name, String flag) {
        super(hawk, name, true, true, true, 0.9, 5, 1000, flag, null);
    }
}
