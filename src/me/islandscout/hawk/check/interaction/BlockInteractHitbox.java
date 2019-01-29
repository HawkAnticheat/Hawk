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

package me.islandscout.hawk.check.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.BlockInteractionCheck;
import me.islandscout.hawk.event.InteractWorldEvent;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.util.block.BlockNMS;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class BlockInteractHitbox extends BlockInteractionCheck {

    private final boolean DEBUG_HITBOX;
    private final boolean DEBUG_RAY;
    private final double MAX_REACH;
    private final boolean CHECK_OCCLUSION;
    private final boolean ALWAYS_CANCEL_OCCLUSION;

    public BlockInteractHitbox() {
        super("blockinteracthitbox", true, 10, 10, 0.9, 5000, "%player% failed block interact hitbox. %type% VL: %vl%", null);
        DEBUG_HITBOX = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.blockinteracthitbox.debug.hitbox");
        DEBUG_RAY = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.blockinteracthitbox.debug.ray");
        MAX_REACH = ConfigHelper.getOrSetDefault(6.0, hawk.getConfig(), "checks.blockinteracthitbox.maxReach");
        CHECK_OCCLUSION = (boolean)customSetting("enabled", "checkOccluding", true);
        ALWAYS_CANCEL_OCCLUSION = (boolean)customSetting("alwaysCancel", "checkOccluding", true);
    }

    @Override
    protected void check(InteractWorldEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Location eyeLoc = pp.getLocation().clone();
        eyeLoc.add(0, (p.isSneaking() ? 1.54 : 1.62), 0);
        Location targetLocation = e.getTargetedBlockLocation();

        Vector min = targetLocation.toVector();
        Vector max = targetLocation.toVector().add(new Vector(1, 1, 1));
        AABB aabb = new AABB(min, max);
        Vector direction = eyeLoc.getDirection();
        Ray ray = new Ray(eyeLoc.toVector(), eyeLoc.getDirection());

        if (DEBUG_HITBOX)
            aabb.highlight(hawk, p.getWorld(), 0.25);
        if (DEBUG_RAY)
            ray.highlight(hawk, p.getWorld(), MAX_REACH, 0.3);

        Vector intersection = aabb.intersectsRay(ray, 0, Float.MAX_VALUE);

        if (intersection == null) {
            punishAndTryCancelAndBlockRespawn(pp, e, new Placeholder("type", "Did not hit hitbox."));
            return;
        }

        double distance = new Vector(intersection.getX() - eyeLoc.getX(), intersection.getY() - eyeLoc.getY(), intersection.getZ() - eyeLoc.getZ()).length();

        if (CHECK_OCCLUSION) {
            Vector eyePos = eyeLoc.toVector();
            BlockIterator iter = new BlockIterator(p.getWorld(), eyePos, direction, 0, (int) distance + 2);
            while (iter.hasNext()) {
                Block bukkitBlock = iter.next();

                if (bukkitBlock.getType() == Material.AIR || bukkitBlock.isLiquid())
                    continue;
                if (bukkitBlock.getLocation().equals(targetLocation))
                    break;

                BlockNMS b = BlockNMS.getBlockNMS(bukkitBlock);
                AABB checkIntersection = new AABB(b.getHitBox().getMin(), b.getHitBox().getMax());
                Vector occludeIntersection = checkIntersection.intersectsRay(new Ray(eyePos, direction), 0, Float.MAX_VALUE);
                if (occludeIntersection != null) {
                    if (occludeIntersection.distance(eyePos) < distance) {
                        Placeholder ph = new Placeholder("type", "Interacted through " + b.getBukkitBlock().getType());
                        if(ALWAYS_CANCEL_OCCLUSION) {
                            punish(pp, true, e, ph);
                            e.setCancelled(true);
                            blockRespawn(pp, e);
                        } else {
                            punishAndTryCancelAndBlockRespawn(pp, e, ph);
                        }
                        return;
                    }
                }
            }

        }

        if (distance > MAX_REACH) {
            punishAndTryCancelAndBlockRespawn(pp, e, new Placeholder("type", "Reached too far."));
            return;
        }

        reward(pp);
    }
}
