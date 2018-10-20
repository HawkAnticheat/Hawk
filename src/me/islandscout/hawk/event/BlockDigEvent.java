/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.event;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.util.packet.WrappedPacket;
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

    public DigAction getDigAction() {
        return digAction;
    }

    public Block getBlock() {
        return block;
    }
}
