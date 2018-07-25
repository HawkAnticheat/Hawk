package me.islandscout.hawk.gui;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
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
            protected void doAction(Player p, Hawk hawk) {
                Window checks = new ToggleChecksWindow(hawk, p);
                hawk.getGuiManager().sendWindow(p, checks);
            }
        });

        elements.add(new Element(5, Material.PAPER, "Reload Configuration") {
            @Override
            protected void doAction(Player p, Hawk hawk) {
                hawk.reloadConfig();
                hawk.loadMessages();
                hawk.getCheckManager().loadChecks();
                p.sendMessage(ChatColor.DARK_AQUA + "Reloaded configuration files for Hawk.");
            }
        });

        ItemStack notify = new ItemStack(Material.INK_SACK);
        notify.setDurability((short)(pp.canReceiveFlags() ? 10 : 8));
        ItemMeta notifyName = notify.getItemMeta();
        notifyName.setDisplayName(pp.canReceiveFlags() ? "Notifications: ON" : "Notifications: OFF");
        notify.setItemMeta(notifyName);
        elements.add(new Element(3, notify) {
            @Override
            protected void doAction(Player p, Hawk hawk) {
                pp.setReceiveFlags(!pp.canReceiveFlags());
                Window mainMenu = new MainMenuWindow(hawk, p);
                hawk.getGuiManager().sendWindow(p, mainMenu);
            }
        });

        elements.add(new Element(8, Material.WOOD_DOOR, "Exit GUI") {
            @Override
            protected void doAction(Player p, Hawk hawk) {
                p.closeInventory();
            }
        });

        prepareInventory();
    }
}
