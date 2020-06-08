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

package me.islandscout.hawk.module;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.event.*;
import me.islandscout.hawk.module.listener.PacketListener;
import me.islandscout.hawk.module.listener.PacketListener7;
import me.islandscout.hawk.module.listener.PacketListener8;
import me.islandscout.hawk.util.ConfigHelper;
import me.islandscout.hawk.util.packet.PacketAdapter;
import me.islandscout.hawk.util.packet.PacketConverter7;
import me.islandscout.hawk.util.packet.PacketConverter8;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is mainly used to process packets that are intercepted from the Netty channels.
 * Remember, caution is advised when accessing the Bukkit API from the Netty thread.
 */
public class PacketHandler implements Listener {

    //Welcome to Minecraft protocol damnation.

    private final int serverVersion;
    private final Hawk hawk;
    private PacketListener packetListener;
    private List<HawkEventListener> hawkEventListeners;
    private final boolean async;

    public PacketHandler(Hawk hawk) {
        this.serverVersion = Hawk.getServerVersion();
        this.hawk = hawk;
        Event.setHawkReference(hawk);
        async = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "asyncChecking");
        if(async) {
            hawk.getLogger().warning("---");
            hawk.getLogger().warning("It appears that you have enabled ASYNCHRONOUS packet checking.");
            hawk.getLogger().warning("Although this will significantly improve network performance, it");
            hawk.getLogger().warning("will not prevent cheating. You will not receive any support for");
            hawk.getLogger().warning("any bypasses that you encounter. You have been warned.");
            hawk.getLogger().warning("---");
        }
        hawkEventListeners = new CopyOnWriteArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, hawk);
    }

    //These packets will be converted into Hawk Events for verification by checks
    public boolean processIn(Object packet, Player p) {
        HawkPlayer pp = hawk.getHawkPlayer(p);

        //ignore packets while player is no longer registered in Hawk
        if (!pp.isOnline())
            return false;

        Event event = convertPacketInboundToEvent(packet, pp);
        if(event == null)
            return true;

        if(!event.preProcess())
            return false;

        for(HawkEventListener eventListener : hawkEventListeners)
            eventListener.onEvent(event);

        hawk.getCheckManager().dispatchEvent(event);

        event.postProcess();

        return !event.isCancelled();
    }

    //These packets will be converted into Bukkit Events and will be broadcasted using Bukkit's event system
    public boolean processOut(Object packet, Player p) {
        org.bukkit.event.Event event = convertPacketOutboundToEvent(packet, p);
        if (event == null)
            return true;

        Bukkit.getServer().getPluginManager().callEvent(event);

        if(event instanceof Cancellable) {
            return !((Cancellable) event).isCancelled();
        }

        return true;
    }

    private Event convertPacketInboundToEvent(Object packet, HawkPlayer pp) {
        Event event;
        if (serverVersion == 8)
            event = PacketConverter8.packetInboundToEvent(packet, pp.getPlayer(), pp);
        else if (serverVersion == 7)
            event = PacketConverter7.packetInboundToEvent(packet, pp.getPlayer(), pp);
        else
            return null;
        return event;
    }

    private org.bukkit.event.Event convertPacketOutboundToEvent(Object packet, Player p) {
        org.bukkit.event.Event event;
        if (serverVersion == 8)
            event = PacketConverter8.packetOutboundToEvent(packet, p);
        else if (serverVersion == 7)
            event = PacketConverter7.packetOutboundToEvent(packet, p);
        else
            return null;
        return event;
    }

    public PacketListener getPacketListener() {
        return packetListener;
    }

    public void startListener() {
        if (serverVersion == 7) {
            packetListener = new PacketListener7(this, async);
            hawk.getLogger().info("Using NMS 1.7_R4 NIO for packet interception.");
        } else if (serverVersion == 8) {
            packetListener = new PacketListener8(this, async);
            hawk.getLogger().info("Using NMS 1.8_R3 NIO for packet interception.");
        } else {
            warnConsole(hawk);
            return;
        }
        packetListener.enable();
    }

    private void warnConsole(Hawk hawk) {
        hawk.getLogger().severe("!!!!!!!!!!");
        hawk.getLogger().severe("It appears that you are not running Hawk on a supported server version.");
        hawk.getLogger().severe("Hawk will NOT work. Please run Hawk on a 1.7_R4 or 1.8_R3 server. If you");
        hawk.getLogger().severe("are confident that you are running the correct version of the server,");
        hawk.getLogger().severe("please verify that the package \"net.minecraft.server.[VERSION]\" in your");
        hawk.getLogger().severe("Spigot JAR reflects one of the above specified versions.");
        hawk.getLogger().severe("!!!!!!!!!!");
        hawk.disable();
    }

    public void stopListener() {
        if(packetListener != null)
            packetListener.disable();
    }

    public void setupListenerForOnlinePlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            hawk.getHawkPlayer(p).setOnline(true);
            setupListenerForPlayer(p);
        }
    }

    private void setupListenerForPlayer(Player p) {
        if(packetListener != null)
            packetListener.addListener(p);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        setupListenerForPlayer(e.getPlayer());
    }

    public void addPacketAdapterInbound(PacketAdapter adapter) {
        packetListener.addAdapterInbound(adapter);
    }

    public void removePacketAdapterInbound(PacketAdapter adapter) {
        packetListener.removeAdapterInbound(adapter);
    }

    public void addPacketAdapterOutbound(PacketAdapter adapter) {
        packetListener.addAdapterOutbound(adapter);
    }

    public void removePacketAdapterOutbound(PacketAdapter adapter) {
        packetListener.removeAdapterOutbound(adapter);
    }

    public List<HawkEventListener> getHawkEventListeners() {
        return hawkEventListeners;
    }
}
