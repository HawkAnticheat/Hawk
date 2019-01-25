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

import me.islandscout.hawk.event.bukkit.HawkPlayerAsyncVelocityChangeEvent;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.Pair;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.PhysicsUtils;
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.*;

public class AntiVelocity extends MovementCheck implements Listener, Cancelless {

    public AntiVelocity() {
        super("antivelocity", false, -1, 5, 0.95, 5000, "%player% may be using antivelocity. VL: %vl%", null);
    }

    @Override
    protected void check(PositionEvent event) {
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVelocity(HawkPlayerAsyncVelocityChangeEvent e) {

    }

    @Override
    public void removeData(Player p) {

    }
}
