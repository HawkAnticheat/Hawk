package me.islandscout.hawk.listener.packets;

import me.islandscout.hawk.modules.PacketCore;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PacketListener7 {

    private PacketCore core;

    public PacketListener7(PacketCore core) {
        this.core = core;
    }

    public void start(Player p) {
        ChannelDuplexHandler channelDuplexHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object packet) throws Exception {

                //TODO: Get rid of this try/catch when you're done debugging
                try {
                    if(!core.process(packet, p)) return; //prevent packet from getting processed by Bukkit if a check fails
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                super.channelRead(context, packet);
            }

            @Override
            public void write(ChannelHandlerContext context, Object packet, ChannelPromise promise) throws Exception {


                super.write(context, packet, promise);
            }
        };
        try {
            Field channelField = ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer)p).getHandle().playerConnection.networkManager.getClass().getDeclaredField("m");
            channelField.setAccessible(true);
            net.minecraft.util.io.netty.channel.Channel channel = (net.minecraft.util.io.netty.channel.Channel) channelField.get(((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer)p).getHandle().playerConnection.networkManager);
            channelField.setAccessible(false);
            net.minecraft.util.io.netty.channel.ChannelPipeline pipeline = channel.pipeline();
            pipeline.addBefore("packet_handler", "hawk" + p.getName(), channelDuplexHandler);
        }
        catch (NoSuchFieldException | SecurityException | IllegalAccessException e){
            e.printStackTrace();
        }
    }

    public void stop() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                Field channelField = ((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer)p).getHandle().playerConnection.networkManager.getClass().getDeclaredField("m");
                channelField.setAccessible(true);
                net.minecraft.util.io.netty.channel.Channel channel = (net.minecraft.util.io.netty.channel.Channel) channelField.get(((org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer)p).getHandle().playerConnection.networkManager);
                channelField.setAccessible(false);
                channel.eventLoop().submit(() -> {
                    channel.pipeline().remove("hawk" + p.getName());
                    return null;
                });
            }
            catch (NoSuchFieldException | SecurityException | IllegalAccessException e){
                e.printStackTrace();
            }
        }
    }

}
