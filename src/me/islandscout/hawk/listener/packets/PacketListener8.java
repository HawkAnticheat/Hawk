package me.islandscout.hawk.listener.packets;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import me.islandscout.hawk.modules.PacketCore;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PacketListener8 {

    private PacketCore packetCore;

    public PacketListener8(PacketCore packetCore) {
        this.packetCore = packetCore;
    }

    public void start(Player p) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {

                //TODO: Get rid of this try/catch when you're done debugging
                try {
                    if(!packetCore.process(packet, p)) return; //prevent packet from getting processed by Bukkit if a check fails
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                super.channelRead(context, packet);
            }
        };
        ChannelPipeline pipeline;
        pipeline = ((CraftPlayer)p).getHandle().playerConnection.networkManager.channel.pipeline();
        if(pipeline != null) pipeline.addBefore("packet_handler", "hawk" + p.getName(), channelDuplexHandler);
    }

    public void stop() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Channel channel = ((CraftPlayer) p).getHandle().playerConnection.networkManager.channel;
            channel.eventLoop().submit(() -> {
                channel.pipeline().remove("hawk" + p.getName());
                return null;
            });
        }
    }

}
