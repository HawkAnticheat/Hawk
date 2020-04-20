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

package me.islandscout.hawk.gui;

import me.islandscout.hawk.Hawk;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public abstract class Window {

    protected final Inventory inventory;
    protected final Hawk hawk;
    protected final Element[] elements;
    protected final Player p;

    public Window(Hawk hawk, Player p, int rows, String title) {
        inventory = Bukkit.createInventory(null, rows * 9, title);
        this.p = p;
        this.hawk = hawk;
        this.elements = new Element[rows * 9];
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Element[] getElements() {
        return elements;
    }

    protected void prepareInventory() {
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] == null)
                continue;
            inventory.setItem(i, elements[i].getItemStack());
        }
    }

    protected void updateWindow() {
        prepareInventory();
        p.updateInventory();
    }
}
