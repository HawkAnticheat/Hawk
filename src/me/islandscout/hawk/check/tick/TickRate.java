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

package me.islandscout.hawk.check.tick;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

/**
 * The Tickrate check enforces a limit to clients' tickrates.
 * Minecraft runs at 20 TPS, and some of the server logic for
 * each player is dependent on their PacketPlayInFlying rates
 * (eg: health regeneration, status effect, item consuming
 * duration). Hackers can exploit this by modifying the tickrate
 * of their client, thus creating cheats such as "instant"
 * regeneration or eating, movement speed, "lag-switching",
 * anti-potion, etc. Tickrate just alerts and prevents this.
 * It can detect at least a 3% increase in tickrate frequency
 * almost immediately under default settings.
 */
public class TickRate extends MovementCheck implements Listener {

    private final Map<UUID, Long> prevNanoTime;
    private final Map<UUID, Long> clockDrift;
    private final Map<UUID, Long> lastBigTeleportTime;
    private final boolean DEBUG;
    private final double THRESHOLD;
    private final long MAX_CATCHUP_TIME;
    private final double MIN_RATE;
    private final double MAX_RATE;
    private final boolean RUBBERBAND;
    private final boolean RESET_DRIFT_ON_FAIL;
    private final int WARM_UP;

    public TickRate() {
        super("tickrate", true, 10, 50, 0.995, 10000, "%player% failed tickrate. VL: %vl%, ping: %ping%, TPS: %tps%", null);
        prevNanoTime = new HashMap<>();
        clockDrift = new HashMap<>();
        lastBigTeleportTime = new HashMap<>();
        THRESHOLD = -(int) customSetting("clockDriftThresholdMillis", "", 50);
        MAX_CATCHUP_TIME = 1000000 * (int) customSetting("maxCatchupTimeMillis", "", 1000);
        DEBUG = (boolean) customSetting("debug", "", false);
        MIN_RATE = (50 - (50 / (double)customSetting("minRateMultiplier", "", 0.995))) / 1E-6;
        MAX_RATE = (50 - (50 / (double)customSetting("maxRateMultiplier", "", 1.005))) / 1E-6;
        RUBBERBAND = (boolean)customSetting("rubberband", "", true);
        RESET_DRIFT_ON_FAIL = (boolean)customSetting("resetDriftOnFail", "", false);
        WARM_UP = (int)customSetting("ignoreTicksAfterLongTeleport", "", 150) - 1;
    }

    @Override
    protected void check(MoveEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();

        long time = System.nanoTime();
        if (!prevNanoTime.containsKey(p.getUniqueId())) {
            prevNanoTime.put(p.getUniqueId(), time);
            return;
        }
        long timeElapsed = time - prevNanoTime.get(p.getUniqueId());
        prevNanoTime.put(p.getUniqueId(), time);

        if (event.hasTeleported() || pp.getCurrentTick() - lastBigTeleportTime.getOrDefault(p.getUniqueId(), 0L) < WARM_UP) {
            if(DEBUG)
                p.sendMessage(ChatColor.GRAY + "Tickrate check warming up. Please wait a moment...");
            clockDrift.put(p.getUniqueId(), 50000000L);
            return;
        }

        //if drift > 0, then player's clock is behind
        //if drift < 0, then player's clock is ahead
        //Debug messages have drift multiplied by -1 so,
        //drift > 0: clock is ahead
        //drift < 0: clock is behind
        long drift = clockDrift.getOrDefault(p.getUniqueId(), 0L);
        drift += timeElapsed - 50000000L;
        if (drift > MAX_CATCHUP_TIME)
            drift = MAX_CATCHUP_TIME;
        if (DEBUG) {
            double msOffset = drift * 1E-6;
            p.sendMessage((msOffset < 0 ? (msOffset < THRESHOLD ? ChatColor.RED : ChatColor.YELLOW) : ChatColor.BLUE) + "CLOCK DRIFT: " + MathPlus.round(-msOffset, 2) + "ms");
        }
        if (drift * 1E-6 < THRESHOLD) {
            if(RUBBERBAND && pp.getCurrentTick() - pp.getLastTeleportSendTick() > 20) //Don't rubberband so often. You're already cancelling a ton of moves.
                punishAndTryRubberband(pp, event, p.getLocation());
            else
                punish(pp, true, event);
            if(RESET_DRIFT_ON_FAIL)
                drift = 0;
        } else
            reward(pp);
        if (drift < 0)
            drift = (long) Math.min(0, drift + MAX_RATE);
        else
            drift = (long) Math.max(0, drift + MIN_RATE);
        clockDrift.put(p.getUniqueId(), drift);
    }

    @Override
    public void removeData(Player p) {
        prevNanoTime.remove(p.getUniqueId());
        clockDrift.remove(p.getUniqueId());
        lastBigTeleportTime.remove(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        lastBigTeleportTime.put(e.getPlayer().getUniqueId(), hawk.getHawkPlayer(e.getPlayer()).getCurrentTick());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent e) {
        Location loc = e.getTo();
        if(!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            lastBigTeleportTime.put(e.getPlayer().getUniqueId(), hawk.getHawkPlayer(e.getPlayer()).getCurrentTick());
        }
    }
}
