package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.*;
import me.islandscout.hawk.utils.entities.EntityNMS;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class Fly extends AsyncMovementCheck implements Listener {

    //TODO: false flag with pistons
    //TODO: false flag on slime blocks
    //TODO: false flag while jumping down stairs
    //TODO: false flag when kb'd out of water
    //TODO: false flag when jumping on edge of block. Perhaps extrapolate next "noPos" moves until they touch the block, then reset expectedDeltaY
    //TODO: BYPASS! You can fly over fences. Jump, then toggle fly, then walk straight.
    //Don't change how you determine if on ground, even though that's what caused this. Instead, check when landing when deltaY > 0
    //perhaps check if player's jump height is great enough?

    //TODO: false flag when jumping on recently placed block
    //To fix this... You'll need to work on PhantomBlocks/ClientBlocks (more in HawkPlayer)
    //Fly check will keep track of positions ON phantom blocks in a List. If a phantom block passes, the fly check
    //will remove it from the List. If a phantom block fails, the fly check will rubberband the player to the location
    //before touching the failed phantom block, then clear the List. Fly check will setCancelled(true) positions as long as the list
    //is not empty.

    //Look, if you're going to rubberband to a location like that, you should make a priority system. For example, if
    //you jump on a phantomblock that gets cancelled, and then X moves ahead if you get flagged for speed AND fly (for failing phantomblock)
    //on the same move, and speed is before fly in the process list, you'll get rubberbanded to the speed legit location and not the fly legit
    //location; a fly bypass. The priority system should give priority to older setback locations if a conflict like this
    //should occur.

    private Map<UUID, Double> lastDeltaY;
    private Map<UUID, Location> legitLoc;
    private Set<UUID> inAir;
    private Map<UUID, Integer> stupidMoves;
    private Map<UUID, List<Location>> locsOnPBlocks;
    private Map<UUID, DoubleTime> velocities; //launch velocities
    private Set<UUID> failedSoDontUpdateRubberband; //Update rubberband loc until someone fails. In this case, do not update until they touch the ground.
    private static final int STUPID_MOVES = 1; //Apparently you can jump in midair right as you fall off the edge of a block. You need to time it right.

    public Fly() {
        super("fly", true, 0, 10, 0.995, 1000, "&7%player% failed fly. VL: %vl%", null);
        lastDeltaY = new HashMap<>();
        inAir = new HashSet<>();
        legitLoc = new HashMap<>();
        stupidMoves = new HashMap<>();
        locsOnPBlocks = new HashMap<>();
        velocities = new HashMap<>();
        failedSoDontUpdateRubberband = new HashSet<>();
    }

    @Override
    protected void check(PositionEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        double deltaY = event.getTo().getY() - event.getFrom().getY();
        if(pp.hasFlyPending() && p.getAllowFlight())
            return;
        if(!event.isOnGroundReally() && !p.isFlying() && !p.isInsideVehicle() && !AdjacentBlocks.blockAdjacentIsLiquid(event.getTo()) &&
                !isInClimbable(event.getTo()) && !isOnBoat(event.getTo())) {

            if(!inAir.contains(p.getUniqueId()) && deltaY > 0)
                lastDeltaY.put(p.getUniqueId(), 0.42 + getJumpBoostLvl(p) * 0.1);

            //handle any pending launch events
            if(velocities.containsKey(p.getUniqueId()) && System.currentTimeMillis() - velocities.get(p.getUniqueId()).time <= ServerUtils.getPing(p) + 200 &&
                    Math.abs(velocities.get(p.getUniqueId()).value - deltaY) < 0.01) {
                lastDeltaY.put(p.getUniqueId(), velocities.get(p.getUniqueId()).value);
                velocities.remove(p.getUniqueId());
            }

            double expectedDeltaY = lastDeltaY.getOrDefault(p.getUniqueId(), 0D);
            double epsilon = 0.03;

            //lastDeltaY.put(p.getUniqueId(), (lastDeltaY.getOrDefault(p.getUniqueId(), 0D) - 0.025) * 0.8); //water function
            if(AdjacentBlocks.matIsAdjacent(event.getTo(), Material.WEB)) {
                lastDeltaY.put(p.getUniqueId(), -0.007);
                epsilon = 0.000001;
                if(AdjacentBlocks.onGroundReally(event.getTo().clone().add(0, -0.03, 0), -1, false))
                    return;
            }
            else
                lastDeltaY.put(p.getUniqueId(), (lastDeltaY.getOrDefault(p.getUniqueId(), 0D) - 0.08) * 0.98);

            //handle teleport
            if(event.hasTeleported()) {
                lastDeltaY.put(p.getUniqueId(), 0D);
                expectedDeltaY = 0;
                legitLoc.put(p.getUniqueId(), event.getTo());
            }

            if(deltaY - expectedDeltaY > epsilon && event.hasDeltaPos()) { //oopsie daisy. client made a goof up

                //wait one little second: minecraft is being a pain in the ass and it wants to play tricks when you parkour on the very edge of blocks
                //we need to check this first...
                if(deltaY < 0) {
                    Location checkLoc = event.getFrom().clone();
                    checkLoc.setY(event.getTo().getY());
                    if(AdjacentBlocks.onGroundReally(checkLoc, deltaY, false)) {
                        onGroundStuff(p, event);
                        return;
                    }
                    //extrapolate move BEFORE getFrom, then check
                    checkLoc.setY(event.getFrom().getY());
                    checkLoc.setX(checkLoc.getX() - (event.getTo().getX() - event.getFrom().getX()));
                    checkLoc.setZ(checkLoc.getZ() - (event.getTo().getZ() - event.getFrom().getZ()));
                    if(AdjacentBlocks.onGroundReally(checkLoc, deltaY, false)) {
                        onGroundStuff(p, event);
                        return;
                    }
                }

                //scold the child
                punish(pp);
                tryRubberband(event, legitLoc.getOrDefault(p.getUniqueId(), event.getFrom()));
                lastDeltaY.put(p.getUniqueId(), canCancel()? 0:deltaY);
                failedSoDontUpdateRubberband.add(p.getUniqueId());
                return;
            }

            reward(pp);

            //the player is in air now, since they have a positive Y velocity and they're not on the ground
            if(inAir.contains(p.getUniqueId()))
                stupidMoves.put(p.getUniqueId(), 0);

            //handle stupid moves, because the client tends to want to jump a little late if you jump off the edge of a block
            if(stupidMoves.getOrDefault(p.getUniqueId(), 0) >= STUPID_MOVES || (deltaY > 0 && AdjacentBlocks.onGroundReally(event.getFrom(), -1, true)))
                inAir.add(p.getUniqueId());
            stupidMoves.put(p.getUniqueId(), stupidMoves.getOrDefault(p.getUniqueId(), 0) + 1);
        }

        else {
            onGroundStuff(p, event);
        }

        if(!failedSoDontUpdateRubberband.contains(p.getUniqueId()) || event.isOnGroundReally()) {
            legitLoc.put(p.getUniqueId(), event.getFrom());
            failedSoDontUpdateRubberband.remove(p.getUniqueId());
        }

    }

    private void onGroundStuff(Player p, PositionEvent e) {
        lastDeltaY.put(p.getUniqueId(), 0D);
        inAir.remove(p.getUniqueId());
        stupidMoves.put(p.getUniqueId(), 0);
    }

    //TODO: Fix issues on edge of chunks
    private boolean isOnBoat(Location loc) {
        Chunk chunk = ServerUtils.getChunkAsync(loc);
        if(chunk == null)
            return false;
        Entity[] entities = chunk.getEntities().clone(); //Thread safety (IOOB exception), so clone?
        for(Entity entity : entities) {
            if(entity instanceof Boat) {
                AABB boatBB = EntityNMS.getEntityNMS(entity).getCollisionBox();
                AABB feet = new AABB(
                        new Vector(-0.3, -0.4, -0.3).add(loc.toVector()),
                        new Vector(0.3, 0, 0.3).add(loc.toVector()));
                if(feet.isColliding(boatBB))
                    return true;
            }
        }
        return false;
    }

    private boolean isInClimbable(Location loc) {
        Block b = ServerUtils.getBlockAsync(loc);
        return b != null && (b.getType() == Material.VINE || b.getType() == Material.LADDER);
    }

    private int getJumpBoostLvl(Player p) {
        for(PotionEffect pEffect : p.getActivePotionEffects()) {
            if(pEffect.getType().equals(PotionEffectType.JUMP)) {
                return pEffect.getAmplifier() + 1;
            }
        }
        return 0;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        velocities.put(uuid, new DoubleTime(e.getVelocity().getY(), System.currentTimeMillis()));
    }

    private class DoubleTime {

        private double value;
        private long time;

        private DoubleTime(double value, long time) {
            this.value = value;
            this.time = time;
        }
    }

    @Override
    public void removeData(Player p) {
        UUID uuid = p.getUniqueId();
        lastDeltaY.remove(uuid);
        inAir.remove(uuid);
        legitLoc.remove(uuid);
        stupidMoves.remove(uuid);
        velocities.remove(uuid);
        failedSoDontUpdateRubberband.remove(uuid);
    }
}
