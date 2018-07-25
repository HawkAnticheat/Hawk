package me.islandscout.hawk.utils.blocks;

import me.islandscout.hawk.utils.AABB;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BlockNMS8 extends BlockNMS {

    private net.minecraft.server.v1_8_R3.Block block;

    public BlockNMS8(Block block) {
        super(block);
        BlockPosition bPos = new BlockPosition(block.getX(), block.getY(), block.getZ());
        net.minecraft.server.v1_8_R3.Block b = MinecraftServer.getServer().getWorld().getType(bPos).getBlock();

        strength = b.g(null, null);

        b.updateShape(((CraftWorld) block.getWorld()).getHandle(), bPos);
        Vector min = new Vector((double) block.getX() + b.B(), (double) block.getY() + b.D(), (double) block.getZ() + b.F());
        Vector max = new Vector((double) block.getX() + b.C(), (double) block.getY() + b.E(), (double) block.getZ() + b.G());
        aabb = new AABB(min, max);
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
}
