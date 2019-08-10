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

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.util.ClientBlock;
import me.islandscout.hawk.wrap.block.WrappedBlock7;
import me.islandscout.hawk.wrap.block.WrappedBlock8;
import me.islandscout.hawk.wrap.packet.WrappedPacket;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class BlockDigEvent extends Event {

    private final DigAction digAction;
    private final Block block;

    public BlockDigEvent(Player p, HawkPlayer pp, DigAction action, Block block, WrappedPacket packet) {
        super(p, pp, packet);
        digAction = action;
        this.block = block;
    }

    @Override
    public boolean preProcess() {
        if(pp.isTeleporting()) {
            revertChangeClientside();
            return false;
        }
        return true;
    }

    @Override
    public void postProcess() {
        if (!isCancelled() && getDigAction() == BlockDigEvent.DigAction.COMPLETE) {
            ClientBlock clientBlock = new ClientBlock(pp.getCurrentTick(), Material.AIR);
            pp.addClientBlock(getBlock().getLocation(), clientBlock);
        }
    }

    protected void revertChangeClientside() {
        if (Hawk.getServerVersion() == 7) {
            WrappedBlock7.getWrappedBlock(getBlock()).sendPacketToPlayer(pp.getPlayer());
        } else if (Hawk.getServerVersion() == 8) {
            WrappedBlock8.getWrappedBlock(getBlock()).sendPacketToPlayer(pp.getPlayer());
        }
    }

    public DigAction getDigAction() {
        return digAction;
    }

    public Block getBlock() {
        return block;
    }

    public enum DigAction {
        START,
        CANCEL,
        COMPLETE
    }
}
