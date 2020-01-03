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

package me.islandscout.hawk.module.listener;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.module.PacketHandler;
import me.islandscout.hawk.util.Pair;
import me.islandscout.hawk.util.packet.PacketAdapter;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PacketListener {

    private boolean running;
    private final PacketHandler packetHandler;
    private List<PacketAdapter> adaptersInbound;
    private List<PacketAdapter> adaptersOutbound;

    private boolean async;
    private Thread hawkAsyncCheckThread;
    private List<Pair<Pair<Object, Player>, Boolean>> asyncQueuedPackets; //<<packet, player>, inbound>

    PacketListener(PacketHandler packetHandler, boolean async) {
        this.packetHandler = packetHandler;
        this.adaptersInbound = new ArrayList<>();
        this.adaptersOutbound = new ArrayList<>();
        this.async = async;
        if(async) {
            prepareAsync();
        }
    }

    public void enable() {
        running = true;
        if(async) {
            hawkAsyncCheckThread.start();
        }
    }

    public void disable() {
        running = false;
        if(async) {
            synchronized (hawkAsyncCheckThread) {
                hawkAsyncCheckThread.notify();
            }
        }
        removeAll();
    }

    abstract void add(Player p);

    abstract void removeAll();

    public void addListener(Player p) {
        add(p);
    }

    //returns false if not async and packet fails checks
    boolean processIn(Object packet, Player p) {
        if(!running)
            return true;
        if(async) {
            addToAsyncQueue(packet, p, true);
            return true;
        }
        return dispatchInbound(packet, p);
    }

    boolean processOut(Object packet, Player p) {
        if(!running)
            return true;
        if(async) {
            addToAsyncQueue(packet, p, false);
            return true;
        }
        return dispatchOutbound(packet, p);
    }

    private void addToAsyncQueue(Object packet, Player p, boolean inbound) {
        Pair<Object, Player> playerAndPacket = new Pair<>(packet, p);
        Pair<Pair<Object, Player>, Boolean> pair = new Pair<>(playerAndPacket, inbound);
        asyncQueuedPackets.add(pair);
        synchronized (hawkAsyncCheckThread) {
            hawkAsyncCheckThread.notify();
        }
    }

    private boolean dispatchInbound(Object packet, Player p) {
        try {
            for(PacketAdapter adapter : adaptersInbound) {
                adapter.run(packet, p);
            }

            if (!packetHandler.processIn(packet, p))
                return false;
        } catch (Exception e) {
            printPacketErrorInformation(packet, p);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean dispatchOutbound(Object packet, Player p) {
        try {
            for(PacketAdapter adapter : adaptersOutbound) {
                adapter.run(packet, p);
            }

            if (!packetHandler.processOut(packet, p))
                return false;
        } catch (Exception e) {
            printPacketErrorInformation(packet, p);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void prepareAsync() {
        asyncQueuedPackets = Collections.synchronizedList(new ArrayList<>());
        hawkAsyncCheckThread = new Thread(() -> {
            while(running) {

                //copy contents from queue to batch for processing
                List<Pair<Pair<Object, Player>, Boolean>> packetBatch = new ArrayList<>(asyncQueuedPackets);
                for (Pair<Pair<Object, Player>, Boolean> pair : packetBatch) {
                    Pair<Object, Player> packetAndPlayer = pair.getKey();

                    Object packet = packetAndPlayer.getKey();
                    Player player = packetAndPlayer.getValue();
                    boolean inbound = pair.getValue();

                    if(inbound) {
                        dispatchInbound(packet, player);
                    }
                    else {
                        dispatchOutbound(packet, player);
                    }

                }

                //Remove processed contents from queue.
                //Continue loop if queue isn't empty.
                //Otherwise, wait until notification from Netty thread.
                synchronized (asyncQueuedPackets) {
                    asyncQueuedPackets.subList(0, packetBatch.size()).clear();
                    if(asyncQueuedPackets.size() > 0)
                        continue;
                }
                try {
                    synchronized (hawkAsyncCheckThread) {
                        hawkAsyncCheckThread.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        hawkAsyncCheckThread.setName("Hawk Async Check Thread");
    }

    private void printPacketErrorInformation(Object packet, Player p) {
        String packetName = packet.getClass().getSimpleName();
        String pName = p == null ? "(NULL)" : "\"" + p.getName() + "\"";
        System.err.println("Hawk (version " + Hawk.BUILD_NAME + ") has encountered an error while processing " + packetName + " for player " + pName);
        System.err.println("The packet was dropped to prevent possible exploitation.");
        System.err.println("This " + packetName + "'s fields:");

        Class current = packet.getClass();
        while(current != null){
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = null;
                try {
                    value = field.get(packet);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
                if (value != null) {
                    System.err.println("    " + field.getType() + ", " + field.getName() + " = " + value);
                }
            }
            current = current.getSuperclass();
        }

        System.err.println("The stacktrace leading to the error is printed below:");
    }

    public void addAdapterInbound(PacketAdapter runnable) {
        adaptersInbound.add(runnable);
    }

    public void removeAdapterInbound(PacketAdapter runnable) {
        adaptersInbound.remove(runnable);
    }

    public void addAdapterOutbound(PacketAdapter runnable) {
        adaptersOutbound.add(runnable);
    }

    public void removeAdapterOutbound(PacketAdapter runnable) {
        adaptersOutbound.remove(runnable);
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isRunning() {
        return running;
    }
}
