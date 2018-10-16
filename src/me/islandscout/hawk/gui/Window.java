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
