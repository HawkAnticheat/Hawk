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
import me.islandscout.hawk.wrap.entity.human.WrappedEntityHuman8;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHumanEntity;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class WrappedEntity8 extends WrappedEntity {

    protected net.minecraft.server.v1_8_R3.Entity nmsEntity;

    public WrappedEntity8(Entity entity) {
        super();
        nmsEntity = ((CraftEntity) entity).getHandle();
        AxisAlignedBB bb = nmsEntity.getBoundingBox();
        collisionBox = new AABB(new Vector(bb.a, bb.b, bb.c), new Vector(bb.d, bb.e, bb.f));
        collisionBorderSize = nmsEntity.ao();
        location = entity.getLocation();
    }

    public static WrappedEntity8 getWrappedEntity(Entity entity) {
        if(entity instanceof CraftHumanEntity)
            return new WrappedEntityHuman8(entity);
        else
            return new WrappedEntity8(entity);

    }
}
