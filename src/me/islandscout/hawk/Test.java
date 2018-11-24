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

package me.islandscout.hawk;

import me.islandscout.hawk.util.packet.PacketAdapter;
import net.minecraft.server.v1_7_R4.*;
import net.minecraft.util.io.netty.buffer.Unpooled;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class Test {

    private final Hawk hawk;

    public Test(Hawk hawk) {
        this.hawk = hawk;
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

        hawk.getPacketCore().addAdapterOutbound(adapter);
    }

    public void skid() {
        PacketAdapter adapter = new PacketAdapter() {
            @Override
            public void run(Object packet, Player player) {
                if(packet instanceof PacketPlayOutExplosion) {

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

                    //Debug.broadcastMessage(ChatColor.GREEN + velocity.toString());

                    Vector expected = velocity.clone().add(hawk.getHawkPlayer(player).getVelocity().clone().multiply(0.1));
                    //Debug.broadcastMessage(ChatColor.YELLOW + expected.toString());
                }
            }
        };

        hawk.getPacketCore().addAdapterOutbound(adapter);
    }
}
