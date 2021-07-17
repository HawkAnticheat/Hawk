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
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.packet.PacketAdapter;
import net.minecraft.server.v1_7_R4.*;
import net.minecraft.util.io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Test {

    private final Hawk hawk;

    public Test(Hawk hawk) {
        this.hawk = hawk;
    }

    public static void main(String[] args) {

    }

    //returns magnetic field around a long wire centered at the origin, given its direction and electric current
    //deriving this almost from scratch and then getting the homework problem right the first time really makes you happy
    //NOTE amps must be >= 0. If you need to switch directions, multiply dir by a negative number
    private static Vector magFieldLongWire(Vector eval, Vector dir, double amps) {
        double k = (eval.getX()*dir.getX() + eval.getY()*dir.getY() + eval.getZ()*dir.getZ()) /
                (dir.getX()*dir.getX() + dir.getY()*dir.getY() + dir.getZ()*dir.getZ());

        Vector closestPointOnWire = dir.clone().multiply(k);
        Vector perp = eval.clone().subtract(closestPointOnWire);
        Vector magDir = dir.clone().crossProduct(perp).multiply(amps).normalize();

        //long wire equation
        return magDir.multiply((1.2566370614359E-6 /*mu nought*/ * amps) / (2 * Math.PI * closestPointOnWire.distance(eval)));
    }

    private static double electrostaticForceAbs(double chargeA, double chargeB, double dist) {
        final double coulombConst = 8.9875517923 * Math.pow(10, 9);
        return Math.abs((coulombConst * chargeA * chargeB)/(dist * dist));
    }

    //returns electrostatic force on chargeB (in newtons)
    private static Vector electrostaticForce(double chargeA, double chargeB, Vector displacement) {
        final double coulombConst = 8.9875517923 * Math.pow(10, 9);
        return displacement.clone().normalize().multiply((coulombConst * chargeA * chargeB)/displacement.lengthSquared());
    }

    //returns electrostatic field at point (in newtons per coulomb)
    private static Vector electrostaticField(double charge, Vector displacement) {
        final double coulombConst = 8.9875517923 * Math.pow(10, 9);
        return displacement.clone().normalize().multiply((coulombConst * charge)/displacement.lengthSquared());
    }

    //returns electric potential at point (in volts)
    private static double electricPotential(double charge, double distance) {
        final double coulombConst = 8.9875517923E9;
        return (coulombConst * charge) / distance; //dont even ask why this is positive (negative times negative is positive) (because E = -dV/dX ?)
    }

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
