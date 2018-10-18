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

package me.islandscout.hawk.checks.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.BlockPlacementCheck;
import me.islandscout.hawk.events.BlockPlaceEvent;
import me.islandscout.hawk.utils.AABB;
import me.islandscout.hawk.utils.ConfigHelper;
import me.islandscout.hawk.utils.Placeholder;
import me.islandscout.hawk.utils.Ray;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BlockInteractHitbox extends BlockPlacementCheck {

    private final boolean DEBUG_HITBOX;
    private final boolean DEBUG_RAY;
    private final double MAX_REACH_SQUARED;

    public BlockInteractHitbox() {
        super("blockinteracthitbox", true, 10, 10, 0.9, 5000, "%player% failed block interact hitbox. %type% VL: %vl%", null);
        DEBUG_HITBOX = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.blockinteracthitbox.debug.hitbox");
        DEBUG_RAY = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.blockinteracthitbox.debug.ray");
        MAX_REACH_SQUARED = Math.pow(ConfigHelper.getOrSetDefault(6, hawk.getConfig(), "checks.blockinteracthitbox.maxReach"), 2);
    }

    @Override
    protected void check(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Location eyeLoc = pp.getLocation().clone();
        eyeLoc.add(0, (p.isSneaking() ? 1.54 : 1.62), 0);
        Location targetLocation = e.getTargetBlockLocation();

        Vector min = targetLocation.toVector();
        Vector max = targetLocation.toVector().add(new Vector(1, 1, 1));
        AABB aabb = new AABB(min, max);

        Ray ray = new Ray(eyeLoc.toVector(), eyeLoc.getDirection());

        if (DEBUG_HITBOX)
            aabb.highlight(hawk, p.getWorld(), 0.25);
        if (DEBUG_RAY)
            ray.highlight(hawk, p.getWorld(), Math.sqrt(MAX_REACH_SQUARED), 0.3);

        Vector intersection = aabb.intersectsRay(ray, 0, Float.MAX_VALUE);

        if (intersection == null) {
            punishAndTryCancelAndBlockDestroy(pp, e, new Placeholder("type", "Did not hit hitbox."));
            return;
        }

        double distanceSquared = new Vector(intersection.getX() - eyeLoc.getX(), intersection.getY() - eyeLoc.getY(), intersection.getZ() - eyeLoc.getZ()).lengthSquared();
        if (distanceSquared > MAX_REACH_SQUARED) {
            punishAndTryCancelAndBlockDestroy(pp, e, new Placeholder("type", "Reached too far."));
            return;
        }

        reward(pp);
    }
}
