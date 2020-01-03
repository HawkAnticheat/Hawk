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

package me.islandscout.hawk.wrap.packet;

import net.minecraft.server.v1_7_R4.Packet;
import net.minecraft.server.v1_7_R4.PacketDataSerializer;
import net.minecraft.util.io.netty.buffer.Unpooled;

import java.io.IOException;

public class WrappedPacket7 extends WrappedPacket {

    public WrappedPacket7(Packet obj, PacketType type) {
        super(obj, type);
    }

    public Packet getPacket() {
        return (Packet) packet;
    }

    public void setByte(int index, int value) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
        try {
            ((Packet) packet).b(serializer); //"b" method writes to PacketDataSerializer (reads from packet)
            serializer.setByte(index, value);
            ((Packet) packet).a(serializer); //"a" method interprets PacketDataSerializer (writes to packet)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getBytes() {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
        try {
            ((Packet) packet).b(serializer); //"b" method writes to PacketDataSerializer (reads from packet)
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serializer.array();
    }

    public PacketDataSerializer readPacket() {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
        try {
            ((Packet) packet).b(serializer); //"b" method writes to PacketDataSerializer (reads from packet)
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serializer;
    }

    public void overwritePacket(Object packetDataSerializer) {
        PacketDataSerializer serializer = (PacketDataSerializer)packetDataSerializer;
        try {
            ((Packet) packet).a(serializer); //"a" method interprets PacketDataSerializer (writes to packet)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
