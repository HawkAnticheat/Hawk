/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.util.entity;

import me.islandscout.hawk.util.AABB;
import net.minecraft.server.v1_7_R4.AxisAlignedBB;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class EntityNMS7 extends EntityNMS {

    public EntityNMS7(Entity entity) {
        super();
        AxisAlignedBB bb = ((CraftEntity) entity).getHandle().boundingBox;
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
    }
}
