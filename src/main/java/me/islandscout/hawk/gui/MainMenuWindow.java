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
import me.islandscout.hawk.HawkPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MainMenuWindow extends Window {

    public MainMenuWindow(Hawk hawk, Player player) {
        super(hawk, player, 1, ChatColor.GOLD + "Hawk Anticheat");
        HawkPlayer pp = hawk.getHawkPlayer(player);

        /*elements[0] = new Element(Material.SAND, "dummy") {
            @Override
            public void doAction(Player p, Hawk hawk) {
                Window testWindow = new TestWindow(hawk, p);
                hawk.getGuiManager().sendWindow(p, testWindow);
            }
        };*/

        elements[4] = new Element(Material.WORKBENCH, "Toggle Checks") {
            @Override
            public void doAction(Player p, Hawk hawk) {
                Window checks = new ToggleChecksWindow(hawk, p);
                hawk.getGuiManager().sendWindow(p, checks);
            }
        };

        elements[5] = new Element(Material.PAPER, "Reload Configuration") {
            @Override
            public void doAction(Player p, Hawk hawk) {
                Bukkit.dispatchCommand(p, "hawk reload");
            }
        };

        ItemStack notify = new ItemStack(Material.INK_SACK);
        notify.setDurability((short) (pp.canReceiveAlerts() ? 10 : 8));
        ItemMeta notifyName = notify.getItemMeta();
        notifyName.setDisplayName(pp.canReceiveAlerts() ? "Notifications: ON" : "Notifications: OFF");
        notify.setItemMeta(notifyName);
        elements[3] = new Element(notify) {
            @Override
            public void doAction(Player p, Hawk hawk) {
                pp.setReceiveNotifications(!pp.canReceiveAlerts());
                Window mainMenu = new MainMenuWindow(hawk, p);
                hawk.getGuiManager().sendWindow(p, mainMenu);
            }
        };

        elements[8] = new Element(Material.WOOD_DOOR, "Exit GUI") {
            @Override
            public void doAction(Player p, Hawk hawk) {
                p.closeInventory();
            }
        };

        prepareInventory();
    }
}
