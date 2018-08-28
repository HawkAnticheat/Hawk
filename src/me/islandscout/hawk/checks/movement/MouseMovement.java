package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.checks.Cancelless;
import me.islandscout.hawk.events.PositionEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FightAimbot exploits a flaw in aim-bot/aim-assist cheats
 * by analyzing mouse movement patterns. Although easily
 * bypassed, it catches a significant number of cheaters.
 */
public class MouseMovement extends AsyncMovementCheck implements Cancelless {

    private Map<UUID, Double> lastLookDistanceSquared;

    public MouseMovement() {
        super("mousemovement", true, false, true, 0.97, 5, 2000, "&7%player% may be using aimbot. VL %vl%", null);
        lastLookDistanceSquared = new HashMap<>();
    }

    public void check(PositionEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        UUID uuid = p.getUniqueId();
        double lookDistanceSquared = pp.getDeltaYaw() * pp.getDeltaYaw() + pp.getDeltaPitch() * pp.getDeltaPitch();

        if(lastLookDistanceSquared.containsKey(uuid)) {
            if(lastLookDistanceSquared.get(uuid) > 8 && lookDistanceSquared < 0.001 && System.currentTimeMillis() - pp.getLastMoveTime() < 60) {
                punish(pp);
            }
            else {
                reward(pp);
            }
        }

        lastLookDistanceSquared.put(uuid, lookDistanceSquared);
    }

    public void removeData(Player p) {
        lastLookDistanceSquared.remove(p.getUniqueId());
    }
}
