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

package me.islandscout.hawk.wrap.itemstack;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.wrap.block.WrappedBlock;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class WrappedItemStack7 extends WrappedItemStack {

    private net.minecraft.server.v1_7_R4.ItemStack itemStack;

    WrappedItemStack7(ItemStack obiItemStack) {
        itemStack = CraftItemStack.asNMSCopy(obiItemStack);
    }

    @Override
    public float getDestroySpeed(Block obbBlock) {
        net.minecraft.server.v1_7_R4.Block block =
                (net.minecraft.server.v1_7_R4.Block) WrappedBlock.getWrappedBlock(obbBlock, Hawk.getServerVersion()).getNMS();
        if(itemStack == null)
            return 1F;
        return itemStack.a(block);
    }

    @Override
    public boolean canDestroySpecialBlock(Block obbBlock) {
        net.minecraft.server.v1_7_R4.Block block =
                (net.minecraft.server.v1_7_R4.Block) WrappedBlock.getWrappedBlock(obbBlock, Hawk.getServerVersion()).getNMS();
        if(itemStack == null)
            return false;
        return itemStack.b(block);
    }
}
