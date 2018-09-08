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
        super(hawk, p, ((hawk.getCheckManager().getCheckList().size() - 1) / 9) + 1, ChatColor.GOLD + "Toggle checks");
        List<Check> list = hawk.getCheckManager().getCheckList();
        for(int i = 0; i < list.size(); i++) {
            ItemStack status;
            String display = list.get(i).getName();
            status = new ItemStack(Material.INK_SACK);
            if(list.get(i).isEnabled()) {
                status.setDurability((short)10);
                display += ": ENABLED";
            }
            else {
                status.setDurability((short)8);
                display += ": DISABLED";
            }
            ItemMeta buttonName = status.getItemMeta();
            buttonName.setDisplayName(display);
            status.setItemMeta(buttonName);
            elements.add(i, new Element(i, status) {
                @Override
                void doAction(Player p, Hawk hawk) {
                    Check check = hawk.getCheckManager().getCheckList().get(getLocation());
                    check.setEnabled(!check.isEnabled());
                    Window window = new ToggleChecksWindow(hawk, p);
                    hawk.getGuiManager().sendWindow(p, window);
                }
            });

            prepareInventory();
        }
    }
}
