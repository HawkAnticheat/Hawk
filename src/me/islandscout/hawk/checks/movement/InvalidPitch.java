package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;

//Not really an important check. This just stops skids from thinking they're so cool.
public class InvalidPitch extends AsyncMovementCheck {

    public InvalidPitch() {
        super("invalidpitch", "&7%player% failed invalid pitch. VL: %vl%");
    }

    @Override
    protected void check(PositionEvent event) {
        if(!event.hasDeltaRot())
            return;
        if(event.getTo().getPitch() < -90 || event.getTo().getPitch() > 90)
            punishAndTryRubberband(event.getHawkPlayer(), event, event.getPlayer().getLocation());
        else
            reward(event.getHawkPlayer());
    }
}
