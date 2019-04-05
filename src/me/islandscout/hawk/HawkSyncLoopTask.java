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

import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.entity.Player;

public class HawkSyncLoopTask implements Runnable {

    private long currentTick;
    private final Hawk hawk;

    HawkSyncLoopTask(Hawk hawk) {
        this.hawk = hawk;
    }

    /**
     * This is what runs every server tick
     */
    @Override
    public void run() {

        if(currentTick % 20 == 0) {
            hawk.getViolationLogger().updateFile();
            hawk.getSQLModule().tick();
        }

        for(HawkPlayer pp : hawk.getHawkPlayers()) {
            if(currentTick % 40 == 0) {
                Player p = pp.getPlayer();
                int newPing = ServerUtils.getPing(p);
                pp.setPingJitter((short) (newPing - pp.getPing()));
                pp.setPing(ServerUtils.getPing(p));
            }

            //TODO: add entities to a list called nearbyEntities in HawkPlayer
        }

        currentTick++;
    }
}
