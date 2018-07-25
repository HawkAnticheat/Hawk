package me.islandscout.hawk.checks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class AsyncMovementCheck extends AsyncCheck<PositionEvent> {

    //BYPASS WARNING:
    //Move checks must check getTo() locations, and if they rubberband, they MUST NOT rubberband to getTo() locations.
    //Checks implementing their own rubberband locations must set them to Player#getLocation() (but if handling teleportation, use getTo()),
    //since that check may not be the last one in the list. Do not change getFrom() or getTo() locations.
    //Player#getLocation() is recommended for rubberbanding for some checks since Spigot has additional movement checks after Hawk's checks.
    //A chain is as strong as its weakest link.

    public AsyncMovementCheck(Hawk hawk, String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        super(hawk, name, enabled, cancelByDefault, flagByDefault, vlPassMultiplier, minVlFlag, flagCooldown, flag, punishCommands);
    }

    public AsyncMovementCheck(Hawk hawk, String name, String flag) {
        super(hawk, name, true, true, true, 0.9, 5, 1000, flag, null);
    }

    protected void rubberband(PositionEvent event, Location setback) {
        event.cancelAndSetBack(setback, hawk);
    }

    protected void tryRubberband(PositionEvent event, Location setback) {
        if(canCancel())
            rubberband(event, setback);
    }

    protected void punishAndTryRubberband(Player offender, PositionEvent event, Location setback, Placeholder... placeholders) {
        super.punish(offender, placeholders);
        tryRubberband(event, setback);
    }
}
