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
import me.islandscout.hawk.wrap.packet.WrappedPacket;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class InteractEntityEvent extends Event {

    private final InteractAction interactAction;
    private final Entity entity;
    private final Vector intersectVector;

    public InteractEntityEvent(Player p, HawkPlayer pp, InteractAction action, Entity entity, Vector intersectVector, WrappedPacket packet) {
        super(p, pp, packet);
        interactAction = action;
        this.entity = entity;
        this.intersectVector = intersectVector;
    }

    @Override
    public boolean preProcess() {
        return !pp.isTeleporting();
    }

    @Override
    public void postProcess() {
        //We won't ignore if it's cancelled because otherwise that would set off
        //movement false flags regarding the hit slowdown mechanic. (Look at the
        //MoveEvent class for more information.)
        pp.updateLastEntityInteractTick();
        pp.addEntityToEntitiesInteractedInThisTick(entity);
        if(/*!isCancelled() && */getInteractAction() == InteractAction.ATTACK) {
            pp.updateItemUsedForAttack();
            if(getEntity() instanceof Player) {
                pp.updateLastAttackedPlayerTick();
                ItemStack heldItem = pp.getItemUsedForAttack();
                if(pp.isSprinting() || (heldItem != null && heldItem.getEnchantmentLevel(Enchantment.KNOCKBACK) > 0)) {
                    pp.updateHitSlowdownTick();
                }
            }
        }
    }

    public InteractAction getInteractAction() {
        return interactAction;
    }

    public Entity getEntity() {
        return entity;
    }

    public Vector getIntersectVector() {
        return intersectVector;
    }
}
