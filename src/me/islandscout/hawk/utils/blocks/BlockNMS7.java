package me.islandscout.hawk.utils.blocks;

import me.islandscout.hawk.utils.AABB;
import net.minecraft.server.v1_7_R4.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BlockNMS7 extends BlockNMS {

    //TODO: Define collision box for carpet and snow (1.7)

    private net.minecraft.server.v1_7_R4.Block block;

    public BlockNMS7(Block block) {
        super(block);
        net.minecraft.server.v1_7_R4.Block b = ((CraftWorld)block.getWorld()).getHandle().getType(block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ());

        strength = b.f(null, 0, 0, 0);
        hitbox = getHitBox(b, block.getLocation());
        solid = isReallySolid(block);
        collisionBoxes = getCollisionBoxes(b, block.getLocation());
        frictionFactor = b.frictionFactor;

        this.block = b;
    }

    public net.minecraft.server.v1_7_R4.Block getNMS() {
        return block;
    }

    public void sendPacketToPlayer(Player p) {
        Location loc = getBukkitBlock().getLocation();
        PacketPlayOutBlockChange pac = new PacketPlayOutBlockChange(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), ((CraftWorld)loc.getWorld()).getHandle());
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(pac);
    }

    private AABB getHitBox(net.minecraft.server.v1_7_R4.Block b, Location loc) {
        AxisAlignedBB nmsAABB = b.a(((CraftWorld) loc.getWorld()).getHandle(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Vector min;
        Vector max;
        if(nmsAABB == null) {
            min = new Vector(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            max = new Vector(loc.getBlockX() + 1, loc.getBlockY() + 1, loc.getBlockZ() + 1);
        }
        else {
            min = new Vector(nmsAABB.a, nmsAABB.b, nmsAABB.c);
            max = new Vector(nmsAABB.d, nmsAABB.e, nmsAABB.f);
        }

        return new AABB(min, max);
    }

    private AABB[] getCollisionBoxes(net.minecraft.server.v1_7_R4.Block b, Location loc) {

        //define boxes for funny blocks
        if(b instanceof BlockCarpet) {
            AABB[] aabbarr = new AABB[1];
            aabbarr[0] = new AABB(loc.toVector(), loc.toVector().add(new Vector(1, 0, 1)));
            return aabbarr;
        }

        List<AxisAlignedBB> bbs = new ArrayList<>();
        AxisAlignedBB cube = AxisAlignedBB.a(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getBlockX() + 1, loc.getBlockY() + 1, loc.getBlockZ() + 1);
        b.a(((CraftWorld) loc.getWorld()).getHandle(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), cube, bbs, null);

        AABB[] collisionBoxes = new AABB[bbs.size()];
        for(int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB bb = bbs.get(i);
            AABB collisionBox = new AABB(new Vector(bb.a, bb.b, bb.c), new Vector(bb.d, bb.e, bb.f));
            collisionBoxes[i] = collisionBox;
        }

        return collisionBoxes;
    }


    private boolean isReallySolid(Block b) {
        boolean reallySolid = b.getType().isSolid();
        if(b.getType() == Material.CARPET) {
            reallySolid = true;
        }
        return reallySolid;
    }
}
