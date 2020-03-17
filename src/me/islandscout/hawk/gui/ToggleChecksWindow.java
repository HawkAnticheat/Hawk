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
import me.islandscout.hawk.check.Check;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ToggleChecksWindow extends Window {

    //TODO: Paginate this menu (door code wont work w/ 54 checks)

    public ToggleChecksWindow(Hawk hawk, Player p) {
        super(hawk, p, ((hawk.getCheckManager().getChecks().size() - 1) / 9) + 1, ChatColor.GOLD + "Toggle checks");
        List<Check> list = hawk.getCheckManager().getChecks();

        for (int i = 0; i < list.size(); i++) {
            ItemStack status;
            String display = list.get(i).getName();
            status = new ItemStack(Material.INK_SACK);
            if (list.get(i).isEnabled()) {
                status.setDurability((short) 10);
                display += ": ENABLED";
            } else {
                status.setDurability((short) 8);
                display += ": DISABLED";
            }
            ItemMeta buttonName = status.getItemMeta();
            buttonName.setDisplayName(display);
            status.setItemMeta(buttonName);
            final int location = i;
            elements[i] = new Element(status) {
                @Override
                public void doAction(Player p, Hawk hawk) {
                    Check check = hawk.getCheckManager().getChecks().get(location);
                    check.setEnabled(!check.isEnabled());
                    Window window = new ToggleChecksWindow(hawk, p);
                    hawk.getGuiManager().sendWindow(p, window);
                }
            };
        }

        elements[53] = new Element(Material.WOOD_DOOR, ChatColor.RED + "Return to Main Menu") {
            @Override
            public void doAction(Player p, Hawk hawk) {
                hawk.getGuiManager().sendWindow(p, new MainMenuWindow(hawk, p));
            }
        };

        prepareInventory();
    }
}
