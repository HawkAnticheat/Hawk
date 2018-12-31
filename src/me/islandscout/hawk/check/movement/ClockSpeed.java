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

package me.islandscout.hawk.check.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.ConfigHelper;
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import java.util.*;

public class ClockSpeed extends MovementCheck implements Listener {

    private final Map<UUID, Long> prevNanoTime;
    private final Map<UUID, Long> clockDrift;
    private final boolean DEBUG;
    private final double THRESHOLD;
    private final long MAX_CATCHUP_TIME;
    private final double CALIBRATE_SLOWER;
    private final double CALIBRATE_FASTER;
    private final boolean RUBBERBAND;
    private final boolean RESET_DRIFT_ON_FAIL;
    private final boolean DENY_ACTIONS;
    private final int WARM_UP;

    public ClockSpeed() {
        super("clockspeed", true, 10, 10, 0.995, 10000, "%player% failed clockspeed. VL: %vl%, ping: %ping%, TPS: %tps%", null);
        prevNanoTime = new HashMap<>();
        clockDrift = new HashMap<>();
        THRESHOLD = -ConfigHelper.getOrSetDefault(30, hawk.getConfig(), "checks.clockspeed.clockDriftThreshold");
        MAX_CATCHUP_TIME = 1000000 * ConfigHelper.getOrSetDefault(500, hawk.getConfig(), "checks.clockspeed.maxCatchupTime");
        DEBUG = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.clockspeed.debug");
        CALIBRATE_SLOWER = 1 - ConfigHelper.getOrSetDefault(0.003, hawk.getConfig(), "checks.clockspeed.calibrateSlower");
        CALIBRATE_FASTER = 1 - ConfigHelper.getOrSetDefault(0.03, hawk.getConfig(), "checks.clockspeed.calibrateFaster");
        RUBBERBAND = (boolean)customSetting("rubberbandOnFail", "", true);
        RESET_DRIFT_ON_FAIL = (boolean)customSetting("resetDriftOnFail", "", false);
        DENY_ACTIONS = (boolean)customSetting("denyClientActionsOnFail", "", true);
        WARM_UP = (int)customSetting("wait", "", 150) - 1;
    }

    @Override
    protected void check(PositionEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        if (event.hasTeleported() || pp.getCurrentTick() < WARM_UP)
            return;
        long time = System.nanoTime();
        if (!prevNanoTime.containsKey(p.getUniqueId())) {
            prevNanoTime.put(p.getUniqueId(), time);
            return;
        }
        time -= prevNanoTime.get(p.getUniqueId());
        prevNanoTime.put(p.getUniqueId(), System.nanoTime());

        long drift = clockDrift.getOrDefault(p.getUniqueId(), 0L);
        drift += time - 50000000L;
        if (drift > MAX_CATCHUP_TIME)
            drift = MAX_CATCHUP_TIME;
        if (DEBUG) {
            double msOffset = drift * 1E-6;
            p.sendMessage((msOffset < 0 ? (msOffset < THRESHOLD ? ChatColor.RED : ChatColor.YELLOW) : ChatColor.BLUE) + "CLOCK DRIFT: " + MathPlus.round(-msOffset, 2) + "ms");
        }
        if (drift * 1E-6 < THRESHOLD) {
            if(RUBBERBAND)
                punishAndTryRubberband(pp, event, p.getLocation());
            else
                punish(pp, true, event);
            if(RESET_DRIFT_ON_FAIL)
                drift = 0;
        } else
            reward(pp);
        if (drift < 0)
            drift *= CALIBRATE_FASTER;
        else
            drift *= CALIBRATE_SLOWER;
        clockDrift.put(p.getUniqueId(), drift);
    }

    @Override
    public void removeData(Player p) {
        prevNanoTime.remove(p.getUniqueId());
        clockDrift.remove(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        if(enabled && !p.hasPermission(permission) && clockDrift.getOrDefault(p.getUniqueId(), 0L) * 1E-6 < THRESHOLD && DENY_ACTIONS)
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onShoot(EntityShootBowEvent e) {
        if(enabled && e instanceof Player) {
            Player p = (Player) e.getEntity();
            if (!p.hasPermission(permission) && clockDrift.getOrDefault(p.getUniqueId(), 0L) * 1E-6 < THRESHOLD && DENY_ACTIONS)
                e.setCancelled(true);
        }
    }
}
