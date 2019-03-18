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

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.util.block.BlockNMS;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.Openable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdjacentBlocks {

    //loop horizontally around nearby blocks about the size of a player's collision box
    //TODO: optimize this? Replace this List with a Set.
    public static List<Block> getBlocksInLocation(Location loc) {
        Location check = loc.clone();
        List<Block> blocks = new ArrayList<>();
        blocks.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        blocks.add(ServerUtils.getBlockAsync(check.add(0.3, 0, 0)));
        blocks.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        blocks.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        blocks.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        blocks.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        blocks.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        blocks.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        Block prevBlock = null;
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Block currBlock = blocks.get(i);
            if (currBlock == null || currBlock.getType() == Material.AIR || (currBlock.equals(prevBlock))) {
                blocks.remove(i);
            }
            prevBlock = currBlock;
        }
        return blocks;
    }

    public static boolean matIsAdjacent(Location loc, Material... materials) {
        Location check = loc.clone();
        Set<Block> sample = new HashSet<>();
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        for (Block b : sample) {
            if(b == null)
                continue;
            for(Material mat : materials) {
                if(b.getType() == mat)
                    return true;
            }
        }
        return false;
    }
    /*public static boolean matIsAdjacent(Location loc, Material material) {
        Location check = loc.clone();
        return ServerUtils.getBlockAsync(check.add(0, 0, 0.3)).getType() == material ||
                ServerUtils.getBlockAsync(check.add(0.3, 0, 0)).getType() == material ||
                ServerUtils.getBlockAsync(check.add(0, 0, -0.3)).getType() == material ||
                ServerUtils.getBlockAsync(check.add(0, 0, -0.3)).getType() == material ||
                ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)).getType() == material ||
                ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)).getType() == material ||
                ServerUtils.getBlockAsync(check.add(0, 0, 0.3)).getType() == material ||
                ServerUtils.getBlockAsync(check.add(0, 0, 0.3)).getType() == material;
    }*/

    public static boolean matContainsStringIsAdjacent(Location loc, String name) {
        Location check = loc.clone();
        Set<Block> sample = new HashSet<>();
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        for (Block b : sample) {
            if (b != null && b.getType().name().contains(name))
                return true;
        }
        return false;
    }

    public static boolean blockAdjacentIsSolid(Location loc) {
        Location check = loc.clone();
        Set<Block> sample = new HashSet<>();
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        for (Block b : sample) {
            if (b != null && b.getType().isSolid())
                return true;
        }
        return false;
    }

    public static boolean blockAdjacentIsLiquid(Location loc) {
        Location check = loc.clone();
        Set<Block> sample = new HashSet<>();
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(-0.3, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 0.3)));
        for (Block b : sample) {
            if (b != null && b.isLiquid())
                return true;
        }
        return false;
    }

    /**
     * Checks if the location is on ground. Good replacement for Entity#isOnGround()
     * since that flag can be spoofed by the client. Hawk's definition of being on
     * ground: yVelocity must not exceed 0.5625, player's feet must not be inside
     * a solid block's AABB, and there must be at least 1 solid block AABB collision
     * with the AABB defining the thin area below the location (represents below the
     * player's feet).
     *
     * @param loc            Test location
     * @param yVelocity      Y-velocity
     * @param ignoreInGround return false if location is inside something
     * @param feetDepth Don't set this too low. The client doesn't like to send moves unless they are significant enough.
     * @return boolean
     */
    //TODO: this still needs to get optimized. Replace List with Set
    //TODO: Pleaaaaaase fix this when jumping onto edges of blocks
    //if not sure what your velocity is, just put -1 for velocity
    //if you just want to check for location, just put -1 for velocity
    public static boolean onGroundReally(Location loc, double yVelocity, boolean ignoreInGround, double feetDepth) {
        if (yVelocity > 0.5625) //allows stepping up short blocks, but not full blocks
            return false;
        //If too low, this might set off fly false flags when jumping on edge of blocks.
        Location check = loc.clone();
        List<Block> blocks = new ArrayList<>();
        blocks.addAll(AdjacentBlocks.getBlocksInLocation(check));
        blocks.addAll(AdjacentBlocks.getBlocksInLocation(check.add(0, -1, 0)));
        Block prevBlock = null;
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Block currBlock = blocks.get(i);
            if (currBlock.equals(prevBlock)) {
                blocks.remove(i);
            }
            prevBlock = currBlock;
        }

        AABB underFeet = new AABB(loc.toVector().add(new Vector(-0.3, -feetDepth, -0.3)), loc.toVector().add(new Vector(0.3, 0, 0.3)));
        for (Block block : blocks) {
            BlockNMS bNMS = BlockNMS.getBlockNMS(block);
            if (block.isLiquid() || (!bNMS.isSolid() && Hawk.getServerVersion() == 8))
                continue;
            if (bNMS.isColliding(underFeet)) {

                //almost done. gotta do one more check... Check if their foot ain't in a block. (stops checkerclimb)
                if (ignoreInGround) {
                    AABB topFeet = underFeet.clone();
                    topFeet.translate(new Vector(0, feetDepth + 0.00001, 0));
                    for (Block block1 : AdjacentBlocks.getBlocksInLocation(loc)) {
                        BlockNMS bNMS1 = BlockNMS.getBlockNMS(block1);
                        if (block1.isLiquid() || (!bNMS1.isSolid() && Hawk.getServerVersion() == 8) || block1.getState().getData() instanceof Openable)
                            continue;
                        if (bNMS1.isColliding(topFeet))
                            return false;
                    }
                }

                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean blockNearbyIsSolid(Location loc, boolean hawkDefinition) {
        Location check = loc.clone();
        Set<Block> sample = new HashSet<>();
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 1)));
        sample.add(ServerUtils.getBlockAsync(check.add(1, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -1)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, -1)));
        sample.add(ServerUtils.getBlockAsync(check.add(-1, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(-1, 0, 0)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 1)));
        sample.add(ServerUtils.getBlockAsync(check.add(0, 0, 1)));
        for (Block b : sample) {
            if(b == null)
                continue;
            if(hawkDefinition) {
                if(BlockNMS.getBlockNMS(b).isSolid())
                    return true;
            }
            else if(b.getType().isSolid())
                return true;
        }
        return false;
    }

    public static Set<Direction> checkTouchingBlock(AABB boundingBox, World world, double borderSize) {
        AABB bigBox = boundingBox.clone();
        Vector min = bigBox.getMin().add(new Vector(-borderSize, -borderSize, -borderSize));
        Vector max = bigBox.getMax().add(new Vector(borderSize, borderSize, borderSize));
        Set<Direction> directions = new HashSet<>();
        //Don't ask why I'm subtracting 1 when the coordinate is less than 0. Beats me too.
        for(int x = (int)(min.getX() < 0 ? min.getX() - 1 : min.getX()); x <= max.getX(); x++) {
            for(int y = (int)(min.getY() < 0 ? min.getY() - 1 : min.getY()); y <= max.getY(); y++) {
                for(int z = (int)(min.getZ() < 0 ? min.getZ() - 1 : min.getZ()); z <= max.getZ(); z++) {
                    Block b = ServerUtils.getBlockAsync(new Location(world, x, y, z));
                    if(b != null) {
                        BlockNMS bNMS = BlockNMS.getBlockNMS(b);
                        for(AABB blockBox : bNMS.getCollisionBoxes()) {
                            if(blockBox.getMin().getX() > boundingBox.getMax().getX() && blockBox.getMin().getX() < bigBox.getMax().getX()) {
                                directions.add(Direction.EAST);
                            }
                            if(blockBox.getMin().getY() > boundingBox.getMax().getY() && blockBox.getMin().getY() < bigBox.getMax().getY()) {
                                directions.add(Direction.TOP);
                            }
                            if(blockBox.getMin().getZ() > boundingBox.getMax().getZ() && blockBox.getMin().getZ() < bigBox.getMax().getZ()) {
                                directions.add(Direction.SOUTH);
                            }
                            if(blockBox.getMax().getX() > bigBox.getMin().getX() && blockBox.getMax().getX() < boundingBox.getMin().getX()) {
                                directions.add(Direction.WEST);
                            }
                            if(blockBox.getMax().getY() > bigBox.getMin().getY() && blockBox.getMax().getY() < boundingBox.getMin().getY()) {
                                directions.add(Direction.BOTTOM);
                            }
                            if(blockBox.getMax().getZ() > bigBox.getMin().getZ() && blockBox.getMax().getZ() < boundingBox.getMin().getZ()) {
                                directions.add(Direction.NORTH);
                            }
                        }
                    }
                }
            }
        }
        return directions;
    }
}
