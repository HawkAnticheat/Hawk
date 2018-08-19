package me.islandscout.hawk.checks;

import me.islandscout.hawk.events.Event;

import java.util.List;

public abstract class AsyncCustomCheck extends AsyncCheck<Event>{

    public AsyncCustomCheck(String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelByDefault, flagByDefault, vlPassMultiplier, minVlFlag, flagCooldown, flag, punishCommands);
    }

    public AsyncCustomCheck(String name, String flag) {
        super(name, true, true, true, 0.9, 5, 1000, flag, null);
    }
}
