package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Stops water walk NCP bypass.
 */
public class LiquidExit extends AsyncMovementCheck {

    //TODO: Support velocity/damage kb

    public LiquidExit() {
        super("liquidexit", true, true, true, 0.99, 3, 2000, "&7%player% failed liquid exit. VL: %vl%", null);
    }

    @Override
    protected void check(PositionEvent e) {
        Player p = e.getPlayer();
        if(p.isFlying() || p.isInsideVehicle())
            return;

        Location from = e.getFrom();
        double deltaY = e.getTo().getY() - from.getY();

        //emerged upwards from liquid
        if(deltaY > 0 && from.getBlock().isLiquid() && !from.clone().add(0, deltaY, 0).getBlock().isLiquid()) {
            if(!AdjacentBlocks.blockNearbyIsSolid(from)) {
                punishAndTryRubberband(p, e, p.getLocation());
            }
        }
    }
}
