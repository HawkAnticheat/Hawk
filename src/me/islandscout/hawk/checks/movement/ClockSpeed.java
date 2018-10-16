package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.MovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.ConfigHelper;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class ClockSpeed extends MovementCheck {

    //TODO: you might actually want to listen to flying packets in general, since position events will not go through during teleport
    //TODO: Cancel eating/shooting/regen if this fails

    private final Map<UUID, Long> prevNanoTime;
    private final Map<UUID, Long> clockDrift;
    private final Set<UUID> penalize;
    private final boolean DEBUG;
    private final double THRESHOLD;
    private final long MAX_CATCHUP_TIME;
    private final double CALIBRATE_SLOWER;
    private final double CALIBRATE_FASTER;

    public ClockSpeed() {
        super("clockspeed", true, 5, 10, 0.995, 10000, "%player% failed clockspeed. VL: %vl%, ping: %ping%, TPS: %tps%", null);
        prevNanoTime = new HashMap<>();
        penalize = new HashSet<>();
        clockDrift = new HashMap<>();
        THRESHOLD = -ConfigHelper.getOrSetDefault(10, hawk.getConfig(), "checks.clockspeed.threshold");
        MAX_CATCHUP_TIME = 1000000 * ConfigHelper.getOrSetDefault(500, hawk.getConfig(), "checks.clockspeed.maxCatchupTime");
        DEBUG = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.clockspeed.debug");
        CALIBRATE_SLOWER = 1 - ConfigHelper.getOrSetDefault(0.003, hawk.getConfig(), "checks.clockspeed.calibrateSlower");
        CALIBRATE_FASTER = 1 - ConfigHelper.getOrSetDefault(0.03, hawk.getConfig(), "checks.clockspeed.calibrateFaster");
    }

    @Override
    protected void check(PositionEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        if (event.hasTeleported())
            return;
        long time = System.nanoTime();
        if (!prevNanoTime.containsKey(p.getUniqueId())) {
            prevNanoTime.put(p.getUniqueId(), time);
            return;
        }
        time -= prevNanoTime.get(p.getUniqueId());
        prevNanoTime.put(p.getUniqueId(), System.nanoTime());

        long drift = clockDrift.getOrDefault(p.getUniqueId(), 0L);
        drift += time - 50000000L;
        if (drift > MAX_CATCHUP_TIME)
            drift = MAX_CATCHUP_TIME;
        if (DEBUG) {
            double msOffset = drift * 1E-6;
            p.sendMessage((msOffset < 0 ? (msOffset < THRESHOLD ? ChatColor.RED : ChatColor.YELLOW) : ChatColor.BLUE) + "CLOCK DRIFT: " + -msOffset + "ms");
        }
        if (drift * 1E-6 < THRESHOLD) {
            punishAndTryRubberband(pp, event, p.getLocation());
        } else
            reward(pp);
        if (drift < 0)
            drift *= CALIBRATE_FASTER;
        else
            drift *= CALIBRATE_SLOWER;
        clockDrift.put(p.getUniqueId(), drift);
    }

    @Override
    public void removeData(Player p) {
        prevNanoTime.remove(p.getUniqueId());
        clockDrift.remove(p.getUniqueId());
        penalize.remove(p.getUniqueId());
    }
}
