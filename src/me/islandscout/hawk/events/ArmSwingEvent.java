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

package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.packets.WrappedPacket;
import org.bukkit.entity.Player;

public class ArmSwingEvent extends Event {

    private final int type;

    public ArmSwingEvent(Player p, HawkPlayer pp, int type, WrappedPacket packet) {
        super(p, pp, packet);
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
