package me.islandscout.hawk.utils.packets;

import net.minecraft.server.v1_7_R4.Packet;
import net.minecraft.server.v1_7_R4.PacketDataSerializer;
import net.minecraft.util.io.netty.buffer.Unpooled;

import java.io.IOException;

public class WrappedPacket7 extends WrappedPacket {

    public WrappedPacket7(Packet obj, PacketType type) {
        super(obj, type);
    }

    public Packet getPacket() {
        return (Packet)packet;
    }

    public void setByte(int index, int value) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(256));
        try {
            ((Packet) packet).b(serializer); //"b" method writes to PacketDataSerializer
            serializer.setByte(index, value);
            ((Packet) packet).a(serializer); //"a" method interprets PacketDataSerializer
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getBytes() {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(256));
        try {
            ((Packet) packet).b(serializer); //"b" method writes to PacketDataSerializer
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serializer.array();
    }
}
