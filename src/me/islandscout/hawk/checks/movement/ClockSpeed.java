package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.ConfigHelper;
import me.islandscout.hawk.utils.Debug;
import org.bukkit.entity.Player;

import java.util.*;

public class ClockSpeed extends AsyncMovementCheck {

    //TODO: Cancel eating/shooting/regen if this fails

    private Map<UUID, Long> prevNanoTime;
    private Map<UUID, Long> clockDrift;
    private Set<UUID> penalize;
    private boolean DEBUG;
    private double THRESHOLD;
    private long MAX_CATCHUP_TIME;

    public ClockSpeed() {
        super("clockspeed", true, true, true, 0.995, 3, 2000, "&7%player% failed clockspeed. VL: %vl%, ping: %ping%, TPS: %tps%", null);
        prevNanoTime = new HashMap<>();
        penalize = new HashSet<>();
        clockDrift = new HashMap<>();
        THRESHOLD = ConfigHelper.getOrSetDefault(10, hawk.getConfig(), "checks.clockspeed.threshold");
        MAX_CATCHUP_TIME = 1000000 * ConfigHelper.getOrSetDefault(500, hawk.getConfig(), "checks.clockspeed.maxCatchupTime");
        DEBUG = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.clockspeed.debug");
    }

    @Override
    protected void check(PositionEvent event) {
        Player p = event.getPlayer();
        if(event.hasTeleported())
            return;
        long time = System.nanoTime();
        if(!prevNanoTime.containsKey(p.getUniqueId())) {
            prevNanoTime.put(p.getUniqueId(), time);
            return;
        }
        time -= prevNanoTime.get(p.getUniqueId());
        prevNanoTime.put(p.getUniqueId(), System.nanoTime());

        long drift = clockDrift.getOrDefault(p.getUniqueId(), 0L);
        drift += time - 50000000L;
        if(drift > MAX_CATCHUP_TIME)
            drift = MAX_CATCHUP_TIME;
        if(DEBUG)
            Debug.sendToPlayer(p, "CLOCK DRIFT: " + drift * 1E-6 + "ms");
        if(drift * 1E-6 < -THRESHOLD) {
            punishAndTryRubberband(p, event, p.getLocation());
        }
        else
            reward(p);
        if(drift < 0)
            drift *= 0.97;
        else
            drift *= 0.997;
        clockDrift.put(p.getUniqueId(), drift);
    }

    @Override
    public void removeData(Player p) {
        prevNanoTime.remove(p.getUniqueId());
        clockDrift.remove(p.getUniqueId());
        penalize.remove(p.getUniqueId());
    }
}
