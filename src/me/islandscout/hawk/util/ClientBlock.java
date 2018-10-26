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

package me.islandscout.hawk.util;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.Set;

public class ClientBlock {

    private final Location location;
    private final Material material;
    private final long initTick;

    public ClientBlock(Location location, long clientTick, Material material) {
        this.location = location;
        this.material = material;
        initTick = clientTick;
    }

    public Location getLocation() {
        return location;
    }

    public Material getMaterial() {
        return material;
    }

    public long getInitTick() {
        return initTick;
    }

    public static ClientBlock playerIsOnAClientBlock(HawkPlayer pp, Location playerLoc) {
        Set<ClientBlock> clientBlocks = pp.getClientBlocks();
        if(clientBlocks.size() == 0)
            return null;
        AABB feet = new AABB(
                new Vector(-0.3, -0.01, -0.3).add(playerLoc.toVector()),
                new Vector(0.3, 0, 0.3).add(playerLoc.toVector()));
        for (ClientBlock pBlock : clientBlocks) {
            AABB cube = new AABB(pBlock.getLocation().toVector(), pBlock.getLocation().toVector().add(new Vector(1, 1, 1)));
            if (feet.isColliding(cube)) {
                return pBlock;
            }
        }
        return null;
    }
}
