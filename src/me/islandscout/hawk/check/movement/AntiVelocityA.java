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
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.event.MoveEvent;
import org.bukkit.event.Listener;

/**
 * The AntiVelocityA check verifies that players accept knockback.
 * It relies on MoveEvent's knockback acceptance detection.
 * The AntiVelocityA check has been tested to detect as low as a
 * 10% difference in horizontal or vertical knockback.
 */
public class AntiVelocityA extends MovementCheck implements Cancelless {

    public AntiVelocityA() {
        super("antivelocitya", true, -1, 5, 0.999, 5000, "%player% may be using anti-velocity. VL: %vl%", null);
    }

    @Override
    protected void check(MoveEvent event) {
        HawkPlayer pp = event.getHawkPlayer();
        if(event.hasFailedKnockback())
            punish(pp, false, event);
        else
            reward(pp);
    }
}
