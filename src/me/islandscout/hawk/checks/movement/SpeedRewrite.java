package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.Debug;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeedRewrite extends AsyncMovementCheck {

    private Map<UUID, Double> threshold;
    private final double GROUND_DEFAULT;
    private final double AIR_DEFAULT;
    private final double FLY_DEFAULT;

    public SpeedRewrite() {
        super("speednew", "&7%player% failed ground speed");
        threshold = new HashMap<>();
        GROUND_DEFAULT = Math.pow(0.28635, 2);
        AIR_DEFAULT = Math.pow(0.2888889, 2);
        FLY_DEFAULT = 1;
    }

    int i = 0;
    boolean ready = false;
    @Override
    protected void check(PositionEvent event) {
        if(!event.hasDeltaPos())
            return;
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        double speedSquared = Math.pow(event.getTo().getX() - event.getFrom().getX(), 2) + Math.pow(event.getTo().getZ() - event.getFrom().getZ(), 2);

        if(speedSquared != 0)
            ready = true;
        if(ready) {
            //Debug.broadcastMessage("(" + i + ", " + Math.sqrt(speedSquared) + ")");
            i++;
        }

        if(speedSquared > GROUND_DEFAULT) {
            //punishAndTryRubberband(pp, event, p.getLocation());
        }
    }
}
