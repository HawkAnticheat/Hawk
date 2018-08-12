package me.islandscout.hawk.utils.blocks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.utils.AABB;
import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_8_R3.Chunk;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.material.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BlockNMS8 extends BlockNMS {

    private net.minecraft.server.v1_8_R3.Block block;

    public BlockNMS8(Block block) {
        super(block);
        BlockPosition.MutableBlockPosition bPos = new BlockPosition.MutableBlockPosition();
        bPos.c(block.getX(), block.getY(), block.getZ());
        IBlockData data = ((CraftWorld) block.getWorld()).getHandle().getType(bPos);
        net.minecraft.server.v1_8_R3.Block b = data.getBlock();
        b.updateShape(((CraftWorld) block.getWorld()).getHandle(), bPos);

        strength = b.g(null, null);
        solid = isReallySolid(block);
        hitbox = getHitBox(b, block.getLocation());
        collisionBoxes = getCollisionBoxes(b, block.getLocation(), bPos, data);

        this.block = b;
    }

    public net.minecraft.server.v1_8_R3.Block getNMS() {
        return block;
    }

    public void sendPacketToPlayer(Player p) {
        Location loc = getBukkitBlock().getLocation();
        PacketPlayOutBlockChange pac = new PacketPlayOutBlockChange(((CraftWorld)loc.getWorld()).getHandle(), new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(pac);
    }

    private AABB getHitBox(net.minecraft.server.v1_8_R3.Block b, Location loc) {

        Vector min = new Vector(loc.getX() + b.B(), loc.getY() + b.D(), loc.getZ() + b.F());
        Vector max = new Vector(loc.getX() + b.C(), loc.getY() + b.E(), loc.getZ() + b.G());

        return new AABB(min, max);
    }

    //Bukkit, get your crap straight and stay consistent on the definition of SOLID.
    private boolean isReallySolid(Block b) {
        boolean reallySolid = b.getType().isSolid();
        MaterialData matData = b.getState().getData();
        if(matData instanceof Sign || matData instanceof Banner)
            reallySolid = false;
        else if(matData instanceof FlowerPot || matData instanceof Diode || matData instanceof Skull ||
                b.getType() == org.bukkit.Material.CARPET || matData instanceof Ladder ||
                b.getType() == Material.REDSTONE_COMPARATOR || b.getType() == Material.REDSTONE_COMPARATOR_ON ||
                b.getType() == Material.REDSTONE_COMPARATOR_OFF || b.getType() == Material.SOIL ||
                b.getType() == Material.WATER_LILY) {
            reallySolid = true;
        }
        return reallySolid;
    }

    //TODO: Fence gates are broken. Please fix.
    private AABB[] getCollisionBoxes(net.minecraft.server.v1_8_R3.Block b, Location loc, BlockPosition bPos, IBlockData data) {

        List<AxisAlignedBB> bbs = new ArrayList<>();
        AxisAlignedBB cube = AxisAlignedBB.a(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getBlockX() + 1, loc.getBlockY() + 1, loc.getBlockZ() + 1);
        b.a(((CraftWorld) loc.getWorld()).getHandle(), bPos, data, cube, bbs, null);

        AABB[] collisionBoxes = new AABB[bbs.size()];
        for(int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB bb = bbs.get(i);
            AABB collisionBox = new AABB(new Vector(bb.a, bb.b, bb.c), new Vector(bb.d, bb.e, bb.f));
            collisionBoxes[i] = collisionBox;
        }

        return collisionBoxes;
    }
}
