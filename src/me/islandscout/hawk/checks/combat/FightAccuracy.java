package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.CustomCheck;
import me.islandscout.hawk.checks.Cancelless;
import me.islandscout.hawk.events.*;
import me.islandscout.hawk.utils.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FightAccuracy extends CustomCheck implements Listener, Cancelless {

    private final Map<UUID, Map<UUID, FightData>> accuracy;
    private final Map<UUID, Player> lastAttacked;
    private final Map<UUID, Vector> attackerToVictim;
    private final Map<UUID, Long> swingTick; //The swing used to compare with corresponding hit. Special; do not replace!
    private final Map<UUID, Double> checkEligibility; //Used to determine whether a player is active enough to check. Range: 0.0-1.0
    private static final double ELIGIBILITY_THRESHOLD = 0.7;
    private static final double ACCURACY_THRESHOLD = 0.8;
    private static final double SWINGS_UNTIL_CHECK = 20;

    public FightAccuracy() {
        super("fightaccuracy", "%player% may be using killaura (ACCURACY). Accuracy: %accuracy%, VL: %vl%");
        accuracy = new HashMap<>();
        lastAttacked = new HashMap<>();
        attackerToVictim = new HashMap<>();
        swingTick = new HashMap<>();
        checkEligibility = new HashMap<>();
    }

    public void check(Event e) {
        if (e instanceof InteractEntityEvent) {
            hitProcessor((InteractEntityEvent) e);
        } else if (e instanceof ArmSwingEvent) {
            swingProcessor((ArmSwingEvent) e);
        } else if (e instanceof PositionEvent) {
            moveProcessor((PositionEvent) e);
        }
    }

    private void hitProcessor(InteractEntityEvent e) {
        if (e.getInteractAction() == InteractAction.INTERACT || !(e.getEntity() instanceof Player))
            return;
        UUID victim = e.getEntity().getUniqueId();
        UUID uuid = e.getPlayer().getUniqueId();

        HawkPlayer att = e.getHawkPlayer();
        Player pVictim = Bukkit.getPlayer(victim);

        //This hit event must be with its corresponding swing
        if (att.getCurrentTick() != swingTick.getOrDefault(uuid, 0L) || pVictim == null)
            return;

        Map<UUID, FightData> accuracyToVictim = accuracy.getOrDefault(uuid, new HashMap<>());
        FightData fightData = accuracyToVictim.getOrDefault(victim, new FightData());
        if (fightData.swings == 0)
            return;
        fightData.hits++;

        //now it's time to check accuracy
        if (fightData.swings >= SWINGS_UNTIL_CHECK) {
            if(fightData.getRatio() > ACCURACY_THRESHOLD && checkEligibility.getOrDefault(uuid, 0D) > ELIGIBILITY_THRESHOLD) {
                punish(att, false, e, new Placeholder("accuracy", fightData.getRatio() * 100 + "%"));
            }
            //Debug.sendToPlayer(e.getPlayer(), "aim: " + fightData.getRatio());
            fightData.normalize();
        }

        accuracyToVictim.put(victim, fightData);
        accuracy.put(uuid, accuracyToVictim);
    }

    private void swingProcessor(ArmSwingEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (!lastAttacked.containsKey(uuid))
            return;

        HawkPlayer att = e.getHawkPlayer();
        Player victim = lastAttacked.get(uuid);
        if (victim == null)
            return;
        long lastSwingTick = swingTick.getOrDefault(uuid, 0L);

        //proceed if victim's invulnerability is gone
        //diff between current client tick and last swing tick should never be negative
        //a bypass for this IS possible, but you'd get caught by clockspeed if you try to change your tickrate
        if (att.getCurrentTick() - lastSwingTick >= victim.getMaximumNoDamageTicks() / 2) {
            if (att.getLocation().distanceSquared(victim.getLocation()) > 9)
                return;

            Map<UUID, FightData> accuracyToVictim = accuracy.getOrDefault(uuid, new HashMap<>());
            FightData fightData = accuracyToVictim.getOrDefault(victim.getUniqueId(), new FightData());
            fightData.swings++;
            accuracyToVictim.put(victim.getUniqueId(), fightData);

            accuracy.put(uuid, accuracyToVictim);
            swingTick.put(uuid, att.getCurrentTick());
        }
    }

    private void moveProcessor(PositionEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if(lastAttacked.get(uuid) == null) {
            return;
        }
        Player vic = lastAttacked.get(uuid);
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Vector curr = vic.getLocation().toVector().subtract(pp.getLocation().toVector()).setY(0);
        Vector last = attackerToVictim.getOrDefault(uuid, curr);
        double requiredPrecision = Math.tan(0.5 * curr.angle(last)) * curr.length();
        double eligibility = checkEligibility.getOrDefault(uuid, 0D);
        if(!Double.isNaN(requiredPrecision) && requiredPrecision >= 0.075) {
            //increase eligibility
            checkEligibility.put(uuid, Math.min(eligibility + 0.02, 1));
        } else {
            //decrease eligibility
            checkEligibility.put(uuid, Math.max(eligibility - 0.02, 0));
        }
        attackerToVictim.put(p.getUniqueId(), curr);
    }


    @EventHandler
    public void damageDealt(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player && e.getEntity() instanceof Player))
            return;
        Player attacker = (Player) e.getDamager();
        Player victim = (Player) e.getEntity();
        lastAttacked.put(attacker.getUniqueId(), victim);
    }

    private class FightData {

        private float hits;
        private float swings;
        private long lastHitTick;

        private FightData() {
            hits = 0;
            swings = 0;
        }

        private float getRatio() {
            return swings == 0 ? Float.NaN : (hits / swings);
        }

        private void normalize() {
            hits /= swings;
            swings = 1;
        }

    }

    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        accuracy.remove(uuid);
        attackerToVictim.remove(uuid);
        lastAttacked.remove(uuid);
        swingTick.remove(uuid);
        checkEligibility.remove(uuid);
    }
}
