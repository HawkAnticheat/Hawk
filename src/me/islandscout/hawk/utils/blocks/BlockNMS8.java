package me.islandscout.hawk.utils.blocks;

import me.islandscout.hawk.utils.AABB;
import me.islandscout.hawk.utils.Debug;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.material.*;
import org.bukkit.util.Vector;

public class BlockNMS8 extends BlockNMS {

    private net.minecraft.server.v1_8_R3.Block block;

    public BlockNMS8(Block block) {
        super(block);
        BlockPosition bPos = new BlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_8_R3.Block b = MinecraftServer.getServer().getWorld().getType(bPos).getBlock();

        strength = b.g(null, null);
        solid = isReallySolid(block);
        aabb = getAABB(b, block.getLocation(), bPos);
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

    private AABB getAABB(net.minecraft.server.v1_8_R3.Block b, Location loc, BlockPosition bPos) {
        b.updateShape(((CraftWorld) loc.getWorld()).getHandle(), bPos);
        Vector min = new Vector(loc.getX() + b.B(), loc.getY() + b.D(), loc.getZ() + b.F());
        Vector max = new Vector(loc.getX() + b.C(), loc.getY() + b.E(), loc.getZ() + b.G());

        if(b instanceof BlockFence || b instanceof BlockFenceGate || b instanceof BlockCobbleWall) {
            max.add(new Vector(0, 0.5, 0));
        }
        else if(b instanceof BlockSoil) {
            max.add(new Vector(0, 0.0625, 0));
        }
        else if(!solid) {
            min = new Vector(0, 0, 0);
            max = new Vector(0, 0, 0);
        }

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
}
