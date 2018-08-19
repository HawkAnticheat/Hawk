package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Stops water walk NCP bypass.
 */
public class LiquidExit extends AsyncMovementCheck {

    //TODO: Support velocity/damage kb
    //There must be a faster way to compute the integral of the velocity function

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

        Block atFrom = ServerUtils.getBlockAsync(from);
        Block belowFrom = ServerUtils.getBlockAsync(from.clone().add(0, deltaY, 0));
        if(atFrom == null || belowFrom == null)
            return;

        //emerged upwards from liquid
        if(deltaY > 0 && atFrom.isLiquid() && !belowFrom.isLiquid()) {
            if(!AdjacentBlocks.blockNearbyIsSolid(from)) {
                punishAndTryRubberband(p, e, p.getLocation());
            }
        }
    }
}
