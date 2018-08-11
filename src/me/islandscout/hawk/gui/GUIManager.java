package me.islandscout.hawk.gui;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.utils.ConfigHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager implements Listener {

    private Map<UUID, Window> activeWindows;
    private final Hawk hawk;
    private boolean enabled;

    public GUIManager(Hawk hawk) {
        this.hawk = hawk;
        activeWindows = new HashMap<>();
        enabled = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "gui");
        if(enabled)
            Bukkit.getPluginManager().registerEvents(this, hawk);
    }

    public void sendWindow(Player p, Window window) {
        if(!enabled) return;
        activeWindows.put(p.getUniqueId(), null);
        activeWindows.put(p.getUniqueId(), window);
        p.openInventory(window.getInventory());
    }

    public void sendMenuWindow(Player p) {
        if(!enabled) return;
        Window window = new MainMenuWindow(hawk, p);
        sendWindow(p, window);
    }

    @EventHandler
    public void clickEvent(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player))
            return;
        Player p = (Player)e.getWhoClicked();
        if(!activeWindows.containsKey(p.getUniqueId()) || activeWindows.get(p.getUniqueId()) == null)
            return;
        if(!activeWindows.get(p.getUniqueId()).getInventory().equals(e.getClickedInventory()))
            return;
        e.setCancelled(true);
        if(!p.hasPermission(Hawk.BASE_PERMISSION + ".gui")) {
            p.sendMessage(ChatColor.RED + "You do not have permission to perform this action.");
            p.closeInventory();
            return;
        }
        Window window = activeWindows.get(p.getUniqueId());
        int clickedLoc = e.getRawSlot();
        for(Element element : window.getElements()) {
            if(element.getLocation() == clickedLoc) {
                element.doAction(p, hawk);
                break;
            }
        }
    }
}
