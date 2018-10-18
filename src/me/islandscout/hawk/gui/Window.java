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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public abstract class Window {

    protected final Inventory inventory;
    protected final Hawk hawk;
    protected final List<Element> elements;
    protected final Player p;

    public Window(Hawk hawk, Player p, int rows, String title) {
        inventory = Bukkit.createInventory(null, rows * 9, title);
        this.p = p;
        this.hawk = hawk;
        this.elements = new ArrayList<>();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public List<Element> getElements() {
        return elements;
    }

    protected void prepareInventory() {
        for (Element element : elements)
            inventory.setItem(element.getLocation(), element.getItemStack());
    }
}
