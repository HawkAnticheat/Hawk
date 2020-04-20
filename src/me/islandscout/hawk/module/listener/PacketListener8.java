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

import io.netty.channel.*;
import me.islandscout.hawk.module.PacketHandler;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PacketListener8 extends PacketListener {

    public PacketListener8(PacketHandler packetHandler, boolean async) {
        super(packetHandler, async);
    }

    public void add(Player p) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {

                if (!processIn(packet, p))
                    return;

                super.channelRead(context, packet);
            }

            @Override
            public void write(ChannelHandlerContext context, Object packet, ChannelPromise promise) throws Exception {

                if (!processOut(packet, p))
                    return;

                super.write(context, packet, promise);
            }
        };
        Channel channel = ((CraftPlayer) p).getHandle().playerConnection.networkManager.channel;
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline == null)
            return;
        String handlerName = "hawk_packet_processor";
        channel.eventLoop().submit(() -> {
            if (pipeline.get(handlerName) != null)
                pipeline.remove(handlerName);
            pipeline.addBefore("packet_handler", handlerName, channelDuplexHandler);
            return null;
        });
    }

    public void removeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Channel channel = ((CraftPlayer) p).getHandle().playerConnection.networkManager.channel;
            ChannelPipeline pipeline = channel.pipeline();
            String handlerName = "hawk_packet_processor";
            channel.eventLoop().submit(() -> {
                if (pipeline.get(handlerName) != null)
                    pipeline.remove(handlerName);
                return null;
            });
        }
    }
}
