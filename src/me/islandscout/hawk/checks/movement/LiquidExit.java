package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Stops water walk NCP bypass.
 */
public class LiquidExit extends AsyncMovementCheck {

    public LiquidExit(Hawk hawk) {
        super(hawk, "liquidexit", true, true, true, 0.99, 3, 2000, "&7%player% failed liquid exit.", null);
    }

    @Override
    protected void check(PositionEvent e) {
        Location from = e.getFrom();
        double deltaY = e.getTo().getY() - from.getY();

        //emerged vertically from liquid
        if(from.getBlock().isLiquid() && !from.clone().add(0, deltaY, 0).getBlock().isLiquid()) {
            if(!AdjacentBlocks.blockNearbyIsSolid(from)) {
                Player p = e.getPlayer();
                punishAndTryRubberband(p, e, p.getLocation());
            }
        }
    }
}
