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

package me.islandscout.hawk.util.block;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.util.AABB;
import net.minecraft.server.v1_7_R4.Vec3D;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public abstract class BlockNMS {

    final Block obcBlock;
    float strength;
    AABB hitbox;
    AABB[] collisionBoxes;
    boolean solid;
    float slipperiness;

    BlockNMS(Block obcBlock) {
        this.obcBlock = obcBlock;
    }

    public abstract Object getNMS();

    public abstract void sendPacketToPlayer(Player p);

    public float getStrength() {
        return strength;
    }

    public Block getBukkitBlock() {
        return obcBlock;
    }

    public AABB getHitBox() {
        return hitbox;
    }

    public AABB[] getCollisionBoxes() {
        return collisionBoxes;
    }

    public boolean isColliding(AABB other) {
        for (AABB cBox : collisionBoxes) {
            if (cBox.isColliding(other))
                return true;
        }
        return false;
    }

    public static BlockNMS getBlockNMS(Block b) {
        if (Hawk.getServerVersion() == 8)
            return new BlockNMS8(b);
        else
            return new BlockNMS7(b);
    }

    public float getSlipperiness() {
        return slipperiness;
    }

    //Man, I hate having to do this. I don't know why Bukkit is confused over the definition of SOLID.
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSolid() {
        return solid;
    }

    public Vector getFlowDirection() {
        return new Vector();
    }
}
