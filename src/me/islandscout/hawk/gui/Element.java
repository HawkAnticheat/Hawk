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

package me.islandscout.hawk.gui;

import me.islandscout.hawk.Hawk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class Element {

    private final int location;
    private final ItemStack itemStack;

    Element(int location, Material mat, String name) {
        this.location = location;
        this.itemStack = new ItemStack(mat);
        ItemMeta checksName = itemStack.getItemMeta();
        checksName.setDisplayName(name);
        itemStack.setItemMeta(checksName);
    }

    Element(int location, ItemStack itemStack) {
        this.location = location;
        this.itemStack = itemStack;
    }

    public int getLocation() {
        return location;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public abstract void doAction(Player p, Hawk hawk);
}
