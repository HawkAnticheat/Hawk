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
import me.islandscout.hawk.event.bukkit.HawkAsyncPlayerAbilitiesEvent;
import me.islandscout.hawk.event.bukkit.HawkAsyncPlayerMetadataEvent;
import me.islandscout.hawk.event.bukkit.HawkAsyncPlayerTeleportEvent;
import me.islandscout.hawk.event.bukkit.HawkAsyncPlayerVelocityChangeEvent;
import me.islandscout.hawk.util.Pair;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.wrap.WrappedWatchableObject;
import me.islandscout.hawk.wrap.entity.MetaData;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.util.Vector;

import java.util.List;

public class PlayerEventListener implements Listener {

    private final Hawk hawk;

    public PlayerEventListener(Hawk hawk) {
        this.hawk = hawk;

        hawk.getHawkSyncTaskScheduler().addRepeatingTask(new Runnable() {
            @Override
            public void run() {
                for(HawkPlayer pp : hawk.getHawkPlayers()) {
                    Player p = pp.getPlayer();
                    pp.setPing(ServerUtils.getPing(p));
                }
            }
        }, 40);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent e) {
        Player p = e.getPlayer();
        hawk.addProfile(p); //This line is necessary since it must get called BEFORE hawk listens to the player's packets
        hawk.getHawkPlayer(p).setOnline(true);
    }

    //Set protocol version
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        hawk.getHawkPlayer(e.getPlayer()).setClientVersion(ServerUtils.getProtocolVersion(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent e) {
        hawk.removeProfile(e.getPlayer().getUniqueId());
        hawk.getCheckManager().removeData(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(HawkAsyncPlayerVelocityChangeEvent e) {
        if(e.isAdditive())
            return;
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        Vector vector = e.getVelocity();

        List<Pair<Vector, Long>> pendingVelocities = pp.getPendingVelocities();
        pendingVelocities.add(new Pair<>(vector, System.currentTimeMillis()));
        if(pendingVelocities.size() > 20)
            pendingVelocities.remove(0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpenServerSide(InventoryOpenEvent e) {
        HumanEntity hE = e.getPlayer();
        if(!(hE instanceof Player))
            return;

        //Fixes issues regarding the client not releasing item usage when a server inventory is opened.
        //Consumables may not have this issue.
        HawkPlayer pp = hawk.getHawkPlayer((Player) hE);
        pp.sendSimulatedAction(new Runnable() {
            @Override
            public void run() {
                pp.setBlocking(false);
                pp.setPullingBow(false);
                pp.setInventoryOpen((byte)2);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryCloseServerSide(InventoryCloseEvent e) {
        HumanEntity hE = e.getPlayer();
        if(!(hE instanceof Player))
            return;

        HawkPlayer pp = hawk.getHawkPlayer((Player) hE);
        pp.sendSimulatedAction(new Runnable() {
            @Override
            public void run() {
                pp.setInventoryOpen((byte)0);
            }
        });
    }

    @EventHandler
    public void sendMetadataEvent(HawkAsyncPlayerMetadataEvent e) {
        List<WrappedWatchableObject> objects = e.getMetaData();
        for(WrappedWatchableObject object : objects) {
            if(object.getIndex() == 0) {
                Player p = e.getPlayer();
                HawkPlayer pp = hawk.getHawkPlayer(p);
                byte status = (byte)object.getObject();

                //bitmask
                if((status & 16) == 16) {
                    pp.addMetaDataUpdate(new MetaData(MetaData.Type.USE_ITEM, true));
                } else {
                    pp.addMetaDataUpdate(new MetaData(MetaData.Type.USE_ITEM, false));
                }
                if((status & 8) == 8) {
                    pp.addMetaDataUpdate(new MetaData(MetaData.Type.SPRINT, true));
                } else {
                    pp.addMetaDataUpdate(new MetaData(MetaData.Type.SPRINT, false));
                }
                /*if((status & 2) == 2) {
                    pp.addMetaDataUpdate(new MetaData(MetaData.Type.SNEAK, true));
                } else {
                    pp.addMetaDataUpdate(new MetaData(MetaData.Type.SNEAK, false));
                }*/

                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void vehicleEnter(VehicleEnterEvent e) {
        if(e.getEntered() instanceof Player) {
            HawkPlayer pp = hawk.getHawkPlayer((Player)e.getEntered());
            pp.sendSimulatedAction(new Runnable() {
                @Override
                public void run() {
                    pp.setInVehicle(true);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void vehicleExit(VehicleExitEvent e) {
        if(e.getExited() instanceof Player) {
            HawkPlayer pp = hawk.getHawkPlayer((Player)e.getExited());
            pp.sendSimulatedAction(new Runnable() {
                @Override
                public void run() {
                    pp.setInVehicle(false);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void abilitiesServerSide(HawkAsyncPlayerAbilitiesEvent e) {
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        pp.sendSimulatedAction(new Runnable() {
            @Override
            public void run() {
                pp.setAllowedToFly(e.isAllowedToFly());
                pp.setFlying(e.isFlying());
                pp.setInCreative(e.isCreativeMode());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleport(HawkAsyncPlayerTeleportEvent e) {
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        pp.setTeleporting(true);
        pp.setTeleportLoc(e.getPlayer().getLocation());
        pp.addPendingTeleport(new Location(pp.getWorld(), e.getX(), e.getY(), e.getZ(), e.getYaw() % 360F, e.getPitch() % 360F));
    }
}
