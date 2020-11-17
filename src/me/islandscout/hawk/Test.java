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

package me.islandscout.hawk;

import me.islandscout.hawk.event.bukkit.HawkAsyncPlayerVelocityChangeEvent;
import me.islandscout.hawk.util.packet.PacketAdapter;
import net.minecraft.server.v1_7_R4.*;
import net.minecraft.util.io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Test {

    private final Hawk hawk;

    public Test(Hawk hawk) {
        this.hawk = hawk;
    }

    /*public static void main(String[] args) {

        double rodMass = 1;
        double triangleMass = 1;
        double squareMass = 1;

        Vector a = new Vector(-0.25, -0.25, 0);
        Vector b = new Vector(0.25, -0.25, 0);
        Vector c = new Vector(-0.25, 0.25, 0);

        double x = (rodMass * a.getX() + triangleMass * b.getX() + squareMass * c.getX()) / (rodMass + triangleMass + squareMass);
        double y = (rodMass * a.getY() + triangleMass * b.getY() + squareMass * c.getY()) / (rodMass + triangleMass + squareMass);

        System.out.println(x + " " + y);

        System.out.println(x*x + y*y);


         save for physics
        double rodMass = 7;
        double triangleMass = 35;
        double squareMass = 5.45;

        Vector squareCM = new Vector(-3.5, 3.5, 0);
        Vector rodCM = new Vector(5.5, 7, 0);
        Vector triangleCM = new Vector(6.8284, 2.1716, 0);

        double x = (rodMass * rodCM.getX() + triangleMass * triangleCM.getX() + squareMass * squareCM.getX()) / (rodMass + triangleMass + squareMass);
        double y = (rodMass * rodCM.getY() + triangleMass * triangleCM.getY() + squareMass * squareCM.getY()) / (rodMass + triangleMass + squareMass);

        System.out.println(x + " " + y);





        int sub = (int) Math.sqrt(4);

        double[][] field = new double[sub + 1][sub + 1];

        double dx = 6D / sub;
        double dy = 8D / sub;

        for (int i = 0; i < field.length; i++) { //row
            for (int j = 0; j < field[i].length; j++) { //column
                field[i][j] = i * dx + j * dy;
                System.out.print(field[i][j] + " ");
            }
            System.out.println();
        }

    }*/

    private static void inputConverter() {
        File file = new File("resources/input.txt");

        try {
            Scanner in = new Scanner(file);

            while(in.hasNext()) {
                String input = in.nextLine();

                input = input.substring(17).replace(",", ".").replace(" ", ", ");

                input = "(" + input + ")";

                System.out.println(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(Hawk hawk) {
        PacketAdapter adapter = new PacketAdapter() {
            @Override
            public void run(Object packet, Player player) {
                //NOT COMPATIBLE WITH 1.8

                if(packet instanceof PacketPlayOutEntityMetadata) {

                    PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
                    ((PacketPlayOutEntityMetadata) packet).b(serializer);

                    int entityId = serializer.readInt();
                    if(entityId != player.getEntityId()) {
                        Entity nmsEntity = ((CraftWorld)player.getWorld()).getHandle().getEntity(entityId);

                        if(nmsEntity instanceof EntityLiving && !(nmsEntity instanceof EntityWolf)) {

                            List b = DataWatcher.b(serializer);

                            //SETTING VALUES
                            for (Object aB : b) {
                                WatchableObject wObj = ((WatchableObject) aB);
                                if (wObj.b() instanceof Float && (Float) wObj.b() != 0F)
                                    wObj.a(1F);
                            }

                            //PREPARE TO OVERWRITE PACKET
                            PacketDataSerializer pendingSerializer = new PacketDataSerializer(Unpooled.buffer(0));
                            pendingSerializer.writeInt(entityId);
                            DataWatcher.a(b, pendingSerializer, pendingSerializer.version);

                            //OVERWRITE
                            ((PacketPlayOutEntityMetadata) packet).a(pendingSerializer);
                        }
                    }
                }
            }
        };

        hawk.getPacketHandler().addPacketAdapterOutbound(adapter);
    }

    public void skid() {
        PacketAdapter adapter = new PacketAdapter() {
            @Override
            public void run(Object packet, Player player) {
                if(packet instanceof PacketPlayOutExplosion) {
                    PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
                    ((PacketPlayOutExplosion) packet).b(serializer);
                    serializer.readerIndex(serializer.writerIndex() - 12);
                    float x = serializer.readFloat();
                    float y = serializer.readFloat();
                    float z = serializer.readFloat();
                    Vector velocity = new Vector(x, y, z);
                    if(velocity.lengthSquared() == 0)
                        return;

                    Bukkit.getScheduler().runTask(hawk, new Runnable() {
                        @Override
                        public void run() {
                            Bukkit.getServer().getPluginManager().callEvent(new HawkAsyncPlayerVelocityChangeEvent(velocity, player, true));
                        }
                    });
                }
                if(packet instanceof PacketPlayOutEntityVelocity) {

                    PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer(0));
                    ((PacketPlayOutEntityVelocity) packet).b(serializer);
                    int id = serializer.readInt();
                    if(id != player.getEntityId()) {
                        return;
                    }

                    double x = serializer.readShort() / 8000D;
                    double y = serializer.readShort() / 8000D;
                    double z = serializer.readShort() / 8000D;
                    Vector velocity = new Vector(x, y, z);

                    Bukkit.getScheduler().runTask(hawk, new Runnable() {
                        @Override
                        public void run() {
                            Bukkit.getServer().getPluginManager().callEvent(new HawkAsyncPlayerVelocityChangeEvent(velocity, player, false));
                        }
                    });
                }
            }
        };

        hawk.getPacketHandler().addPacketAdapterOutbound(adapter);
    }
}
