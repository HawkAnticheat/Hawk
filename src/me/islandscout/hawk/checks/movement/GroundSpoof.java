package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.checks.Cancelless;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import org.bukkit.Location;

public class GroundSpoof extends AsyncMovementCheck implements Cancelless {

    public GroundSpoof() {
        super("groundspoof", true, false, true, 0.995, 3, 2000, "&7%player% failed ground spoof. VL: %vl%", null);
    }

    @Override
    protected void check(PositionEvent event) {
        if(!event.isOnGroundReally()) {
            if(event.isOnGround()) {

                //minecraft is really getting on my nerves.
                Location checkLoc = event.getFrom().clone();
                checkLoc.setY(event.getTo().getY());
                if(!AdjacentBlocks.onGroundReally(checkLoc, -1)) {
                    punish(event.getPlayer());
                }

            } else {
                reward(event.getPlayer());
            }
        }
    }
}
