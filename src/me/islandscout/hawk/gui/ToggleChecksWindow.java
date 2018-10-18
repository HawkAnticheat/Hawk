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
import me.islandscout.hawk.checks.Check;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ToggleChecksWindow extends Window {

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
            elements.add(i, new Element(i, status) {
                @Override
                public void doAction(Player p, Hawk hawk) {
                    Check check = hawk.getCheckManager().getChecks().get(getLocation());
                    check.setEnabled(!check.isEnabled());
                    Window window = new ToggleChecksWindow(hawk, p);
                    hawk.getGuiManager().sendWindow(p, window);
                }
            });

            prepareInventory();
        }
    }
}
