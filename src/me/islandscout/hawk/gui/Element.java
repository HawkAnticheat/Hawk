package me.islandscout.hawk.gui;

import me.islandscout.hawk.Hawk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class Element {

    private int location;
    private ItemStack itemStack;

    Element(int location, Material mat, String name) {
        this.location = location;
        this.itemStack = new ItemStack(mat);
        ItemMeta checksName = itemStack.getItemMeta();
        checksName.setDisplayName(name);
        itemStack.setItemMeta(checksName);
    }

    Element(int location, ItemStack itemStack) {
        this.location = location;
        this.itemStack = itemStack;
    }

    public int getLocation() {
        return location;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    abstract void doAction(Player p, Hawk hawk);
}
