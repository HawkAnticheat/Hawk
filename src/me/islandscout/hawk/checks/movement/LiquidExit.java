package me.islandscout.hawk.checks.movement;

import javafx.util.Pair;
import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.checks.MovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import me.islandscout.hawk.utils.PhysicsUtils;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stops water walk NCP bypass.
 */
public class LiquidExit extends MovementCheck implements Listener {

    private final Map<UUID, Pair<Double, Long>> kbTime; //[Pair] K: vertical velocity, V: timestamp

    public LiquidExit() {
        super("liquidexit", true, 0, 3, 0.99, 5000, "%player% failed liquid exit. VL: %vl%", null);
        kbTime = new HashMap<>();
    }

    @Override
    protected void check(PositionEvent e) {
        Player p = e.getPlayer();
        if (p.isFlying() || p.isInsideVehicle())
            return;

        Location from = e.getFrom();
        double deltaY = e.getTo().getY() - from.getY();

        Block atFrom = ServerUtils.getBlockAsync(from);
        Block belowFrom = ServerUtils.getBlockAsync(from.clone().add(0, deltaY, 0));
        if (atFrom == null || belowFrom == null)
            return;

        //emerged upwards from liquid
        if (deltaY > 0 && atFrom.isLiquid() && !belowFrom.isLiquid() && !AdjacentBlocks.blockNearbyIsSolid(from)) {
            Pair<Double, Long> kb = kbTime.getOrDefault(p.getUniqueId(), new Pair<>(0D, 0L));
            double ticksSinceKb = System.currentTimeMillis() - kb.getValue();
            ticksSinceKb /= 50;
            //check if they're being knocked out of the water
            if (PhysicsUtils.waterYVelFunc(kb.getKey(), ticksSinceKb) < 0) {
                punishAndTryRubberband(e.getHawkPlayer(), e, p.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent e) {
        Vector vector = null;
        if (Hawk.getServerVersion() == 7) {
            vector = e.getVelocity();
        } else if (Hawk.getServerVersion() == 8) {
            //lmao Bukkit is broken. event velocity is broken when attacked by a player (NMS.EntityHuman.java, attack(Entity))
            vector = e.getPlayer().getVelocity();
        }
        if (vector == null)
            return;
        kbTime.put(e.getPlayer().getUniqueId(), new Pair<>(vector.getY(), System.currentTimeMillis() + ServerUtils.getPing(e.getPlayer())));
    }

    @Override
    public void removeData(Player p) {
        kbTime.remove(p.getUniqueId());
    }
}
