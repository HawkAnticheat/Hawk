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

package me.islandscout.hawk.util;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.Location;

public class Teleport {

    private Cause cause;
    private HawkPlayer pp;
    private Location to;

    public Teleport(Cause cause, HawkPlayer pp, Location to) {
        this.cause = cause;
        this.pp = pp;
        this.to = to;
    }

    public Cause getCause() {
        return cause;
    }

    public HawkPlayer getHawkPlayer() {
        return pp;
    }

    public Location getTo() {
        return to;
    }

    public enum Cause {
        ANTICHEAT_RESYNC,
        OTHER
    }

}
