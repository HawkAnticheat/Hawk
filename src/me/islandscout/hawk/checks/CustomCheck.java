package me.islandscout.hawk.checks;

import me.islandscout.hawk.events.Event;

import java.util.List;

public abstract class CustomCheck extends Check<Event> {

    protected CustomCheck(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelThreshold, flagThreshold, vlPassMultiplier, flagCooldown, flag, punishCommands);
        hawk.getCheckManager().getCustomChecks().add(this);
    }

    protected CustomCheck(String name, String flag) {
        this(name, true, 0, 5, 0.9, 5000, flag, null);
    }
}
