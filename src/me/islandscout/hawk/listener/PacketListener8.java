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

package me.islandscout.hawk.listener;

import io.netty.channel.*;
import me.islandscout.hawk.module.PacketCore;
import me.islandscout.hawk.util.packet.PacketAdapter;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PacketListener8 extends PacketListener {

    public PacketListener8(PacketCore packetCore) {
        super(packetCore);
    }

    public void start(Player p) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {

                //TODO: Get rid of this try/catch when you're done debugging
                try {
                    if (!packetCore.processIn(packet, p))
                        return; //prevent packet from getting processed by Bukkit if a check fails
                } catch (Exception e) {
                    e.printStackTrace();
                }

                for(PacketAdapter adapter : adaptersInbound) {
                    adapter.run(packet, p);
                }

                super.channelRead(context, packet);
            }

            @Override
            public void write(ChannelHandlerContext context, Object packet, ChannelPromise promise) throws Exception {

                packetCore.processOut(packet, p);

                for(PacketAdapter adapter : adaptersOutbound) {
                    adapter.run(packet, p);
                }

                super.write(context, packet, promise);
            }
        };
        ChannelPipeline pipeline;
        pipeline = ((CraftPlayer) p).getHandle().playerConnection.networkManager.channel.pipeline();
        if (pipeline == null)
            return;
        String handlerName = "hawk" + p.getName();
        if (pipeline.get(handlerName) != null)
            pipeline.remove(handlerName);
        pipeline.addBefore("packet_handler", handlerName, channelDuplexHandler);
    }

    public void stop() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Channel channel = ((CraftPlayer) p).getHandle().playerConnection.networkManager.channel;

            ChannelPipeline pipeline = channel.pipeline();
            String handlerName = "hawk" + p.getName();
            if (pipeline.get(handlerName) != null)
                pipeline.remove(handlerName);
            //old. Should probably use this since it might have to do with concurrency safety
            /*channel.eventLoop().submit(() -> {
                channel.pipeline().remove("hawk" + p.getName());
                return null;
            });*/
        }
    }

}
