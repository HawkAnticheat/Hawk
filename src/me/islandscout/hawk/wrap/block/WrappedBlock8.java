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

package me.islandscout.hawk.wrap.block;

import me.islandscout.hawk.util.AABB;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.material.*;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class WrappedBlock8 extends WrappedBlock {

    private final net.minecraft.server.v1_8_R3.Block block;

    public WrappedBlock8(Block block) {
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
        slipperiness = b.frictionFactor;

        this.block = b;
    }

    public net.minecraft.server.v1_8_R3.Block getNMS() {
        return block;
    }

    public void sendPacketToPlayer(Player p) {
        Location loc = getBukkitBlock().getLocation();
        PacketPlayOutBlockChange pac = new PacketPlayOutBlockChange(((CraftWorld) loc.getWorld()).getHandle(), new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(pac);
    }

    @Override
    public float getDamage(HumanEntity entity) {
        return block.getDamage(((CraftHumanEntity)entity).getHandle(), ((CraftWorld)obBlock.getWorld()).getHandle(), new BlockPosition(obBlock.getX(), obBlock.getY(), obBlock.getZ()));
    }

    @Override
    public boolean isMaterialAlwaysDestroyable() {
        return block.getMaterial().isAlwaysDestroyable();
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
        if (matData instanceof Sign || matData instanceof Banner)
            reallySolid = false;
        else if (matData instanceof FlowerPot || matData instanceof Diode || matData instanceof Skull ||
                b.getType() == org.bukkit.Material.CARPET || matData instanceof Ladder ||
                b.getType() == Material.REDSTONE_COMPARATOR || b.getType() == Material.REDSTONE_COMPARATOR_ON ||
                b.getType() == Material.REDSTONE_COMPARATOR_OFF || b.getType() == Material.SOIL ||
                b.getType() == Material.WATER_LILY || b.getType() == Material.SNOW) {
            reallySolid = true;
        }
        return reallySolid;
    }

    private AABB[] getCollisionBoxes(net.minecraft.server.v1_8_R3.Block b, Location loc, BlockPosition bPos, IBlockData data) {

        //define boxes for funny blocks
        if (b instanceof BlockCarpet) {
            AABB[] aabbarr = new AABB[1];
            aabbarr[0] = new AABB(loc.toVector(), loc.toVector().add(new Vector(1, 0.0625, 1)));
            return aabbarr;
        }
        if (b instanceof BlockSnow && data.get(BlockSnow.LAYERS) == 1) {
            AABB[] aabbarr = new AABB[1];
            aabbarr[0] = new AABB(loc.toVector(), loc.toVector().add(new Vector(1, 0, 1)));
            return aabbarr;
        }

        List<AxisAlignedBB> bbs = new ArrayList<>();
        AxisAlignedBB cube = AxisAlignedBB.a(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getBlockX() + 1, loc.getBlockY() + 1, loc.getBlockZ() + 1);
        b.a(((CraftWorld) loc.getWorld()).getHandle(), bPos, data, cube, bbs, null);

        AABB[] collisionBoxes = new AABB[bbs.size()];
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB bb = bbs.get(i);
            AABB collisionBox = new AABB(new Vector(bb.a, bb.b, bb.c), new Vector(bb.d, bb.e, bb.f));
            collisionBoxes[i] = collisionBox;
        }

        return collisionBoxes;
    }

    public Vector getFlowDirection() {
        Vector vec = new Vector();
        Vec3D nmsVec = new Vec3D(0, 0, 0);
        Entity dummy = null;
        if(!block.getMaterial().isLiquid())
            return vec;
        BlockPosition bPos = new BlockPosition(obBlock.getX(), obBlock.getY(), obBlock.getZ());

        //this should prevent async threads from calling NMS code that actually loads chunks
        if(!Bukkit.isPrimaryThread()) {
            if(!obBlock.getWorld().isChunkLoaded(obBlock.getX() >> 4, obBlock.getZ() >> 4) ||
                    !obBlock.getWorld().isChunkLoaded(obBlock.getX() + 1 >> 4, obBlock.getZ() >> 4) ||
                    !obBlock.getWorld().isChunkLoaded(obBlock.getX() - 1 >> 4, obBlock.getZ() >> 4) ||
                    !obBlock.getWorld().isChunkLoaded(obBlock.getX() >> 4, obBlock.getZ() + 1 >> 4) ||
                    !obBlock.getWorld().isChunkLoaded(obBlock.getX() >> 4, obBlock.getZ() - 1 >> 4)) {
                return vec;
            }
        }

        nmsVec = block.a(((CraftWorld) obBlock.getWorld()).getHandle(), bPos, dummy, nmsVec);
        vec.setX(nmsVec.a);
        vec.setY(nmsVec.b);
        vec.setZ(nmsVec.c);
        return vec;
    }
}
