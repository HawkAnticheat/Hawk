package me.islandscout.hawk.checks;

import me.islandscout.hawk.events.InteractEntityEvent;

import java.util.List;

public abstract class EntityInteractionCheck extends Check<InteractEntityEvent> {

    protected EntityInteractionCheck(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelThreshold, flagThreshold, vlPassMultiplier, flagCooldown, flag, punishCommands);
        hawk.getCheckManager().getEntityInteractionChecks().add(this);
    }

    protected EntityInteractionCheck(String name, String flag) {
        this(name, true, 0, 5, 0.9, 5000, flag, null);
    }
}
