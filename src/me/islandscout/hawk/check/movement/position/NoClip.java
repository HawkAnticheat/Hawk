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

package me.islandscout.hawk.check.movement.position;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.AABB;
import me.islandscout.hawk.util.Placeholder;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.Openable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NoClip extends MovementCheck {

    //TODO handle when player edits terrain

    private static final double VERTICAL_EPSILON = 0.01;
    private static final Material[] exemptedMats = {Material.WEB, Material.CHEST, Material.ANVIL, Material.PISTON_MOVING_PIECE};

    public NoClip() {
        super("noclip", false, 0, 10, 0.995, 5000, "%player% failed no-clip. Moved into %block%. VL: %vl%", null);
    }

    @Override
    protected void check(MoveEvent e) {
        AABB aabb = AABB.playerCollisionBox.clone();
        aabb.expand(0, -VERTICAL_EPSILON, 0);
        aabb.translate(e.getTo().toVector());

        HawkPlayer pp = e.getHawkPlayer();

        Block b = blockCollided(aabb, pp, exemptedMats);

        if(b != null) {
            punishAndTryRubberband(pp, e, new Placeholder("block", b.getType()));
        } else {
            reward(pp);
        }
    }

    private Block blockCollided(AABB aabb, HawkPlayer pp, Material... exemptedMats) {
        Set<Material> exempt = new HashSet<>(Arrays.asList(exemptedMats));
        Set<Location> ignored = pp.getIgnoredBlockCollisions();

        Vector min = aabb.getMin();
        Vector max = aabb.getMax();

        for (int x = (int)Math.floor(min.getX()); x < (int)Math.ceil(max.getX()); x++) {
            for (int y = (int)Math.floor(min.getY()); y < (int)Math.ceil(max.getY()); y++) {
                for (int z = (int)Math.floor(min.getZ()); z < (int)Math.ceil(max.getZ()); z++) {
                    Block block = ServerUtils.getBlockAsync(new Location(pp.getPlayer().getWorld(), x, y, z));

                    if(block == null || exempt.contains(block.getType()) ||
                            block.getState().getData() instanceof Openable || ignored.contains(block.getLocation()))
                        continue;

                    AABB[] blockBoxes = WrappedBlock.getWrappedBlock(block, pp.getClientVersion()).getCollisionBoxes();

                    for(AABB box : blockBoxes) {
                        if(aabb.isColliding(box)) {
                            return block;
                        }
                    }

                }
            }
        }

        return null;
    }
}
