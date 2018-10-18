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

package me.islandscout.hawk.events.external;

import me.islandscout.hawk.utils.Violation;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class HawkViolationEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Violation violation;

    public HawkViolationEvent(Violation violation) {
        super(true); //TODO: make sure to check if the thread is async or not!
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
