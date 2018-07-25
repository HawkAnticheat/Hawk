package me.islandscout.hawk.utils.blocks;

import me.islandscout.hawk.utils.AABB;
import net.minecraft.server.v1_7_R4.AxisAlignedBB;
import net.minecraft.server.v1_7_R4.MinecraftServer;
import net.minecraft.server.v1_7_R4.PacketPlayOutBlockChange;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BlockNMS7 extends BlockNMS {

    private net.minecraft.server.v1_7_R4.Block block;

    public BlockNMS7(Block block) {
        super(block);
        net.minecraft.server.v1_7_R4.Block b = MinecraftServer.getServer().getWorld().getType(block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ());

        strength = b.f(null, 0, 0, 0);

        AxisAlignedBB nmsAABB = b.a(((CraftWorld) block.getWorld()).getHandle(), block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ());
        Vector min;
        Vector max;
        if(nmsAABB == null) {
            Vector notExist = new Vector(0, 0, 0);
            min = notExist;
            max = notExist;
        }
        else {
            min = new Vector(nmsAABB.a, nmsAABB.b, nmsAABB.c);
            max = new Vector(nmsAABB.d, nmsAABB.e, nmsAABB.f);
        }
        aabb = new AABB(min, max);
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
}
