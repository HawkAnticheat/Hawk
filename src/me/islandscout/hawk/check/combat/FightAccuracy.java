/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.event.*;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The FightAccuracy check is designed to flag players whose aim accuracies are
 * improbably high. Because of the design choices for this check, there are
 * multiple known bypasses, and because of this, this check depends on other
 * checks.
 */
public class FightAccuracy extends CustomCheck implements Listener, Cancelless {

    private final Map<UUID, Map<UUID, FightData>> accuracy;
    private final Map<UUID, HawkPlayer> lastAttacked;
    private final Map<UUID, Long> swingTick; //The swing used to compare with corresponding hit. Special; do not replace!
    private final Map<UUID, Double> activity; //Used to determine whether a player is active enough to check. Range: 0.0-1.0
    private final double ACTIVITY_THRESHOLD;
    private final double ACCURACY_THRESHOLD;
    private final double SWINGS_UNTIL_CHECK;
    private final double MIN_PRECISION_THRESHOLD;
    private final boolean DEBUG;

    public FightAccuracy() {
        super("fightaccuracy", true, -1, 0, 0.99, 5000, "%player% may be using killaura (ACCURACY). Accuracy: %accuracy%, VL: %vl%", null);
        accuracy = new HashMap<>();
        lastAttacked = new HashMap<>();
        swingTick = new HashMap<>();
        activity = new HashMap<>();
        ACTIVITY_THRESHOLD = (double)customSetting("activityThreshold", "", 0.7D);
        ACCURACY_THRESHOLD = (double)customSetting("accuracyThreshold", "", 0.9D);
        SWINGS_UNTIL_CHECK = (int)customSetting("swingsUntilCheck", "", 20);
        MIN_PRECISION_THRESHOLD = (double)customSetting("minPrecisionThreshold", "", 0.3D);
        DEBUG = (boolean)customSetting("debug", "", false);
    }

    public void check(Event e) {
        if (e instanceof InteractEntityEvent) {
            hitProcessor((InteractEntityEvent) e);
        } else if (e instanceof ArmSwingEvent) {
            swingProcessor((ArmSwingEvent) e);
        }
    }

    private void hitProcessor(InteractEntityEvent e) {
        if (e.getInteractAction() == InteractAction.INTERACT || !(e.getEntity() instanceof Player))
            return;
        UUID victim = e.getEntity().getUniqueId();
        UUID uuid = e.getPlayer().getUniqueId();

        HawkPlayer att = e.getHawkPlayer();
        Player pVictim = Bukkit.getPlayer(victim);

        //This hit event must be with its corresponding swing.
        //You can bypass this by using noswing, but you'll get flagged by FightNoSwing for using that.
        if (att.getCurrentTick() != swingTick.getOrDefault(uuid, 0L) || pVictim == null)
            return;

        Map<UUID, FightData> accuracyToVictim = accuracy.getOrDefault(uuid, new HashMap<>());
        FightData fightData = accuracyToVictim.getOrDefault(victim, new FightData());
        if (fightData.swings == 0)
            return;
        fightData.hits++;

        //Now it's time to check accuracy.
        //Anyone having an accuracy over 100% should re-evaluate what they're doing with their life.
        //Technically this is a bug, but I'll keep it for the sake of ratting out garbage-juice skids.
        if (fightData.swings >= SWINGS_UNTIL_CHECK) {
            if(DEBUG) {
                att.getPlayer().sendMessage("Checking aim...");
                att.getPlayer().sendMessage("Activity: " + activity.getOrDefault(uuid, 0D));
                att.getPlayer().sendMessage("Activity threshold: " + ACTIVITY_THRESHOLD);
                att.getPlayer().sendMessage("Aim accuracy: " + fightData.getRatio() * 100 + "%");
                att.getPlayer().sendMessage("Aim accuracy threshold: " + ACCURACY_THRESHOLD * 100 + "%");
            }
            if(fightData.getRatio() > ACCURACY_THRESHOLD && activity.getOrDefault(uuid, 0D) >= ACTIVITY_THRESHOLD) {
                if(DEBUG) {
                    att.getPlayer().sendMessage(ChatColor.RED + "FAIL");
                }
                punish(att, false, e, new Placeholder("accuracy", MathPlus.round(fightData.getRatio() * 100, 2) + "%"));
            } else if (DEBUG) {
                att.getPlayer().sendMessage(ChatColor.GREEN + "PASS");
            }
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
        HawkPlayer victim = lastAttacked.get(uuid);
        if (victim == null)
            return;
        long lastSwingTick = swingTick.getOrDefault(uuid, 0L);

        //proceed if victim's invulnerability is gone
        //diff between current client tick and last swing tick should never be negative
        //a bypass for this IS possible, but you'd get caught by clockspeed if you try to change your tickrate
        if (att.getCurrentTick() - lastSwingTick >= victim.getPlayer().getMaximumNoDamageTicks() / 2) {

            Map<UUID, FightData> accuracyToVictim = accuracy.getOrDefault(uuid, new HashMap<>());
            FightData fightData = accuracyToVictim.getOrDefault(victim.getUuid(), new FightData());
            fightData.swings++;
            accuracyToVictim.put(victim.getUuid(), fightData);

            accuracy.put(uuid, accuracyToVictim);
            swingTick.put(uuid, att.getCurrentTick());
        }

        //determine how far the opponent has moved horizontally on local coordinates and compute required mouse precision
        if(!att.getPlayer().getWorld().equals(victim.getPlayer().getWorld()))
            return;
        Vector victimVelocity = victim.getVelocity().clone().setY(0);
        Vector attackerDirection = att.getPlayer().getLocation().getDirection().clone().setY(0);
        double localMovement = Math.sin(victimVelocity.angle(attackerDirection)) * victimVelocity.length();
        if(Double.isNaN(localMovement))
            localMovement = 0D;
        double requiredPrecision = localMovement * att.getLocation().distance(victim.getLocation());
        double activity = this.activity.getOrDefault(uuid, 0D);

        if(DEBUG) {
            if(requiredPrecision >= MIN_PRECISION_THRESHOLD && activity < ACTIVITY_THRESHOLD && activity + 0.02 >= ACTIVITY_THRESHOLD) {
                att.getPlayer().sendMessage(ChatColor.GREEN + "You are now eligible to be checked by fightaccuracy because your opponent is moving significantly.");
            }
            else if(requiredPrecision < MIN_PRECISION_THRESHOLD && activity >= ACTIVITY_THRESHOLD && activity - 0.01 < ACTIVITY_THRESHOLD) {
                att.getPlayer().sendMessage(ChatColor.RED + "You are no longer eligible to be checked by fightaccuracy because your opponent is not moving enough.");
            }
        }

        if (requiredPrecision >= MIN_PRECISION_THRESHOLD) {
            //increase activity
            this.activity.put(uuid, Math.min(activity + 0.02, 1));
        } else {
            //decrease activity
            this.activity.put(uuid, Math.max(activity - 0.01, 0));
        }
    }

    @EventHandler
    public void damageDealt(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player && e.getEntity() instanceof Player))
            return;
        Player attacker = (Player) e.getDamager();
        Player victim = (Player) e.getEntity();
        lastAttacked.put(attacker.getUniqueId(), hawk.getHawkPlayer(victim));
    }

    private class FightData {

        private float hits;
        private float swings;

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
        lastAttacked.remove(uuid);
        swingTick.remove(uuid);
        activity.remove(uuid);
    }
}
