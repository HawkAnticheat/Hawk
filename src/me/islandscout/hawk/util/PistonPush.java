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

package me.islandscout.hawk.util;

import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class PistonPush {

    private World world;
    private Vector position;
    private BlockFace direction;
    private long timestamp;

    public PistonPush(World world, Vector position, BlockFace direction, long timestamp) {
        this.world = world;
        this.position = position;
        this.direction = direction;
        this.timestamp = timestamp;
    }

    public World getWorld() {
        return world;
    }

    public Vector getPosition() {
        return position;
    }

    public BlockFace getDirection() {
        return direction;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
