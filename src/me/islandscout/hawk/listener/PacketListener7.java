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

package me.islandscout.hawk.listener;

import me.islandscout.hawk.module.PacketCore;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PacketListener7 extends PacketListener {

    public PacketListener7(PacketCore packetCore, boolean async) {
        super(packetCore, async);
    }

    public void add(Player p) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {

                if(!processIn(packet, p))
                    return;

                super.channelRead(context, packet);
            }

            @Override
            public void write(ChannelHandlerContext context, Object packet, ChannelPromise promise) throws Exception {

                processOut(packet, p);

                super.write(context, packet, promise);
            }
        };
        try {
            Field channelField = ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer) p).getHandle().playerConnection.networkManager.getClass().getDeclaredField("m");
            channelField.setAccessible(true);
            net.minecraft.util.io.netty.channel.Channel channel = (net.minecraft.util.io.netty.channel.Channel) channelField.get(((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer) p).getHandle().playerConnection.networkManager);
            channelField.setAccessible(false);
            net.minecraft.util.io.netty.channel.ChannelPipeline pipeline = channel.pipeline();
            if (pipeline == null)
                return;
            String handlerName = "hawk_packet_processor";
            if (pipeline.get(handlerName) != null)
                pipeline.remove(handlerName);
            pipeline.addBefore("packet_handler", handlerName, channelDuplexHandler);
        } catch (ReflectiveOperationException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public void removeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                Field channelField = ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer) p).getHandle().playerConnection.networkManager.getClass().getDeclaredField("m");
                channelField.setAccessible(true);
                net.minecraft.util.io.netty.channel.Channel channel = (net.minecraft.util.io.netty.channel.Channel) channelField.get(((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer) p).getHandle().playerConnection.networkManager);
                channelField.setAccessible(false);
                net.minecraft.util.io.netty.channel.ChannelPipeline pipeline = channel.pipeline();
                String handlerName = "hawk_packet_processor";
                if (pipeline.get(handlerName) != null)
                    pipeline.remove(handlerName);
                //old. Should probably use this since it might have to do with concurrency safety
                /*channel.eventLoop().submit(() -> {
                    channel.pipeline().remove("hawk" + p.getName());
                    return null;
                });*/
            } catch (ReflectiveOperationException | SecurityException e) {
                e.printStackTrace();
            }
        }
    }

}
