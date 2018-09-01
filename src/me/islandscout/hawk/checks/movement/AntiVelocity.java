package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.checks.Cancelless;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class AntiVelocity extends AsyncMovementCheck implements Listener, Cancelless {

    //TODO: False flag right as player hits the ground and immediately jumps

    private Map<UUID, VectorTime> velocities; //We're talking about launch velocities
    private static double FRICTION_AIR = 0.09;
    private static double FRICTION_WATER = 0.2;
    private static double FRICTION_GROUND = 0.46;
    private static final double EPSILON = 0.035;

    public AntiVelocity() {
        super("antivelocity", true, -1, 5, 0.95, 2000, "&7%player% may be using antivelocity. VL: %vl%", null);
        velocities = new HashMap<>();
        FRICTION_AIR = (1 - FRICTION_AIR) * (1 - EPSILON);
        FRICTION_WATER = 1 - FRICTION_WATER * (1 - EPSILON);
        FRICTION_GROUND = 1 - FRICTION_GROUND * (1 - EPSILON);
    }

    @Override
    protected void check(PositionEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        Vector currVelocity = new Vector(event.getTo().getX() - event.getFrom().getX(), event.getTo().getY() - event.getFrom().getY(), event.getTo().getZ() - event.getFrom().getZ());
        long currTime = System.currentTimeMillis();
        int ping = ServerUtils.getPing(p);
        if(!p.isFlying() && !p.isInsideVehicle() && velocities.containsKey(p.getUniqueId()) && currTime - velocities.get(p.getUniqueId()).time > ping) {

            Vector expectedVelocity = velocities.get(p.getUniqueId()).vector;

            //clear map element if vector is no longer significant
            if(expectedVelocity.lengthSquared() <= 0.05) {
                velocities.remove(p.getUniqueId());
            }
            else {
                Vector diff = currVelocity.clone().subtract(expectedVelocity);
                double dot = diff.dot(expectedVelocity);
                if(dot < -0.07) {
                    punish(pp);
                }
                else {
                    reward(pp);
                }
            }

            //applying vertical friction
            if(!event.isOnGroundReally())
                expectedVelocity.setY((expectedVelocity.getY() - 0.08) * 0.98);
            else {
                expectedVelocity.setY(0);
            }

            //applying horizontal friction
            //on ground
            if(AdjacentBlocks.onGroundReally(event.getFrom(), -1, false)) {
                expectedVelocity.setX(expectedVelocity.getX() * FRICTION_GROUND);
                expectedVelocity.setZ(expectedVelocity.getZ() * FRICTION_GROUND);
            }
            //in air
            else {
                expectedVelocity.setX(expectedVelocity.getX() * FRICTION_AIR);
                expectedVelocity.setZ(expectedVelocity.getZ() * FRICTION_AIR);
            }

            //TODO: If collide with a solid block, clear map element.

        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(PlayerVelocityEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        velocities.put(uuid, new VectorTime(e.getVelocity(), System.currentTimeMillis()));
    }

    private class VectorTime {

        private Vector vector;
        private long time;

        private VectorTime(Vector vector, long time) {
            this.vector = vector;
            this.time = time;
        }
    }

    @Override
    public void removeData(Player p) {
        velocities.remove(p.getUniqueId());
    }
}
