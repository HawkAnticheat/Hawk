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
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import me.islandscout.hawk.wrap.entity.human.WrappedEntityHuman;
import me.islandscout.hawk.wrap.packet.WrappedPacket;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;

public class InteractItemEvent extends Event {

    private final Action action;
    private final ItemStack itemStack; //cannot be spoofed; determined server-side based on the slot the client says they're on

    public InteractItemEvent(Player p, HawkPlayer pp, ItemStack itemStack, Action action, WrappedPacket wPacket) {
        super(p, pp, wPacket);
        this.action = action;
        this.itemStack = itemStack;
    }

    @Override
    public void postProcess() {
        Material mat = getItemStack().getType();
        boolean gapple = mat == Material.GOLDEN_APPLE;
        if(action == Action.START_USE_ITEM) {
            if((mat.isEdible() && (p.getFoodLevel() < 20 || gapple) && p.getGameMode() != GameMode.CREATIVE) ||
                    (mat == Material.POTION && getItemStack().getDurability() == 0) || //water bottles
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
        else if(action == Action.RELEASE_USE_ITEM || action == Action.DROP_HELD_ITEM || action == Action.DROP_HELD_ITEM_STACK) {
            pp.setConsumingItem(false);
            pp.setBlocking(false);
            pp.setPullingBow(false);
        }
    }

    @Override
    public void resync() {
        if(Event.allowedToResync(pp)) {
            WrappedEntityHuman weh = (WrappedEntityHuman) WrappedEntity.getWrappedEntity(p);
            boolean usingItem = weh.usingItem();
            //TODO
        }
    }

    public Action getAction() {
        return action;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public enum Action {
        START_USE_ITEM,
        RELEASE_USE_ITEM,
        DROP_HELD_ITEM_STACK,
        DROP_HELD_ITEM
    }
}
