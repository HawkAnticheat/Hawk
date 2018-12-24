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

package me.islandscout.hawk.event.bukkit;

import me.islandscout.hawk.util.Violation;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class HawkFlagEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Violation violation;

    public HawkFlagEvent(Violation violation) {
        super();
        this.violation = violation;
    }

    public Violation getViolation() {
        return violation;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
