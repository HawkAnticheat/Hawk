/*
 * This file is part of Hawk Anticheat.
 * Copyright (C) 2018 Hawk Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.EntityInteractionCheck;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.util.block.BlockNMS;
import me.islandscout.hawk.util.entity.EntityNMS;
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
    private final double BOX_EPSILON = 0.05;

    public FightHitbox() {
        super("fighthitbox", false, 5, 10000, 0.95, 5000, "%player% failed combat hitbox. %type% VL: %vl%", null);
        CHECK_BOX_INTERSECTION = (boolean) customSetting("checkBoxIntersection", "", true);
        CHECK_OCCLUSION = (boolean) customSetting("checkOccluding", "", false);
        MAX_REACH = (double) customSetting("maxReach", "", 3.1);
        CHECK_OTHER_ENTITIES = (boolean) customSetting("checkOtherEntities", "", false);
        LAG_COMPENSATION = (boolean) customSetting("lagCompensation", "", true);
        PING_LIMIT = (int) customSetting("pingLimit", "", -1);
        DEBUG_HITBOX = (boolean) customSetting("hitbox", "debug", false);
        DEBUG_RAY = (boolean) customSetting("ray", "debug", false);
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
        Location attackerEyeLocation;

        //Extrapolate last position. (For 1.7 clients ONLY)
        //Unfortunately, there will be false positives from 1.7 users because 1.7's hit detection isn't broken (unlike 1.8).
        //There is no effective way to stop these false positives without creating bypasses.
        if (ServerUtils.getClientVersion(attacker) == 7) {
            attackerEyeLocation = att.getPosition().clone().add(new Vector(0, 1.62, 0)).toLocation(att.getWorld());
        }
        else {
            attackerEyeLocation = new Location(att.getWorld(), att.getPosition().getX(), att.getPosition().getY(), att.getPosition().getZ(), att.getYaw(), att.getPitch()).clone().add(0, 1.62, 0);
        }

        Vector attackerDirection = attackerEyeLocation.getDirection();

        double maxReach = MAX_REACH;
        if (attacker.getGameMode() == GameMode.CREATIVE)
            maxReach += 1.9;

        Vector victimLocation;
        if (LAG_COMPENSATION)
            victimLocation = hawk.getLagCompensator().getHistoryLocation(ping, e.getEntity()).toVector();
        else
            victimLocation = e.getEntity().getLocation().toVector();

        Vector eyePos = new Vector(attackerEyeLocation.getX(), attacker.isSneaking() ? attackerEyeLocation.getY() - 0.08 : attackerEyeLocation.getY(), attackerEyeLocation.getZ());
        Vector direction = new Vector(attackerDirection.getX(), attackerDirection.getY(), attackerDirection.getZ());
        Ray attackerRay = new Ray(eyePos, direction);

        AABB victimAABB;
        victimAABB = EntityNMS.getEntityNMS(entity).getHitbox(victimLocation);
        victimAABB.expand(BOX_EPSILON, BOX_EPSILON, BOX_EPSILON);

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
                punish(att, 1, true, e, new Placeholder("type", "Reach: " + MathPlus.round(interDistance, 2) + "m"));
                return;
            }
            if (CHECK_OCCLUSION && interDistance > 1D) {
                BlockIterator iter = new BlockIterator(attacker.getWorld(), eyePos, attackerDirection, 0, (int) interDistance + 1);
                while (iter.hasNext()) {
                    Block bukkitBlock = iter.next();

                    if (bukkitBlock.getType() == Material.AIR || bukkitBlock.isLiquid())
                        continue;

                    BlockNMS b = BlockNMS.getBlockNMS(bukkitBlock);
                    Vector intersection = b.getHitBox().intersectsRay(new Ray(attackerEyeLocation.toVector(), attackerDirection), 0, Float.MAX_VALUE);
                    if (intersection != null) {
                        if (intersection.distance(eyePos) < interDistance) {
                            punish(att, 1, true, e, new Placeholder("type", "Interacted through " + b.getBukkitBlock().getType()));
                            return;
                        }
                    }
                }

            }
        } else if (CHECK_BOX_INTERSECTION) {
            punish(att, 1, true, e, new Placeholder("type", "Did not hit hitbox."));
            return;
        }

        reward(att); //reward player
    }
}
