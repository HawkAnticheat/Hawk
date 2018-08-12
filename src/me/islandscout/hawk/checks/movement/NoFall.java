package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class NoFall extends AsyncMovementCheck {

    //TODO: Do NOT damage the player. Just change the onGround flag to false. And actually, I'd remove this check and let GroundSpoof do that.

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

                //TODO: Still false flagging...
                //wait one little second: minecraft is being a pain in the ass and it wants to play tricks when you parkour on the very edge of blocks
                //we need to check this first...
                Location checkLoc = event.getFrom().clone();
                checkLoc.setY(event.getTo().getY());
                if(!AdjacentBlocks.onGroundReally(checkLoc, -1)) {
                    failed.add(p.getUniqueId());
                }

            }
        }
        else {
            maxHeight.put(p.getUniqueId(), 0D);
            double fallDistance = maxY - event.getTo().getY();
            if(fallDistance > 3) {
                if(failed.contains(p.getUniqueId())) {
                    punish(p);
                    if(canCancel()) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, () -> p.damage(fallDistance - 3), 0L);
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
