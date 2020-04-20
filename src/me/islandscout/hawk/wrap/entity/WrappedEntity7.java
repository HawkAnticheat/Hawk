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

package me.islandscout.hawk.wrap.entity;

import me.islandscout.hawk.util.AABB;
import me.islandscout.hawk.wrap.entity.human.WrappedEntityHuman7;
import net.minecraft.server.v1_7_R4.AxisAlignedBB;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftHumanEntity;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class WrappedEntity7 extends WrappedEntity {

    protected net.minecraft.server.v1_7_R4.Entity nmsEntity;

    public WrappedEntity7(Entity entity) {
        super();
        nmsEntity = ((CraftEntity) entity).getHandle();
        AxisAlignedBB bb = nmsEntity.boundingBox;
        Vector min;
        Vector max;
        if (bb != null) {
            min = new Vector(bb.a, bb.b, bb.c);
            max = new Vector(bb.d, bb.e, bb.f);
        } else {
            min = new Vector(0, 0, 0);
            max = new Vector(0, 0, 0);
        }
        collisionBox = new AABB(min, max);

        //MCP to the rescue! Minecraft client 1.7.10 net.minecraft.entity.Entity#getCollisionBorderSize()
        //You wouldn't expect this method to be in NMS since it is used for hit scanning for the client.
        collisionBorderSize = nmsEntity.af();
        location = entity.getLocation();
    }

    public static WrappedEntity7 getWrappedEntity(Entity entity) {
        if (entity instanceof CraftHumanEntity)
            return new WrappedEntityHuman7(entity);
        else
            return new WrappedEntity7(entity);
    }
}
