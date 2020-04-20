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

package me.islandscout.hawk.module;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.gui.Element;
import me.islandscout.hawk.gui.MainMenuWindow;
import me.islandscout.hawk.gui.Window;
import me.islandscout.hawk.util.ConfigHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager implements Listener {

    private final Map<UUID, Window> activeWindows;
    private final Hawk hawk;
    private boolean enabled;

    public GUIManager(Hawk hawk) {
        this.hawk = hawk;
        activeWindows = new HashMap<>();
        enabled = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "gui");
        if (enabled)
            Bukkit.getPluginManager().registerEvents(this, hawk);
    }

    public void sendWindow(Player p, Window window) {
        if (!enabled) return;
        activeWindows.put(p.getUniqueId(), null);
        activeWindows.put(p.getUniqueId(), window);
        p.openInventory(window.getInventory());
    }

    public void sendMainMenuWindow(Player p) {
        if (!enabled) return;
        Window window = new MainMenuWindow(hawk, p);
        sendWindow(p, window);
    }

    @EventHandler
    public void clickEvent(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player))
            return;
        Player p = (Player) e.getWhoClicked();
        if (!activeWindows.containsKey(p.getUniqueId()) || activeWindows.get(p.getUniqueId()) == null)
            return;
        if (!activeWindows.get(p.getUniqueId()).getInventory().equals(e.getClickedInventory()))
            return;
        e.setCancelled(true);
        String perm = Hawk.BASE_PERMISSION + ".gui";
        if (!p.hasPermission(perm)) {
            p.sendMessage(String.format(Hawk.NO_PERMISSION, perm));
            p.closeInventory();
            return;
        }
        Window window = activeWindows.get(p.getUniqueId());
        int clickedLoc = e.getRawSlot();
        for (int i = 0; i < window.getElements().length; i++) {
            if (i == clickedLoc) {
                Element element = window.getElements()[i];
                if (element == null)
                    break;
                element.doAction(p, hawk);
                break;
            }
        }
    }

    public void stop() {
        for (UUID uuid : activeWindows.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null)
                return;
            p.closeInventory();
        }
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
