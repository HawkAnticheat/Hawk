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

package me.islandscout.hawk.check.interaction.terrain;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.BlockInteractionCheck;
import me.islandscout.hawk.event.InteractWorldEvent;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.Placeholder;
import me.islandscout.hawk.util.Ray;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import me.islandscout.hawk.util.AABB;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class BlockInteractOcclusion extends BlockInteractionCheck {

    public BlockInteractOcclusion() {
        super("blockinteractocclusion", "%player% failed block interact occlusion, Type: %type%, VL: %vl%");
    }

    @Override
    protected void check(InteractWorldEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        Vector eyePos = pp.getPosition().clone().add(new Vector(0, pp.isSneaking() ? 1.54 : 1.62, 0));
        Vector direction = MathPlus.getDirection(pp.getYaw(), pp.getPitch());

        Location bLoc = e.getTargetedBlockLocation();
        Block b = bLoc.getBlock();
        WrappedBlock bNMS = WrappedBlock.getWrappedBlock(b, pp.getClientVersion());
        AABB targetAABB = new AABB(bNMS.getHitBox().getMin(), bNMS.getHitBox().getMax());

        double distance = targetAABB.distanceToPosition(eyePos);
        BlockIterator iter = new BlockIterator(pp.getWorld(), eyePos, direction, 0, (int) distance + 2);
        while (iter.hasNext()) {
            Block bukkitBlock = iter.next();

            if (bukkitBlock.getType() == Material.AIR || bukkitBlock.isLiquid())
                continue;
            if (bukkitBlock.getLocation().equals(bLoc))
                break;

            WrappedBlock iterBNMS = WrappedBlock.getWrappedBlock(bukkitBlock, pp.getClientVersion());
            AABB checkIntersection = new AABB(iterBNMS.getHitBox().getMin(), iterBNMS.getHitBox().getMax());
            Vector occludeIntersection = checkIntersection.intersectsRay(new Ray(eyePos, direction), 0, Float.MAX_VALUE);
            if (occludeIntersection != null) {
                if (occludeIntersection.distance(eyePos) < distance) {
                    Placeholder ph = new Placeholder("type", iterBNMS.getBukkitBlock().getType());
                    punishAndTryCancelAndBlockRespawn(pp, 1, e, ph);
                    return;
                }
            }
        }
    }
}
