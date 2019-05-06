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
import me.islandscout.hawk.listener.PacketListener;
import me.islandscout.hawk.listener.PacketListener7;
import me.islandscout.hawk.listener.PacketListener8;
import me.islandscout.hawk.util.ClientBlock;
import me.islandscout.hawk.util.ConfigHelper;
import me.islandscout.hawk.util.packet.PacketAdapter;
import me.islandscout.hawk.util.packet.PacketConverter7;
import me.islandscout.hawk.util.packet.PacketConverter8;
import org.bukkit.*;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.Potion;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is mainly used to process packets that are intercepted from the Netty channels.
 * Remember, caution is advised when accessing the Bukkit API from the Netty thread.
 */
public class PacketCore implements Listener {

    //Welcome to TCP damnation.

    private final int serverVersion;
    private final Hawk hawk;
    private PacketListener packetListener;
    private List<HawkEventListener> hawkEventListeners;
    private final boolean async;

    public PacketCore(Hawk hawk) {
        this.serverVersion = Hawk.getServerVersion();
        this.hawk = hawk;
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
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean processIn(Object packet, Player p) {
        HawkPlayer pp = hawk.getHawkPlayer(p);

        //ignore packets while player is no longer registered in Hawk
        if (!pp.isOnline())
            return false;

        Event event;
        if (serverVersion == 8)
            event = PacketConverter8.packetInboundToEvent(packet, p, pp);
        else if (serverVersion == 7)
            event = PacketConverter7.packetInboundToEvent(packet, p, pp);
        else
            return true;
        if (event == null)
            return true;

        if (event instanceof MoveEvent) {
            MoveEvent posEvent = (MoveEvent) event;
            posEvent.setTeleported(false);
            pp.incrementCurrentTick();
            if(posEvent.isUpdatePos())
                pp.setHasMoved();
            //handle teleports
            if (pp.isTeleporting()) {
                Location tpLoc = pp.getTeleportLoc();
                //accepted teleport
                if (tpLoc.getWorld().equals(posEvent.getTo().getWorld()) && posEvent.getTo().distanceSquared(tpLoc) < 0.001) {
                    posEvent.setFrom(tpLoc);
                    pp.setTeleporting(false);
                    posEvent.setTeleported(true);
                } else if(!pp.getPlayer().isSleeping()){
                    //Help guide the confused client back to the tp location
                    if (pp.getCurrentTick() - pp.getLastTeleportTime() > 20) {
                        pp.teleportPlayer(tpLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    }
                    return false;
                }
            }
            //handle illegal move or discrepancy
            else if (posEvent.getFrom().getWorld().equals(posEvent.getTo().getWorld()) && posEvent.getTo().distanceSquared(posEvent.getFrom()) > 64) {
                posEvent.cancelAndSetBack(p.getLocation());
                return false;
            }
        }

        for(HawkEventListener eventListener : hawkEventListeners)
            eventListener.onEvent(event);

        hawk.getCheckManager().dispatchEvent(event);

        //ignore packets if player is no longer registered in Hawk
        if (!pp.isOnline())
            return false;

        //update HawkPlayer
        //TODO: Jeez, this oughta bog down performance. Replace these if statements w/ else-if statements
        if (event instanceof InteractItemEvent && !event.isCancelled()) {
            InteractItemEvent itemEvent = (InteractItemEvent) event;
            Material mat = itemEvent.getItemStack().getType();
            if(itemEvent.getType() == InteractItemEvent.Type.START_USE_ITEM) {
                if((mat.isEdible() && p.getFoodLevel() < 20 && p.getGameMode() != GameMode.CREATIVE) ||
                        //TODO: Fix IllegalArgumentException when consuming water bottles
                        (mat == Material.POTION && !Potion.fromItemStack(itemEvent.getItemStack()).isSplash())) {
                    pp.setConsumingItem(true);
                }
                if(EnchantmentTarget.WEAPON.includes(mat)) {
                    pp.setBlocking(true);
                }
                if(mat == Material.BOW && (p.getInventory().contains(Material.ARROW) || p.getGameMode() == GameMode.CREATIVE)) {
                    pp.setPullingBow(true);
                }
            }
            else if(itemEvent.getType() == InteractItemEvent.Type.RELEASE_USE_ITEM) {
                if((mat.isEdible() && p.getFoodLevel() < 20 && p.getGameMode() != GameMode.CREATIVE) ||
                        //TODO: Fix IllegalArgumentException when consuming water bottles
                        (mat == Material.POTION && !Potion.fromItemStack(itemEvent.getItemStack()).isSplash())) {
                    pp.setConsumingItem(false);
                }
                if(EnchantmentTarget.WEAPON.includes(mat)) {
                    pp.setBlocking(false);
                }
                if(mat == Material.BOW && (p.getInventory().contains(Material.ARROW) || p.getGameMode() == GameMode.CREATIVE)) {
                    pp.setPullingBow(false);
                }
            }
        }
        if (event instanceof ItemSwitchEvent && !event.isCancelled()) {
            pp.setHeldItemSlot(((ItemSwitchEvent) event).getSlotIndex());
            pp.setConsumingItem(false);
            pp.setBlocking(false);
            pp.setPullingBow(false);
        }
        if (event instanceof InteractWorldEvent && ((InteractWorldEvent) event).getInteractionType() == InteractWorldEvent.InteractionType.PLACE_BLOCK) {
            InteractWorldEvent bPlaceEvent = (InteractWorldEvent) event;
            if (!bPlaceEvent.isCancelled()) {
                ClientBlock clientBlock = new ClientBlock(bPlaceEvent.getPlacedBlockLocation(), pp.getCurrentTick(), bPlaceEvent.getPlacedBlockMaterial());
                pp.addClientBlock(clientBlock);
            }
        }
        if (event instanceof BlockDigEvent && ((BlockDigEvent) event).getDigAction() == BlockDigEvent.DigAction.COMPLETE) {
            BlockDigEvent dEvent = (BlockDigEvent) event;
            if(!dEvent.isCancelled()) {
                ClientBlock clientBlock = new ClientBlock(dEvent.getBlock().getLocation(), pp.getCurrentTick(), Material.AIR);
                pp.addClientBlock(clientBlock);
            }
        }
        if (event instanceof MoveEvent) {
            pp.setLastMoveTime(System.currentTimeMillis());
            MoveEvent mEvent = (MoveEvent) event;
            if(event.isCancelled()) {
                //handle rubberband if applicable
                if(((MoveEvent) event).getCancelLocation() != null) {
                    ((MoveEvent) event).setTo(((MoveEvent) event).getCancelLocation());
                    pp.setTeleporting(true);
                    pp.teleportPlayer(((MoveEvent) event).getCancelLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                } else {
                    //2/17/19: well, technically this shouldn't be allowed. I did
                    //this so at least some other check such as speed can rubberband
                    //if tickrate fails. If tickrate rubberbands, that'll just spam
                    //more packets. And if someone fails tickrate then they'll just spam
                    //speed, especially if speed isn't set to rubberband.

                    //((MoveEvent) event).setTo(((MoveEvent) event).getFrom());
                }
            } else {
                //handle item consumption
                if(pp.getCurrentTick() - pp.getItemUseTick() > 31 && pp.isConsumingItem()) {
                    pp.setConsumingItem(false);
                }

                //handle swimming
                pp.setInLiquid(mEvent.isInLiquid());
                if(pp.getCurrentTick() < 2)
                    pp.setSwimming(pp.isInLiquid());
                long ticksSinceSwimToggle = pp.getCurrentTick() - pp.getLastInLiquidToggleTick();
                pp.setSwimming(!pp.isFlyingClientside() && ((pp.isInLiquid() && ticksSinceSwimToggle > 0) || (!pp.isInLiquid() && ticksSinceSwimToggle < 1)));

                Location to = mEvent.getTo();
                Location from = mEvent.getFrom();
                pp.setVelocity(new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ()));
                pp.setDeltaYaw(to.getYaw() - from.getYaw());
                pp.setDeltaPitch(to.getPitch() - from.getPitch());
                pp.setLocation(to);
                pp.updateFallDistance(to);
                pp.updateTotalAscensionSinceGround(from.getY(), to.getY());
                pp.setOnGround(mEvent.isOnGround());
                pp.setOnGroundReally(mEvent.isOnGroundReally());
                pp.getBoxSidesTouchingBlocks().clear();
                pp.getBoxSidesTouchingBlocks().addAll(mEvent.getBoxSidesTouchingBlocks());
                pp.setWaterFlowForce(mEvent.getWaterFlowForce());
            }

        }
        if (event instanceof AbilitiesEvent && !event.isCancelled() && ((AbilitiesEvent) event).isFlying()) {
            pp.setFlyPendingTime(System.currentTimeMillis());
        }
        if (event instanceof PlayerActionEvent && !event.isCancelled()) {
            PlayerActionEvent.PlayerAction action = ((PlayerActionEvent) event).getAction();
            switch (action) {
                case SNEAK_START:
                    pp.setSneaking(true);
                    break;
                case SNEAK_STOP:
                    pp.setSneaking(false);
                    break;
                case SPRINT_START:
                    pp.setSprinting(true);
                    break;
                case SPRINT_STOP:
                    pp.setSprinting(false);
                    break;
            }
        }
        if(event instanceof InteractEntityEvent && !event.isCancelled()) {
            InteractEntityEvent interactEvent = (InteractEntityEvent) event;
            if(interactEvent.getInteractAction() == InteractAction.ATTACK) {
                pp.updateItemUsedForAttack();
                if(interactEvent.getEntity() instanceof Player) {
                    pp.updateLastAttackedPlayerTick();
                }
            }
        }

        return !event.isCancelled();
    }

    //These packets will be converted into Bukkit Events and will be broadcasted on the main thread
    public void processOut(Object packet, Player p) {

        org.bukkit.event.Event event;
        if (serverVersion == 8)
            event = PacketConverter8.packetOutboundToEvent(packet, p);
        else if (serverVersion == 7)
            event = PacketConverter7.packetOutboundToEvent(packet, p);
        else
            return;
        if (event == null)
            return;

        Bukkit.getServer().getPluginManager().callEvent(event);

    }

    public PacketListener getPacketListener() {
        return packetListener;
    }

    public void startListener() {
        try {
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
        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
            hawk.disable();
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
        hawk.getLogger().severe("Spigot JAR contains one of the above specified versions.");
        hawk.getLogger().severe("!!!!!!!!!!");
        hawk.disable();
    }

    public void killListener() {
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
