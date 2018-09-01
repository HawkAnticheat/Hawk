package me.islandscout.hawk.checks;

import me.islandscout.hawk.events.Event;

import java.util.List;

public abstract class AsyncCustomCheck extends AsyncCheck<Event>{

    public AsyncCustomCheck(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelThreshold, flagThreshold, vlPassMultiplier, flagCooldown, flag, punishCommands);
    }

    public AsyncCustomCheck(String name, String flag) {
        super(name, true, 0, 5, 0.9, 1000, flag, null);
    }
}
