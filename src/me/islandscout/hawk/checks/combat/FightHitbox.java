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

package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.EntityInteractionCheck;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.*;
import me.islandscout.hawk.utils.blocks.BlockNMS;
import me.islandscout.hawk.utils.entities.EntityNMS;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

/**
 * The hit detection in Minecraft is mostly determined by the client,
 * thus servers are left to trust the client. (There are a few exceptions
 * for the unmodified Notchian server such as preventing hits exceeding 6
 * blocks from the target entity). This poses a problem for players and
 * server owners since players can modify the client to manipulate hit
 * detection and allow for cheating when in combat with other players.
 * <p>
 * Hawk’s hitbox checking aims to prevent cheating during player vs.
 * player combat by validating hits on the server. When the server
 * receives an interaction packet from the client, Hawk gets the client’s
 * last known position, extrapolating if necessary, and uses it to
 * perform a ray-trace test on the victim’s hitbox. Before the ray-trace
 * test begins, though, Hawk must factor in latency by rewinding the
 * victim’s hitbox to an appropriate time based on the client’s ping
 * latency. To accomplish this, Hawk keeps a history of players’
 * positions and timestamps in a table and it retrieves a position that
 * best approximates where it appeared to the client when the client
 * attacked it. Hawk then performs the ray-trace test to determine
 * whether the client landed the hit or not. Although the system may
 * produce false positives with clients on unstable connections, it is a
 * fair tradeoff between user experience and cheat prevention.
 * <p>
 * Please disable flags and punishing commands for this check. Treat this
 * check as if it isn't a real check, but rather a hit registration
 * system. There will be false positives.
 */
public class FightHitbox extends EntityInteractionCheck {

    //PASSED: (9/30/18)

    private final double MAX_REACH;
    private final int PING_LIMIT;
    private final boolean CHECK_OTHER_ENTITIES;
    private final boolean LAG_COMPENSATION;
    private final boolean DEBUG_HITBOX;
    private final boolean DEBUG_RAY;
    private final boolean CHECK_OCCLUSION;
    private final boolean CHECK_BOX_INTERSECTION;

    public FightHitbox() {
        super("fighthitbox", true, 5, 10000, 0.95, 5000, "%player% failed combat hitbox. %type% VL: %vl%", null);
        CHECK_BOX_INTERSECTION = ConfigHelper.getOrSetDefault(true, hawk.getConfig(), "checks.fighthitbox.checkBoxIntersection");
        CHECK_OCCLUSION = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.fighthitbox.checkOccluding");
        MAX_REACH = ConfigHelper.getOrSetDefault(3.1, hawk.getConfig(), "checks.fighthitbox.maxReach");
        CHECK_OTHER_ENTITIES = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.fighthitbox.checkOtherEntities");
        LAG_COMPENSATION = ConfigHelper.getOrSetDefault(true, hawk.getConfig(), "checks.fighthitbox.lagCompensation");
        PING_LIMIT = ConfigHelper.getOrSetDefault(-1, hawk.getConfig(), "checks.fighthitbox.pingLimit");
        DEBUG_HITBOX = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.fighthitbox.debug.hitbox");
        DEBUG_RAY = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.fighthitbox.debug.ray");
    }

    protected void check(InteractEntityEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player) && !CHECK_OTHER_ENTITIES)
            return;
        Player attacker = e.getPlayer();
        int ping = ServerUtils.getPing(attacker);
        if (ping > PING_LIMIT && PING_LIMIT != -1)
            return;

        HawkPlayer att = e.getHawkPlayer();
        Location attackerEyeLocation = att.getLocation().clone().add(0, 1.62, 0);

        //Extrapolate last position. (For 1.7 clients ONLY)
        //Unfortunately, there will be false positives from 1.7 users due to the nature of how the client interacts
        //with entities. There is no effective way to stop these false positives without creating bypasses.
        if (ServerUtils.getClientVersion(attacker) == 7) {
            Vector attackerVelocity = att.getVelocity().clone();
            Vector attackerDeltaRotation = new Vector(att.getDeltaYaw(), att.getDeltaPitch(), 0);
            double moveDelay = System.currentTimeMillis() - att.getLastMoveTime();
            if (moveDelay >= 100) {
                moveDelay = 0D;
            } else {
                moveDelay = moveDelay / 50;
            }
            attackerVelocity.multiply(moveDelay);
            attackerDeltaRotation.multiply(moveDelay);

            attackerEyeLocation.add(attackerVelocity);

            attackerEyeLocation.setYaw(attackerEyeLocation.getYaw() + (float) attackerDeltaRotation.getX());
            attackerEyeLocation.setPitch(attackerEyeLocation.getPitch() + (float) attackerDeltaRotation.getY());
        }

        Vector attackerDirection = attackerEyeLocation.getDirection();

        double maxReach = MAX_REACH;
        if (attacker.getGameMode() == GameMode.CREATIVE)
            maxReach += 1.8; //MC1.7: 1.8, MC1.8: 1.5

        Location victimLocation;
        if (LAG_COMPENSATION && entity instanceof Player)
            victimLocation = hawk.getLagCompensator().getHistoryLocation(ping, (Player) e.getEntity());
        else
            victimLocation = e.getEntity().getLocation();

        Vector eyePos = new Vector(attackerEyeLocation.getX(), attacker.isSneaking() ? attackerEyeLocation.getY() - 0.08 : attackerEyeLocation.getY(), attackerEyeLocation.getZ());
        Vector direction = new Vector(attackerDirection.getX(), attackerDirection.getY(), attackerDirection.getZ());
        Ray attackerRay = new Ray(eyePos, direction);

        AABB victimAABB;
        if (entity instanceof Player) {
            Vector min = new Vector(victimLocation.getX() - 0.45, victimLocation.getY(), victimLocation.getZ() - 0.45);
            Vector max = new Vector(victimLocation.getX() + 0.45, victimLocation.getY() + 2, victimLocation.getZ() + 0.45);
            victimAABB = new AABB(min, max);
        } else {
            victimAABB = EntityNMS.getEntityNMS(entity).getCollisionBox();
        }

        Vector intersectVec3d = victimAABB.intersectsRay(attackerRay, 0, Float.MAX_VALUE);

        if (DEBUG_HITBOX) {
            victimAABB.highlight(hawk, attacker.getWorld(), 0.29);
        }

        if (DEBUG_RAY) {
            attackerRay.highlight(hawk, attacker.getWorld(), maxReach, 0.1);
        }

        if (intersectVec3d != null) {
            Location intersect = new Location(attacker.getWorld(), intersectVec3d.getX(), intersectVec3d.getY(), intersectVec3d.getZ());
            double interDistance = intersect.distance(attackerEyeLocation);
            if (interDistance > maxReach) {
                punish(att, true, e, new Placeholder("type", "Reach: " + MathPlus.round(interDistance, 2) + "m"));
                return;
            }
            if (CHECK_OCCLUSION && interDistance > 1D) {
                BlockIterator iter = new BlockIterator(attacker.getWorld(), eyePos, attackerDirection, 0, (int) interDistance + 1);
                while (iter.hasNext()) {
                    Block bukkitBlock = iter.next();

                    if (bukkitBlock.getType() == Material.AIR || bukkitBlock.isLiquid())
                        continue;

                    BlockNMS b = BlockNMS.getBlockNMS(bukkitBlock);
                    AABB checkIntersection = new AABB(b.getHitBox().getMin(), b.getHitBox().getMax());
                    Vector intersection = checkIntersection.intersectsRay(new Ray(attackerEyeLocation.toVector(), attackerDirection), 0, Float.MAX_VALUE);
                    if (intersection != null) {
                        if (intersection.distance(eyePos) < interDistance) {
                            punish(att, true, e, new Placeholder("type", "Interacted through " + b.getBukkitBlock().getType()));
                            return;
                        }
                    }
                }

            }
        } else if (CHECK_BOX_INTERSECTION) {
            punish(att, true, e, new Placeholder("type", "Did not hit hitbox."));
            return;
        }

        reward(att); //reward player
    }
}
