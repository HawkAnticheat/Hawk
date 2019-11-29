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

package me.islandscout.hawk.module.listener;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.module.PacketHandler;
import me.islandscout.hawk.module.listener.tinyprotocol.TinyProtocol7;
import net.minecraft.util.io.netty.channel.Channel;
import org.bukkit.entity.Player;

public class PacketListener7 extends PacketListener {

    private TinyProtocol7 tP;

    public PacketListener7(PacketHandler packetHandler, boolean async, Hawk hawk) {
        super(packetHandler, async, hawk);
    }

    @Override
    public void enable() {
        tP = new TinyProtocol7(hawk) {

            @Override
            public Object onPacketInAsync(Player sender, Channel channel, Object packet) {

                if(sender == null) {
                    return super.onPacketInAsync(null, channel, packet);
                }

                if(!processIn(packet, sender))
                    return null;

                return super.onPacketInAsync(sender, channel, packet);
            }

            @Override
            public Object onPacketOutAsync(Player reciever, Channel channel, Object packet) {

                if(reciever == null) {
                    return super.onPacketInAsync(null, channel, packet);
                }

                if(!processOut(packet, reciever))
                    return null;

                return super.onPacketOutAsync(reciever, channel, packet);
            }

        };
        super.enable();
    }

    @Override
    public void disable() {
        if(tP != null) {
            tP.close();
            tP = null;
        }
        super.disable();
    }

    @Override
    public int getProtocolVersion(Player player) {
        return tP.getProtocolVersion(player);
    }
}
