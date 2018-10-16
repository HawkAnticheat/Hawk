package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.MovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Inertia extends MovementCheck {

    //"Inertia is a property of matter... Bill, Bill, Bill..."

    //TODO: Handle damage knockback & applied velocity?

    private final Map<UUID, Vector> vec;

    public Inertia() {
        super("inertia", true, -1, 3, 0.995, 5000, "%player% failed inertia. VL: %vl%", null);
        vec = new HashMap<>();
    }

    @Override
    public void check(PositionEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Vector moveVector = new Vector(e.getTo().getX() - e.getFrom().getX(), 0, e.getTo().getZ() - e.getFrom().getZ());
        double horizSpeedSquared = Math.pow(e.getTo().getX() - e.getFrom().getX(), 2) + Math.pow(e.getTo().getZ() - e.getFrom().getZ(), 2);
        if (horizSpeedSquared > 0.05) {
            double deltaAngle = moveVector.angle(vec.getOrDefault(p.getUniqueId(), new Vector(0, 0, 0)));
            if (!AdjacentBlocks.blockNearbyIsSolid(e.getTo()) && !AdjacentBlocks.blockAdjacentIsSolid(e.getFrom().clone().add(0, -0.3, 0)) &&
                    !AdjacentBlocks.blockNearbyIsSolid(e.getTo().clone().add(0, 1, 0)) && !p.isFlying() && !p.isInsideVehicle()) {
                if (vec.containsKey(p.getUniqueId()) && deltaAngle > 0.2) {

                    punishAndTryRubberband(pp, e, e.getFrom());
                } else {
                    reward(pp);
                }
            }
        }
        vec.put(p.getUniqueId(), moveVector);
    }

    public void removeData(Player p) {
        vec.remove(p.getUniqueId());
    }
}
