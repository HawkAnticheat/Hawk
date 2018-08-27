package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import me.islandscout.hawk.utils.Debug;
import me.islandscout.hawk.utils.MathPlus;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;


public class Speed extends AsyncMovementCheck implements Listener {

    //This was moved from the old Hawk codebase.
    //I hate having to dig around this horror. But, hey, if this works, I'm leaving this alone.
    //I'm gonna bet that a bypass will pop up. In that case, I'll have to redo this entire thing.

    //TODO: CLEAN THIS HELL HOLE UP
    //TODO: YOU MUST GET BLOCKS USING SERVERUTILS!!!

    private static final int WATER_TREAD_GRACE = 12;
    private static final int WATER_UNDER_GRACE = 16;
    private static final double SPEED_THRES_SOFT = Math.pow(0.36055513, 2);
    private static final double SPEED_THRES_HARD = Math.pow(0.632455532, 2);
    private static final double DAMAGE_SPEED = 1.0; //you must square this value to use it
    private static final int FAIL_BUFFER_1 = 4;
    private static final int FAIL_BUFFER_2 = 9;
    private static final boolean FAIL_BUFFER_RESET = true;

    //launch velocity handling settings
    private static double FRICTION_AIR = 0.09;
    private static double FRICTION_WATER = 0.2;
    private static double FRICTION_GROUND = 0.46;
    private static final double EPSILON = 0.035;

    private Map<UUID, Integer> sprintgracetimer;
    private Map<UUID, Integer> speedygrace;
    private Map<UUID, Integer> speedygracetimer;
    private Map<UUID, Integer> speedbuffer;
    private Map<UUID, Integer> speed1;
    private Map<UUID, Integer> sneakgrace;
    private Map<UUID, Integer> watergrace;
    private Map<UUID, Location> lastLegitLoc;
    private Map<UUID, Long> penalizeTimestamp;
    private Map<UUID, DoubleTime> velocities; //launch velocities
    private Map<UUID, Double> launchVelocity;

    public Speed() {
        super("speed", true, true, true, 0.995, 10, 2000,"&7%player% failed speed. VL: %vl%", null);
        sprintgracetimer = new HashMap<>();
        speedygrace = new HashMap<>();
        speedygracetimer = new HashMap<>();
        speedbuffer = new HashMap<>();
        speed1 = new HashMap<>();
        sneakgrace = new HashMap<>();
        watergrace = new HashMap<>();
        lastLegitLoc = new HashMap<>();
        penalizeTimestamp = new HashMap<>();
        velocities = new HashMap<>();
        launchVelocity = new HashMap<>();
        FRICTION_AIR = 1 - FRICTION_AIR * (1 - EPSILON);
        FRICTION_WATER = 1 - FRICTION_WATER * (1 - EPSILON);
        FRICTION_GROUND = 1 - FRICTION_GROUND * (1 - EPSILON);
        startLoops();
    }

    @Override
    public void check(PositionEvent event) {
        final Player player = event.getPlayer();
        if(event.hasTeleported())
            lastLegitLoc.put(player.getUniqueId(), event.getTo());
        if (!player.isFlying()) { //TODO: give a bit of grace time after one toggles fly off

            double finalspeed = Math.pow(event.getTo().getX() - event.getFrom().getX(), 2) + Math.pow(event.getTo().getZ() - event.getFrom().getZ(), 2);
            if(finalspeed < 0.0001)
                return;

            double speedThresSoft = SPEED_THRES_SOFT;
            double speedThresHard = SPEED_THRES_HARD;
            int failBufferSize = FAIL_BUFFER_1;

            if (!speedygrace.containsKey(player.getUniqueId()))
                speedygrace.put(player.getUniqueId(), 0);
            if (speedygrace.get(player.getUniqueId()) == 1) {
                failBufferSize = FAIL_BUFFER_2;
                speedThresSoft = 0.5;
                speedThresHard = 0.5;
            }
            if (speedygrace.get(player.getUniqueId()) == 2) {
                failBufferSize = 15;
                speedThresSoft = 1.3;
                speedThresHard = 1.3;
            }

            if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                speedThresSoft *= speedBoost(player);
                speedThresHard *= speedBoost(player);
                failBufferSize = 6;
            }

            if (AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, -0.5, 0), "STAIRS") || AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, -0.5, 0), "STEP")) {
                if (player.isSprinting() && speedygrace.get(player.getUniqueId()) != 2) {
                    failBufferSize = FAIL_BUFFER_2;
                    speedThresSoft = 0.5;
                    speedThresHard = 0.5;
                    speedygrace.put(player.getUniqueId(), 1);
                    speedygracetimer.put(player.getUniqueId(), 0);
                }
            }
            Material b = event.getTo().clone().add(0, -1.0, 0).getBlock().getType();
            if (b.name().contains("ICE")) {
                if (player.isSprinting() && speedygrace.get(player.getUniqueId()) != 2) {
                    failBufferSize = FAIL_BUFFER_2;
                    speedThresSoft = 0.5;
                    speedThresHard = 0.5;
                    speedygrace.put(player.getUniqueId(), 1);
                    speedygracetimer.put(player.getUniqueId(), 0);
                }
            }
            b = event.getTo().clone().add(0, -1.5, 0).getBlock().getType();
            if (b.name().contains("ICE") && speedygrace.get(player.getUniqueId()) != 2) {
                if (player.isSprinting()) {
                    speedThresSoft = 0.5;
                    speedygrace.put(player.getUniqueId(), 1);
                    speedygracetimer.put(player.getUniqueId(), 0);
                }
            }

            if (AdjacentBlocks.blockAdjacentIsSolid(event.getTo().clone().add(0, 2, 0)) && event.isOnGroundReally() && player.isSprinting()) {
                if (speedygrace.get(player.getUniqueId()) != 2) {
                    speedygrace.put(player.getUniqueId(), 1);
                    failBufferSize = FAIL_BUFFER_2;
                    speedThresSoft = 0.5;
                    speedThresHard = 0.5;
                    speedygracetimer.put(player.getUniqueId(), 0);
                }
                if (AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, -1, 0), "ICE") && speedygrace.get(player.getUniqueId()) != 3) {
                    speedThresSoft = 1.3;
                    speedThresHard = 1.3;
                    speedygrace.put(player.getUniqueId(), 2);
                    speedygracetimer.put(player.getUniqueId(), 0);
                }
                if (AdjacentBlocks.matContainsStringIsAdjacent(event.getTo(), "TRAP") || AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, 1, 0), "TRAP")) {
                    failBufferSize = 15;
                    speedThresSoft = 1.3;
                    speedygrace.put(player.getUniqueId(), 2);
                    speedygracetimer.put(player.getUniqueId(), 0);
                }
            }

			/*
            The water speed check is somewhat frustrating and complicated. Let me explain what happens:
			1) Check if player is in water, and if true, wait a bit before checking. If not, reset delay A before checking surface water.
			2) If player is in water and is treading, set a threshold with a high buffer size.
			3) If the player goes underwater, wait a bit before checking. If not, reset delay B before checking under water.
			4) If player is underwater, set a threshold with a low buffer size.
			 */
            //noinspection deprecation
            if (event.getTo().getBlock().getData() == 0 && !AdjacentBlocks.matContainsStringIsAdjacent(event.getTo(), "WATER_LILY") && (AdjacentBlocks.matContainsStringIsAdjacent(event.getTo(), "WATER") || AdjacentBlocks.matContainsStringIsAdjacent(event.getTo(), "LAVA") ||
                    AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, 1, 0), "WATER") || AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, 1, 0), "LAVA"))) {
                if (!watergrace.containsKey(player.getUniqueId()))
                    watergrace.put(player.getUniqueId(), WATER_TREAD_GRACE);
                if (watergrace.get(player.getUniqueId()) <= 0) { //if delay is equal or less than 0, then start checking for water treading speed.
                    speedThresSoft = 0.014; //set water tread speed thres
                    //set speedthresHard?
                    failBufferSize = 11; //set water tread speed buffer size
                    if (watergrace.get(player.getUniqueId()) == -WATER_UNDER_GRACE && (AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, 1, 0), "WATER") || AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, 1, 0), "LAVA"))) {
                        speedThresSoft = 0.011; //if player is fully underwater and delay is at -10, set underwater speed thres
                        failBufferSize = 4; //set underwater speed buffer size
                    } else if (watergrace.get(player.getUniqueId()) != -WATER_UNDER_GRACE) {
                        watergrace.put(player.getUniqueId(), watergrace.get(player.getUniqueId()) - 1); //decrement delay for every move until -10
                        if (watergrace.get(player.getUniqueId()) == -WATER_UNDER_GRACE) {
                            speedbuffer.put(player.getUniqueId(), 0); //reset speed buffer once player has dove into the water
                        }
                    }
                    if(Hawk.getServerVersion() > 7 && player.getInventory().getBoots() != null)
                        speedThresSoft += 0.0386 * player.getInventory().getBoots().getEnchantmentLevel(Enchantment.DEPTH_STRIDER); //increase threshold if player has depth-strider enchant
                    if (!(AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, 1, 0), "WATER") || AdjacentBlocks.matContainsStringIsAdjacent(event.getTo().clone().add(0, 1, 0), "LAVA"))) {
                        watergrace.put(player.getUniqueId(), 0); //set delay back to 0 if still treading water
                    }
                } else {
                    watergrace.put(player.getUniqueId(), watergrace.get(player.getUniqueId()) - 1); //if in water, for every move, decrement until 0
                }
            } else {
                watergrace.put(player.getUniqueId(), WATER_TREAD_GRACE); //if not in water, reset grace.
            }

            /*
            1) If player is sneaking and appears to be on the ground, decrement delay until 0. Otherwise, reset delay to 6.
            2) If delay is at 0, set threshold.
             */
            if (player.isSneaking() && event.isOnGroundReally()) {
                if (!sneakgrace.containsKey(player.getUniqueId())) sneakgrace.put(player.getUniqueId(), 6);
                if (sneakgrace.get(player.getUniqueId()) == 0) {
                    if (event.getTo().clone().add(0, -1, 0).getBlock().getType().isSolid())
                        speedThresSoft = 0.009;
                    else
                        speedThresSoft = 0.016; //weird... the client seems to update moves slower rate when sneaking on edges of blocks. The server thinks the player is moving faster. This helps compensate it.
                    if (player.hasPotionEffect(PotionEffectType.SPEED))
                        speedThresSoft *= speedBoost(player);
                } else {
                    sneakgrace.put(player.getUniqueId(), sneakgrace.get(player.getUniqueId()) - 1);
                }
            } else {
                sneakgrace.put(player.getUniqueId(), 6);
            }

            if (player.getWalkSpeed() < 0.2F && event.getTo().clone().add(0, -0.1, 0).getBlock().getType().isSolid()) {
                speedThresSoft *= player.getWalkSpeed() * 5;
                speedThresHard *= player.getWalkSpeed() * 5;
            }
            if (player.getWalkSpeed() > 0.2F) {
                speedThresSoft *= player.getWalkSpeed() * 15;
                speedThresHard *= player.getWalkSpeed() * 15;
            }

            //handle any pending launch events
            if(velocities.containsKey(player.getUniqueId()) && System.currentTimeMillis() - velocities.get(player.getUniqueId()).time <= ServerUtils.getPing(player) + 200 &&
                    Math.abs(velocities.get(player.getUniqueId()).value - finalspeed) < 0.03) {
                launchVelocity.put(player.getUniqueId(), velocities.get(player.getUniqueId()).value);
                velocities.remove(player.getUniqueId());
            }
            if(launchVelocity.getOrDefault(player.getUniqueId(), 0D) > 0) {
                speedThresSoft = Math.max(speedThresSoft, launchVelocity.getOrDefault(player.getUniqueId(), 0D));
                speedThresHard = Math.max(speedThresSoft, speedThresHard);
                //applying horizontal friction
                //on ground
                if(AdjacentBlocks.onGroundReally(event.getFrom(), -1, false)) {
                    launchVelocity.put(player.getUniqueId(), launchVelocity.get(player.getUniqueId()) * FRICTION_GROUND);
                }
                //in air
                else {
                    launchVelocity.put(player.getUniqueId(), launchVelocity.get(player.getUniqueId()) * FRICTION_AIR);
                }
                if(launchVelocity.get(player.getUniqueId()) <= 0.05)
                    launchVelocity.remove(player.getUniqueId());
            }

            //if finalspeed is greater than speedthresSoft * wspeedmultiplier!!!!! remember the wspeedmultiplier
            if (finalspeed > speedThresSoft && event.getTo().getBlock().getType() != Material.PISTON_MOVING_PIECE && !player.isInsideVehicle()) {
                if (finalspeed > speedThresHard) {
                    speedbuffer.put(player.getUniqueId(), failBufferSize + 1);
                }
                speedbuffer.put(player.getUniqueId(), speedbuffer.getOrDefault(player.getUniqueId(), 0) + 1);
                if (speedbuffer.get(player.getUniqueId()) > failBufferSize) {
                    punishAndTryRubberband(player, event, lastLegitLoc.getOrDefault(player.getUniqueId(), player.getLocation()));
                    penalizeTimestamp.put(player.getUniqueId(), System.currentTimeMillis());
                    if (FAIL_BUFFER_RESET) {
                        speedbuffer.put(player.getUniqueId(), 0);
                    }
                    if (!speed1.containsKey(player.getUniqueId())) {
                        speed1.put(player.getUniqueId(), 0);
                    }
                    return;
                }
            }
        } else {
            watergrace.put(player.getUniqueId(), WATER_TREAD_GRACE);
            sneakgrace.put(player.getUniqueId(), 6);
        }

        reward(player);
        if(System.currentTimeMillis() - penalizeTimestamp.getOrDefault(player.getUniqueId(), 0L) >= 500)
            lastLegitLoc.put(player.getUniqueId(), player.getLocation());
    }

    private void startLoops() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(hawk, () -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
                speedbuffer.put(player.getUniqueId(), 0);
            }
        }, 0L, 20L);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(hawk, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isSprinting()) {
                    //sprintgrace.add(player.getUniqueId());
                    sprintgracetimer.put(player.getUniqueId(), 0);
                }
                if (!player.isSprinting()) {
                    if (!sprintgracetimer.containsKey(player.getUniqueId())) {
                        sprintgracetimer.put(player.getUniqueId(), 0);
                    }
                    sprintgracetimer.put(player.getUniqueId(), sprintgracetimer.get(player.getUniqueId()) + 1);
                    if (sprintgracetimer.get(player.getUniqueId()) > 2) {
                        sprintgracetimer.put(player.getUniqueId(), 0);
                        //sprintgrace.remove(player.getUniqueId());
                    }
                }
                speedygracetimer.put(player.getUniqueId(), speedygracetimer.getOrDefault(player.getUniqueId(), 0) + 1);
                if (speedygracetimer.get(player.getUniqueId()) > 2) {
                    speedygrace.put(player.getUniqueId(), 0);
                    speedygracetimer.put(player.getUniqueId(), 0);
                }
            }
        }, 0L, 10L);
    }

    private double speedBoost(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!effect.getType().equals(PotionEffectType.SPEED))
                continue;
            double add = effect.getAmplifier() + 1;
            add = add * 1.2;
            return add;
        }
        return 0;
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        Vector horizVelocity = new Vector(e.getVelocity().getX(), 0, e.getVelocity().getZ());
        velocities.put(uuid, new DoubleTime(horizVelocity.lengthSquared(), System.currentTimeMillis()));
    }

    private class DoubleTime {

        private double value;
        private long time;

        private DoubleTime(double value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    public void removeData(Player player) {
        UUID uuid = player.getUniqueId();
        //sprintgrace.remove(uuid);
        sprintgracetimer.remove(uuid);
        speedygrace.remove(uuid);
        speedygracetimer.remove(uuid);
        speedbuffer.remove(uuid);
        speed1.remove(uuid);
        sneakgrace.remove(uuid);
        watergrace.remove(uuid);
        lastLegitLoc.remove(uuid);
        penalizeTimestamp.remove(uuid);
    }

}
