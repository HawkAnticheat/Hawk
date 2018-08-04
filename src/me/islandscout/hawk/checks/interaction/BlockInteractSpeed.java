package me.islandscout.hawk.checks.interaction;

import me.islandscout.hawk.checks.AsyncBlockPlacementCheck;
import me.islandscout.hawk.events.BlockPlaceEvent;
import me.islandscout.hawk.utils.Placeholder;
import org.bukkit.entity.Player;

import java.util.*;

public class BlockInteractSpeed extends AsyncBlockPlacementCheck {

    private Map<UUID, Long> prevTime;
    private Map<UUID, List<Long>> deltaTimes;
    private Map<UUID, Float> hardFails;
    private final int AVG_THRESHOLD;
    private final int SAMPLE_SIZE;
    private final double HARD_THRESHOLD;

    public BlockInteractSpeed() {
        super("blockplacespeed", "&7%player% failed block place speed. VL: %vl%");
        prevTime = new HashMap<>();
        deltaTimes = new HashMap<>();
        hardFails = new HashMap<>();
        AVG_THRESHOLD = 15;
        SAMPLE_SIZE = 10;
        HARD_THRESHOLD = 1D/20*1000; //20
    }

    @Override
    protected void check(BlockPlaceEvent e) {
        boolean passed = true;
        Player p = e.getPlayer();
        long deltaTime = System.currentTimeMillis() - prevTime.getOrDefault(p.getUniqueId(), 0L);

        if(deltaTime < HARD_THRESHOLD) {
            double bps = 1/deltaTime*1000;
            hardFails.put(p.getUniqueId(), hardFails.getOrDefault(p.getUniqueId(), 0F) + 1);
            if(hardFails.get(p.getUniqueId()) > 5) {
                punishAndTryCancelAndBlockDestroy(p, e, new Placeholder("rate", bps + "bps"));
                passed = false;
            }
        }

        List<Long> deltaTimess = deltaTimes.getOrDefault(p.getUniqueId(), new ArrayList<>());
        deltaTimess.add(deltaTime);

        if(deltaTimess.size() >= SAMPLE_SIZE) {
            long avg = 0;
            for(Long loopDeltaTime : deltaTimess) {
                avg += loopDeltaTime;
            }
            avg /= SAMPLE_SIZE;
            deltaTimes.remove(p.getUniqueId());
            double bps = avg == 0 ? Double.POSITIVE_INFINITY : 1 / (avg / 1000D);
            if(bps > AVG_THRESHOLD) {
                punish(p, new Placeholder("rate", bps + "bps"));
                passed = false;
            }
        }
        else {
            deltaTimes.put(p.getUniqueId(), deltaTimess);
        }

        if(passed) {
            reward(p);
            hardFails.put(p.getUniqueId(), hardFails.getOrDefault(p.getUniqueId(), 0F) * 0.95F);
        }

        prevTime.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public void removeData(Player p) {
        prevTime.remove(p.getUniqueId());
        deltaTimes.remove(p.getUniqueId());
        hardFails.remove(p.getUniqueId());
    }
}
