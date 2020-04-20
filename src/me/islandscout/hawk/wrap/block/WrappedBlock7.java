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
import net.minecraft.server.v1_7_R4.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class WrappedBlock7 extends WrappedBlock {

    private final net.minecraft.server.v1_7_R4.Block block;

    public WrappedBlock7(Block block, int clientVersion) {
        super(block, clientVersion);
        net.minecraft.server.v1_7_R4.Block b = ((CraftWorld) block.getWorld()).getHandle().getType(block.getLocation().getBlockX(), block.getLocation().getBlockY(), block.getLocation().getBlockZ());

        strength = b.f(null, 0, 0, 0);
        hitbox = getHitBox(b, block.getLocation());
        solid = isReallySolid(block);
        collisionBoxes = getCollisionBoxes(b, block.getLocation());
        slipperiness = b.frictionFactor;

        this.block = b;
    }

    public net.minecraft.server.v1_7_R4.Block getNMS() {
        return block;
    }

    public void sendPacketToPlayer(Player p) {
        Location loc = getBukkitBlock().getLocation();
        PacketPlayOutBlockChange pac = new PacketPlayOutBlockChange(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), ((CraftWorld) loc.getWorld()).getHandle());
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(pac);
    }

    @Override
    public float getDamage(HumanEntity entity) {
        return block.getDamage(((CraftHumanEntity) entity).getHandle(), ((CraftWorld) obBlock.getWorld()).getHandle(), obBlock.getX(), obBlock.getY(), obBlock.getZ());
    }

    @Override
    public boolean isMaterialAlwaysDestroyable() {
        return block.getMaterial().isAlwaysDestroyable();
    }

    private AABB getHitBox(net.minecraft.server.v1_7_R4.Block b, Location loc) {
        AxisAlignedBB nmsAABB = b.a(((CraftWorld) loc.getWorld()).getHandle(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Vector min;
        Vector max;
        if (nmsAABB == null) {
            min = new Vector(0, 0, 0);
            max = new Vector(0, 0, 0);
        } else {
            min = new Vector(nmsAABB.a, nmsAABB.b, nmsAABB.c);
            max = new Vector(nmsAABB.d, nmsAABB.e, nmsAABB.f);
        }

        return new AABB(min, max);
    }

    private AABB[] getCollisionBoxes(net.minecraft.server.v1_7_R4.Block b, Location loc) {

        //define boxes for funny blocks
        if (b instanceof BlockCarpet) {
            AABB[] aabbarr = new AABB[1];
            if (clientVersion == 7) {
                aabbarr[0] = new AABB(loc.toVector(), loc.toVector().add(new Vector(1, 0, 1)));
            } else {
                aabbarr[0] = new AABB(loc.toVector(), loc.toVector().add(new Vector(1, 0.0625, 1)));
            }
            return aabbarr;
        }

        List<AxisAlignedBB> bbs = new ArrayList<>();
        AxisAlignedBB cube = AxisAlignedBB.a(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getBlockX() + 1, loc.getBlockY() + 1, loc.getBlockZ() + 1);
        b.a(((CraftWorld) loc.getWorld()).getHandle(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), cube, bbs, null);

        AABB[] collisionBoxes = new AABB[bbs.size()];
        for (int i = 0; i < bbs.size(); i++) {
            AxisAlignedBB bb = bbs.get(i);
            AABB collisionBox = new AABB(new Vector(bb.a, bb.b, bb.c), new Vector(bb.d, bb.e, bb.f));
            collisionBoxes[i] = collisionBox;
        }

        return collisionBoxes;
    }


    private boolean isReallySolid(Block b) {
        boolean reallySolid = b.getType().isSolid();
        if (b.getType() == Material.CARPET || b.getType() == Material.WATER_LILY) {
            reallySolid = true;
        }
        return reallySolid;
    }

    public Vector getFlowDirection() {
        Vector vec = new Vector();
        Vec3D nmsVec = Vec3D.a(0, 0, 0);
        Entity dummy = null;
        if (!block.getMaterial().isLiquid())
            return vec;

        //this should prevent async threads from calling NMS code that actually loads chunks
        if (!Bukkit.isPrimaryThread()) {
            if (!obBlock.getWorld().isChunkLoaded(obBlock.getX() >> 4, obBlock.getZ() >> 4) ||
                    !obBlock.getWorld().isChunkLoaded(obBlock.getX() + 1 >> 4, obBlock.getZ() >> 4) ||
                    !obBlock.getWorld().isChunkLoaded(obBlock.getX() - 1 >> 4, obBlock.getZ() >> 4) ||
                    !obBlock.getWorld().isChunkLoaded(obBlock.getX() >> 4, obBlock.getZ() + 1 >> 4) ||
                    !obBlock.getWorld().isChunkLoaded(obBlock.getX() >> 4, obBlock.getZ() - 1 >> 4)) {
                return vec;
            }
        }

        block.a(((CraftWorld) obBlock.getWorld()).getHandle(), obBlock.getX(), obBlock.getY(), obBlock.getZ(), dummy, nmsVec);
        vec.setX(nmsVec.a);
        vec.setY(nmsVec.b);
        vec.setZ(nmsVec.c);
        return vec;
    }
}
