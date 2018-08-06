package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.checks.AsyncEntityInteractionCheck;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.MathPlus;
import me.islandscout.hawk.utils.Placeholder;
import org.bukkit.entity.Player;

import java.util.*;

public class FightSpeed extends AsyncEntityInteractionCheck {

    private Map<UUID, Double> lastClickTime; //in seconds
    private Map<UUID, List<Double>> deltaTimes;
    private static final double RECORD_SENSITIVITY = 5;
    private static final int SAMPLES = 10;


    public FightSpeed() {
        super("fightspeed", "&7%player% failed attack speed. CPS: %cps%, VL: %vl%");
        lastClickTime = new HashMap<>();
        deltaTimes = new HashMap<>();
    }

    @Override
    protected void check(InteractEntityEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if(lastClickTime.containsKey(uuid)) {
            List<Double> deltaTs = deltaTimes.getOrDefault(uuid, new ArrayList<>());
            double deltaT = (System.nanoTime() - lastClickTime.get(uuid)) / 1E+9D;
            if(1D / deltaT >= RECORD_SENSITIVITY) {
                deltaTs.add(deltaT);
                if(deltaTs.size() >= SAMPLES) {
                    double avgCps = 0;
                    for(double entry : deltaTs) {
                        avgCps += entry;
                    }
                    avgCps /= SAMPLES;
                    avgCps = 1D/avgCps;
                    if(avgCps > 15) {
                        punish(p, true, e, new Placeholder("cps", MathPlus.round(avgCps, 2) + ""));
                    }
                    else {
                        reward(p);
                    }
                    deltaTs.remove(0);
                }
                deltaTimes.put(uuid, deltaTs);
            }
        }
        lastClickTime.put(uuid, (double)System.nanoTime());
    }
}
