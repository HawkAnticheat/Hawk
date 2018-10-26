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

package me.islandscout.hawk.module;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.util.ClientBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;

public class PlayerManager implements Listener {

    private final Hawk hawk;

    public PlayerManager(Hawk hawk) {
        this.hawk = hawk;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        Player p = e.getPlayer();
        hawk.addProfile(p); //This line is necessary since it must get called BEFORE hawk listens to the player's packets
        hawk.getHawkPlayer(p).setOnline(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        hawk.removeProfile(e.getPlayer().getUniqueId());
        hawk.getCheckManager().removeData(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (!e.getTo().getWorld().equals(e.getFrom().getWorld())) {
            return;
        }
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        pp.setTeleporting(true);
        pp.setTeleportLoc(e.getTo());
        pp.setLocation(e.getTo());
        pp.setLastTeleportTime(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void worldChangeEvent(PlayerChangedWorldEvent e) {
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        pp.setTeleporting(true);
        pp.setTeleportLoc(e.getPlayer().getLocation());
        pp.setLocation(e.getPlayer().getLocation());
        pp.setLastTeleportTime(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        pp.setTeleporting(true);
        pp.setTeleportLoc(e.getRespawnLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent e) {
        HawkPlayer pp = hawk.getHawkPlayer(e.getPlayer());
        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, () -> {
            ClientBlock pBlockDel = null;
            for (ClientBlock pBlock : pp.getClientBlocks()) {
                Location a = pBlock.getLocation();
                Location b = e.getBlockPlaced().getLocation();
                if ((int) a.getX() == (int) b.getX() && (int) a.getY() == (int) b.getY() && (int) a.getZ() == (int) b.getZ()) {
                    pBlockDel = pBlock;
                    break;
                }
            }
            if (pBlockDel == null)
                return;
            pp.getClientBlocks().remove(pBlockDel);
        }, 1 + pp.getPing());
    }

}
