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

package me.islandscout.hawk.modules;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Scheduler {

    private final Hawk hawk;

    public Scheduler(Hawk hawk) {
        this.hawk = hawk;
    }

    public void startSchedulers() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(hawk, () -> {
            hawk.getViolationLogger().updateFile();
            hawk.getSql().postBuffer();
        }, 0L, 20L);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(hawk, () -> {
            for (HawkPlayer pp : hawk.getHawkPlayers()) {
                Player p = pp.getPlayer();
                int newPing = ServerUtils.getPing(p);
                pp.setPingJitter((short) (newPing - pp.getPing()));
                pp.setPing(ServerUtils.getPing(p));
            }
        }, 0L, 40L);
    }
}
