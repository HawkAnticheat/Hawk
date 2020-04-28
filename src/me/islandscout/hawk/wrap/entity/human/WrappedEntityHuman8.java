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

package me.islandscout.hawk.wrap.entity.human;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import me.islandscout.hawk.wrap.entity.WrappedEntity8;
import net.minecraft.server.v1_8_R3.EntityHuman;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public class WrappedEntityHuman8 extends WrappedEntity8 implements WrappedEntityHuman {

    public WrappedEntityHuman8(Entity entity) {
        super(entity);
    }

    @Override
    public boolean canHarvestBlock(Block block) {
        Object obj = WrappedBlock.getWrappedBlock(block, Hawk.getServerVersion()).getNMS();
        net.minecraft.server.v1_8_R3.Block b = (net.minecraft.server.v1_8_R3.Block) obj;
        return ((EntityHuman) nmsEntity).b(b);
    }

    @Override
    public float getCurrentPlayerStrVsBlock(Block block, boolean flag) {
        Object obj = WrappedBlock.getWrappedBlock(block, Hawk.getServerVersion()).getNMS();
        net.minecraft.server.v1_8_R3.Block b = (net.minecraft.server.v1_8_R3.Block) obj;
        return ((EntityHuman) nmsEntity).a(b);
    }

    @Override
    public void releaseItem() {
        ((EntityHuman) nmsEntity).bU();
    }

    @Override
    public boolean usingItem() {
        return ((EntityHuman) nmsEntity).bS();
    }
}
