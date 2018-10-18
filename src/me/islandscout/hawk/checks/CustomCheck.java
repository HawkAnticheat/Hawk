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

package me.islandscout.hawk.checks;

import me.islandscout.hawk.events.Event;

import java.util.List;

public abstract class CustomCheck extends Check<Event> {

    protected CustomCheck(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelThreshold, flagThreshold, vlPassMultiplier, flagCooldown, flag, punishCommands);
        hawk.getCheckManager().getCustomChecks().add(this);
    }

    protected CustomCheck(String name, String flag) {
        this(name, true, 0, 5, 0.9, 5000, flag, null);
    }
}
