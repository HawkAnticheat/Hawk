package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncEntityInteractionCheck;
import me.islandscout.hawk.checks.Cancelless;
import me.islandscout.hawk.events.InteractAction;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.ConfigHelper;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FightSynchronized exploits a flaw in aim-bot/aim-assist cheats
 * by comparing the player's last attack time to their last move
 * time. Although easily bypassed, it catches a significant
 * number of cheaters.
 */
public class FightSynchronized extends AsyncEntityInteractionCheck implements Cancelless {

    //TODO: False positives after HUGE lag spikes. This can falsely ban players. Have a VL cooldown for lag catchup?

    private Map<UUID, Long> attackTimes;
    private Map<UUID, Integer> samples;
    private final int SAMPLE_SIZE;
    private final int THRESHOLD;

    public FightSynchronized() {
        super("fightsync", true, false, true, 0.95, 2, 1000, "&7%player% may be using killaura (SYNC). VL %vl%", null);
        attackTimes = new HashMap<>();
        samples = new HashMap<>();
        SAMPLE_SIZE = ConfigHelper.getOrSetDefault(20, hawk.getConfig(), "checks.fightsync.samplesize");
        THRESHOLD = ConfigHelper.getOrSetDefault(6, hawk.getConfig(), "checks.fightsync.threshold");
    }

    @Override
    public void check(InteractEntityEvent event) {
        if(event.getInteractAction() != InteractAction.ATTACK)
            return;
        Player attacker = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        samples.put(attacker.getUniqueId(), samples.getOrDefault(attacker, 0) + 1);
        long diff = System.currentTimeMillis() - pp.getLastMoveTime();
        if (diff > 100) {
            diff = 100L;
        }
        attackTimes.put(attacker.getUniqueId(), attackTimes.getOrDefault(attacker, 0L) + diff);
        if (samples.get(attacker.getUniqueId()) >= SAMPLE_SIZE) {
            samples.put(attacker.getUniqueId(), 0);
            attackTimes.put(attacker.getUniqueId(), attackTimes.get(attacker.getUniqueId()) / SAMPLE_SIZE);
            if (attackTimes.get(attacker.getUniqueId()) < THRESHOLD) {
                punish(pp);
            }
            else
                reward(pp);
        } else {
            samples.put(attacker.getUniqueId(), samples.get(attacker.getUniqueId()) + 1);
        }
    }

    public void removeData(Player p) {
        samples.remove(p.getUniqueId());
        attackTimes.remove(p.getUniqueId());
    }
}
