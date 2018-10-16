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

        elements.add(new Element(4, Material.WORKBENCH, "Toggle Checks") {
            @Override
            public void doAction(Player p, Hawk hawk) {
                Window checks = new ToggleChecksWindow(hawk, p);
                hawk.getGuiManager().sendWindow(p, checks);
            }
        });

        elements.add(new Element(5, Material.PAPER, "Reload Configuration") {
            @Override
            public void doAction(Player p, Hawk hawk) {
                Bukkit.dispatchCommand(p, "hawk reload");
            }
        });

        ItemStack notify = new ItemStack(Material.INK_SACK);
        notify.setDurability((short) (pp.canReceiveNotifications() ? 10 : 8));
        ItemMeta notifyName = notify.getItemMeta();
        notifyName.setDisplayName(pp.canReceiveNotifications() ? "Notifications: ON" : "Notifications: OFF");
        notify.setItemMeta(notifyName);
        elements.add(new Element(3, notify) {
            @Override
            public void doAction(Player p, Hawk hawk) {
                pp.setReceiveNotifications(!pp.canReceiveNotifications());
                Window mainMenu = new MainMenuWindow(hawk, p);
                hawk.getGuiManager().sendWindow(p, mainMenu);
            }
        });

        elements.add(new Element(8, Material.WOOD_DOOR, "Exit GUI") {
            @Override
            public void doAction(Player p, Hawk hawk) {
                p.closeInventory();
            }
        });

        prepareInventory();
    }
}
