package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stops water walk NCP bypass.
 */
public class LiquidExit extends AsyncMovementCheck implements Listener {

    private Map<UUID, DoubleTime> kbTime;

    //TODO: Support velocity/damage kb
    //There must be a faster way to compute the integral of the velocity function

    public LiquidExit() {
        super("liquidexit", true, 0, 3, 0.99, 2000, "&7%player% failed liquid exit. VL: %vl%", null);
        kbTime = new HashMap<>();
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
                punishAndTryRubberband(e.getHawkPlayer(), e, p.getLocation());
            }
        }
    }

    private class DoubleTime {

        private double value;
        private long time;

        private DoubleTime(double value, long time) {
            this.value = value;
            this.time = time;
        }
    }
}
