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

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.util.AABB;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public abstract class WrappedEntity {

    AABB collisionBox;
    float collisionBorderSize;
    float length;
    float width;
    float height;
    Location location;

    WrappedEntity() {
    }

    public static WrappedEntity getWrappedEntity(Entity entity) {
        if (Hawk.getServerVersion() == 8)
            return WrappedEntity8.getWrappedEntity(entity);
        else
            return WrappedEntity7.getWrappedEntity(entity);
    }

    public AABB getCollisionBox(Vector entityPos) {
        Vector move = entityPos.clone().subtract(location.toVector());
        AABB box = getCollisionBox().clone();
        box.translate(move);
        return box;
    }

    public AABB getHitbox(Vector entityPos) {
        AABB box = getCollisionBox(entityPos);
        box.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
        return box;
    }

    public AABB getCollisionBox() {
        return collisionBox;
    }

    public AABB getHitbox() {
        AABB hitbox = collisionBox.clone();
        hitbox.expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
        return hitbox;
    }

    public float getLength() {
        return length;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
