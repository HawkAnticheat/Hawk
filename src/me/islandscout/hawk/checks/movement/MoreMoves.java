package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.ConfigHelper;
import me.islandscout.hawk.utils.Debug;
import me.islandscout.hawk.utils.MathPlus;
import me.islandscout.hawk.utils.Placeholder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;

public class MoreMoves extends AsyncMovementCheck implements Listener {

    //TODO: Cancel eating/shooting/regen if this fails

    private Map<UUID, Long> prevNanoTime;
    private Map<UUID, List<Long>> deltaTimes;
    private Map<UUID, Location> legitLoc;
    private Set<UUID> penalize;
    private final int SAMPLE_SIZE;
    private static final int CANCEL_BY_VL = 4;
    private final double THRESHOLD;

    public MoreMoves() {
        super("moremoves", true, true, true, 0.8, 3, 1000, "&7%player% is sending too many moves. Factor: %factor%x, VL: %vl%, ping: %ping%, TPS: %tps%", null);
        prevNanoTime = new HashMap<>();
        deltaTimes = new HashMap<>();
        legitLoc = new HashMap<>();
        penalize = new HashSet<>();
        THRESHOLD = ConfigHelper.getOrSetDefault(1.01, hawk.getConfig(), "checks.moremoves.threshold");
        SAMPLE_SIZE = ConfigHelper.getOrSetDefault(30, hawk.getConfig(), "checks.moremoves.sampleSize");
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
        List<Long> times = deltaTimes.getOrDefault(p.getUniqueId(), new ArrayList<>());
        if(times.size() == 0) {
            legitLoc.put(p.getUniqueId(), event.getFrom());
        }
        times.add(time);
        if(times.size() >= SAMPLE_SIZE) {
            long avg = 0;
            for(Long deltaTime : times) {
                avg += deltaTime;
            }
            deltaTimes.remove(p.getUniqueId());
            avg /= SAMPLE_SIZE;
            double factor = avg == 0 ? Double.POSITIVE_INFINITY : (1 / (avg / 50000000D));
            if(factor >= THRESHOLD) {
                HawkPlayer pp = hawk.getHawkPlayer(p);
                punish(p, new Placeholder("factor", MathPlus.round(factor, 2)));
                if(pp.getVL(this) >= CANCEL_BY_VL) {
                    tryRubberband(event, legitLoc.get(p.getUniqueId()));
                }
                penalize.add(p.getUniqueId());
            }
            else {
                reward(p);
                penalize.remove(p.getUniqueId());
            }
        }
        else {
            deltaTimes.put(p.getUniqueId(), times);
        }
    }

    @Override
    public void removeData(Player p) {
        prevNanoTime.remove(p.getUniqueId());
        deltaTimes.remove(p.getUniqueId());
        legitLoc.remove(p.getUniqueId());
        penalize.remove(p.getUniqueId());
    }
}
