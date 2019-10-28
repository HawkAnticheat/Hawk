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
import me.islandscout.hawk.util.AABB;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

/** This check prevents players from interacting on
 * unavailable locations on blocks. Players must be
 * looking at the face of the block they want to interact
 * with.
 */
public class WrongBlockFace extends BlockInteractionCheck {

    public WrongBlockFace() {
        super("wrongblockface", true, 0, 10, 0.99, 5000, "%player% failed wrongblockface; interacted on invalid block face, VL: %vl%", null);
    }

    @Override
    protected void check(InteractWorldEvent e) {
        HawkPlayer pp = e.getHawkPlayer();

        Block b = ServerUtils.getBlockAsync(e.getTargetedBlockLocation());
        AABB hitbox;
        if(b != null) {
            hitbox = WrappedBlock.getWrappedBlock(b).getHitBox();
        }
        else {
            hitbox = new AABB(new Vector(), new Vector());
        }

        if(e.getTargetedBlockFaceNormal().dot(MathPlus.getDirection(pp.getYaw(), pp.getPitch())) >= 0 &&
            !hitbox.containsPoint(pp.getHeadPosition())) {
            punishAndTryCancelAndBlockRespawn(pp, e);
        }
        else {
            reward(pp);
        }
    }
}
