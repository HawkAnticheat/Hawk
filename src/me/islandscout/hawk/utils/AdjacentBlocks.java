package me.islandscout.hawk.utils;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.utils.blocks.BlockNMS;
import org.bukkit.Location;
import org.bukkit.Material;
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
        for(int i = blocks.size() - 1; i >= 0; i--) {
            Block currBlock = blocks.get(i);
            if(currBlock == null || currBlock.getType() == Material.AIR || (prevBlock != null && currBlock.equals(prevBlock))) {
                blocks.remove(i);
            }
            prevBlock = currBlock;
        }
        return blocks;
    }

    public static boolean matIsAdjacent(Location loc, Material material) {
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
        for(Block b : sample) {
            if(b != null && b.getType() == material)
                return true;
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
        for(Block b : sample) {
            if(b != null && b.getType().name().contains(name))
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
        for(Block b : sample) {
            if(b != null && b.getType().isSolid())
                return true;
        }
        return false;
    }

    //TODO: this still needs to get optimized. Replace List with Set
    /**
     * Checks if the location is on ground. Good replacement for Entity#isOnGround()
     * since that flag can be spoofed by the client. Hawk's definition of being on
     * ground: yVelocity must not exceed 0.5625, player's feet must not be inside
     * a solid block's AABB, and there must be at least 1 solid block AABB collision
     * with the AABB defining the thin area below the location (represents below the
     * player's feet).
     * @param loc Test location
     * @param yVelocity Y-velocity
     * @return boolean
     */
    //if not sure what your velocity is, just put -1 for velocity
    //if you just want to check for location, just put -1 for velocity
    public static boolean onGroundReally(Location loc, double yVelocity) {
        if(yVelocity > 0.5625) //allows stepping up short blocks, but not full blocks
            return false;
        double feetDepth = 0.02; //Don't set this too low. The client doesn't like to send moves unless they are significant enough.
        //If too low, this might set off fly false flags when jumping on edge of blocks.
        //TO DO: Perhaps replace "depth" with "groundPrecision" and "feetDepth". feetDepth should not be less than 0.02. groundPrecision should be very very close to 0.
        Location check = loc.clone();
        List<Block> blocks = new ArrayList<>();
        blocks.addAll(AdjacentBlocks.getBlocksInLocation(check.add(0, -1, 0)));
        blocks.addAll(AdjacentBlocks.getBlocksInLocation(check.add(0, 0.999, 0))); //TODO: YAY! FLY-CLIP ISSUE IS BACK! Better figure out what's going on with this line. 0.999?
        Block prevBlock = null;
        for(int i = blocks.size() - 1; i >= 0; i--) {
            Block currBlock = blocks.get(i);
            if(prevBlock != null && currBlock.equals(prevBlock)) {
                blocks.remove(i);
            }
            prevBlock = currBlock;
        }

        AABB underFeet = new AABB(check.add(-0.3, 0, -0.3).toVector(), check.add(0.6, feetDepth, 0.6).toVector());
        for(Block block : blocks) {
            BlockNMS bNMS = BlockNMS.getBlockNMS(block);
            if(block.isLiquid() || (!bNMS.isSolid() && Hawk.getServerVersion() == 8))
                continue;
            if(bNMS.isColliding(underFeet)) {

                //almost done. gotta do one more check... Check if their foot ain't in a block. (stops checkerclimb)
                AABB topFeet = underFeet.clone();
                topFeet.translate(new Vector(0, feetDepth, 0));
                for(Block block1 : AdjacentBlocks.getBlocksInLocation(loc)) {
                    BlockNMS bNMS1 = BlockNMS.getBlockNMS(block1);
                    if(block1.isLiquid() || (!bNMS1.isSolid() && Hawk.getServerVersion() == 8) || block1.getState().getData() instanceof Openable)
                        continue;
                    if(bNMS1.isColliding(topFeet))
                        return false;
                }

                return true;
            }
        }
        return false;
    }

    public static boolean blockNearbyIsSolid(Location loc) {
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
        for(Block b : sample) {
            if(b != null && b.getType().isSolid())
                return true;
        }
        return false;
    }
}
