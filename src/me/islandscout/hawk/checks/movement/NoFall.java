package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class NoFall extends AsyncMovementCheck {

    private Map<UUID, Double> maxHeight;
    private Set<UUID> failed;
    private static final double EPSILON = 0.3;

    public NoFall() {
        super("nofall", true, true, true, 0.9, 2, 1000, "&7%player% failed nofall. VL: %vl%", null);
        maxHeight = new HashMap<>();
        failed = new HashSet<>();
    }

    @Override
    protected void check(PositionEvent event) {
        Player p = event.getPlayer();
        double maxY = maxHeight.getOrDefault(p.getUniqueId(), 0D);
        if(!event.isOnGroundReally()) {
            if(event.getTo().getY() > event.getFrom().getY() || event.getTo().getY() > maxY) {
                maxHeight.put(p.getUniqueId(), event.getTo().getY());
            }
            if(event.isOnGround() && maxY - event.getTo().getY() > EPSILON) {

                //TODO: false pos when landing on very edge of block and then immediately slipping. copy code from fly?

                failed.add(p.getUniqueId());
            }
        }
        else {
            maxHeight.put(p.getUniqueId(), 0D);
            double fallDistance = maxY - event.getTo().getY();
            if(fallDistance > 3) {
                if(failed.contains(p.getUniqueId())) {
                    punish(p);
                    if(canCancel()) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, new Runnable() {
                            @Override
                            public void run() {
                                p.damage(fallDistance - 3);
                            }
                        }, 0L);
                    }
                    failed.remove(p.getUniqueId());
                }
                else
                    reward(p);
            }
        }
    }

    @Override
    public void removeData(Player p) {
        maxHeight.remove(p.getUniqueId());
        failed.remove(p.getUniqueId());
    }
}
