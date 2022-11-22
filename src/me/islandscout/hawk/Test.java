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
import me.islandscout.hawk.module.BanManager;
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

        System.out.println(magFieldLongWire(new Vector(0.2, 1, 0), new Vector(1, 0, 0), 1));

        double accel = -9.81;
        double vel = 0;
        double pos = 0;

        double TPS = 10;
        double timescale = 1;

        //Double integration using various numerical algorithms. I may use this in my game, for which I plan
        // to allow adjustable TPS.

        //Various integration algorithms have their applications, and some perform better than others in regards
        // to accuracy and complexity.
        //These algorithms approximate the area under the function by fitting thin simple shapes under the curve,
        // side-by-side. The approximated area is simply the sum of the areas of these simple shapes.
        //For the best results, set TPS as high as possible.
        //Notice how the trapezoidal algorithm is perfect for double integration of a constant, regardless of
        // the value of dt. This is because the trapezoidal approximation fits under a straight curve exactly; both
        // the constant and its first anti-derivative is a straight curve. The only error introduced is from arithmetic
        // rounding errors.

        //This is essentially a Right-Riemann sum
        //Notice how vel (integrand) is incremented before evaluating dPos
        /*for(double currTime = 0; currTime < 1; currTime += 1/TPS) {

            System.out.println("(" + currTime + ", " + vel + ")");

            double dt = timescale/TPS;

            double dVel = accel * dt; //The differential of velocity is acceleration times the differential of time
            vel += dVel;

            double dPos = vel * dt; //The differential of position is velocity times the differential of time
            pos += dPos;

        }*/

        //This is essentially a Left-Riemann sum
        //Notice how vel (integrand) is incremented after evaluating dPos
        // I didn't make the changes for integrating acceleration because it is constant; it wouldn't make a difference
        /*for(double currTime = 0; currTime < 1; currTime += 1/TPS) {

            System.out.println("(" + currTime + ", " + pos + ")");

            double dt = timescale/TPS;

            double dVel = accel * dt;

            double dPos = vel * dt;
            vel += dVel;

            pos += dPos;

        }*/

        //This is essentially a trapezoidal sum
        // I didn't make the changes for integrating acceleration because it is constant; it wouldn't make a difference
        // The trapezoidal sum works well for double integrals if the integrand is constant.
        /*for(double currTime = 0; currTime < 3; currTime += 1/TPS) {

            System.out.println("(" + currTime + ", " + pos + ")");

            double dt = timescale/TPS;

            double dVel = accel * dt;

            double velBefore = vel;
            vel += dVel;
            double velAfter = vel;

            double dPos = (velBefore + velAfter) * dt / 2;

            pos += dPos;

        }*/

        //I see a pattern here. If you want the n anti-derivative of an m degree polynomial, you need
        //an approximation polynomial of at least degree n+m-1. For example, degrees of some approximation methods:
        // - Riemann sums (rectangular): 0th degree, minimum method for integrating constants
        // - Trapezoidal: 1st degree, minimum method for integrating slopes
        // - Quadratic: 2nd degree, minimum method for integrating quadratics
        // - Cubic: 3rd degree, minimum method for integrating cubics
        //For instance, if you want the triple anti-derivative of a constant, you need at least a quadratic
        // approximation.
        //Also, for degree h of the approximation method, you need at least h points.

        //This is essentially a quadratic sum
        //incomplete
        /*{
            double velMinus2 = 0;
            for (double currTime = 0; currTime < 3; currTime += 1 / TPS) {

                System.out.println("(" + currTime + ", " + pos + ")");

                double dt = timescale / TPS;

                double dVel = (currTime - 1) * dt;

                double velMinus1 = vel;
                vel += dVel;

                double dPos = (velMinus1 + vel) / 2 * dt;

                pos += dPos;

                velMinus2 = velMinus1;

            }
        }*/



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
    private static Vector electrostaticForce(double chargeA, double chargeB, Vector displacement) { //displacement: position of B relative to A
        final double coulombConst = 8.9875517923 * Math.pow(10, 9);
        return displacement.clone().normalize().multiply((coulombConst * chargeA * chargeB)/displacement.lengthSquared());
    }

    //returns electrostatic field at point (in newtons per coulomb)
    private static Vector electrostaticField(double charge, Vector displacement) { //displacement from charge
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
