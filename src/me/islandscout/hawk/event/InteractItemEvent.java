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

package me.islandscout.hawk.event;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.util.packet.WrappedPacket;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;

public class InteractItemEvent extends Event {

    private final InteractItemEvent.Type type;
    private final ItemStack itemStack;

    public InteractItemEvent(Player p, HawkPlayer pp, ItemStack itemStack, InteractItemEvent.Type type, WrappedPacket wPacket) {
        super(p, pp, wPacket);
        this.type = type;
        this.itemStack = itemStack;
    }

    @Override
    public void postProcess() {
        if (!isCancelled()) {
            Material mat = getItemStack().getType();
            if(getType() == InteractItemEvent.Type.START_USE_ITEM) {
                if((mat.isEdible() && p.getFoodLevel() < 20 && p.getGameMode() != GameMode.CREATIVE) ||
                        //TODO: Fix IllegalArgumentException when consuming water bottles
                        (mat == Material.POTION && !Potion.fromItemStack(getItemStack()).isSplash())) {
                    pp.setConsumingItem(true);
                }
                if(EnchantmentTarget.WEAPON.includes(mat)) {
                    pp.setBlocking(true);
                }
                if(mat == Material.BOW && (p.getInventory().contains(Material.ARROW) || p.getGameMode() == GameMode.CREATIVE)) {
                    pp.setPullingBow(true);
                }
            }
            else if(getType() == InteractItemEvent.Type.RELEASE_USE_ITEM) {
                if((mat.isEdible() && p.getFoodLevel() < 20 && p.getGameMode() != GameMode.CREATIVE) ||
                        //TODO: Fix IllegalArgumentException when consuming water bottles
                        (mat == Material.POTION && !Potion.fromItemStack(getItemStack()).isSplash())) {
                    pp.setConsumingItem(false);
                }
                if(EnchantmentTarget.WEAPON.includes(mat)) {
                    pp.setBlocking(false);
                }
                if(mat == Material.BOW && (p.getInventory().contains(Material.ARROW) || p.getGameMode() == GameMode.CREATIVE)) {
                    pp.setPullingBow(false);
                }
            }
        }
    }

    public Type getType() {
        return type;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public enum Type {
        START_USE_ITEM,
        RELEASE_USE_ITEM,
        DROP_HELD_ITEM_STACK,
        DROP_HELD_ITEM
    }
}
