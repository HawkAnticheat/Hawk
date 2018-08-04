package me.islandscout.hawk.checks;

import me.islandscout.hawk.events.InteractEntityEvent;

import java.util.List;

public abstract class AsyncEntityInteractionCheck extends AsyncCheck<InteractEntityEvent> {

    public AsyncEntityInteractionCheck(String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelByDefault, flagByDefault, vlPassMultiplier, minVlFlag, flagCooldown, flag, punishCommands);
    }

    public AsyncEntityInteractionCheck(String name, String flag) {
        super(name, true, true, true, 0.9, 5, 1000, flag, null);
    }
}
