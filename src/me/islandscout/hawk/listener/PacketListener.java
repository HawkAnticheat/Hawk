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

package me.islandscout.hawk.listener;

import me.islandscout.hawk.module.PacketCore;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.Pair;
import me.islandscout.hawk.util.packet.PacketAdapter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PacketListener {

    private boolean running;
    private final PacketCore packetCore;
    private List<PacketAdapter> adaptersInbound;
    private List<PacketAdapter> adaptersOutbound;

    private boolean async;
    private Thread hawkAsyncCheckThread;
    private List<Pair<Pair<Object, Player>, Boolean>> asyncBufferedPackets; //<<packet, player>, inbound>

    PacketListener(PacketCore packetCore, boolean async) {
        this.packetCore = packetCore;
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

    void processOut(Object packet, Player p) {
        if(!running)
            return;
        if(async) {
            addToAsyncQueue(packet, p, false);
            return;
        }
        dispatchOutbound(packet, p);
    }

    private void addToAsyncQueue(Object packet, Player p, boolean inbound) {
        Pair<Object, Player> playerAndPacket = new Pair<>(packet, p);
        Pair<Pair<Object, Player>, Boolean> pair = new Pair<>(playerAndPacket, inbound);
        asyncBufferedPackets.add(pair);
        synchronized (hawkAsyncCheckThread) {
            hawkAsyncCheckThread.notify();
        }
    }

    private boolean dispatchInbound(Object packet, Player p) {
        for(PacketAdapter adapter : adaptersInbound) {
            adapter.run(packet, p);
        }

        try {
            if (!packetCore.processIn(packet, p))
                return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void dispatchOutbound(Object packet, Player p) {
        for(PacketAdapter adapter : adaptersOutbound) {
            adapter.run(packet, p);
        }

        packetCore.processOut(packet, p);
    }

    private void prepareAsync() {
        asyncBufferedPackets = Collections.synchronizedList(new ArrayList<>());
        hawkAsyncCheckThread = new Thread(() -> {
            while(running) {

                //copy contents from synchronized list for processing
                List<Pair<Pair<Object, Player>, Boolean>> packetsSnapshot = new ArrayList<>(asyncBufferedPackets);
                for (Pair<Pair<Object, Player>, Boolean> pair : packetsSnapshot) {
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

                //clear contents already processed from synchronized list
                synchronized (asyncBufferedPackets) {
                    asyncBufferedPackets.subList(0, packetsSnapshot.size()).clear();
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
