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

package me.islandscout.hawk.check;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.event.BlockDigEvent;
import me.islandscout.hawk.util.Placeholder;

import java.util.List;

public abstract class BlockDigCheck extends Check<BlockDigEvent> {

    protected BlockDigCheck(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelThreshold, flagThreshold, vlPassMultiplier, flagCooldown, flag, punishCommands);
        hawk.getCheckManager().getBlockDigChecks().add(this);
    }

    protected BlockDigCheck(String name, String flag) {
        this(name, true, 0, 5, 0.9, 5000, flag, null);
    }

    protected void punishAndTryCancelAndBlockRespawn(HawkPlayer offender, BlockDigEvent event, Placeholder... placeholders) {
        punishAndTryCancelAndBlockRespawn(offender, 1, event, placeholders);
    }

    protected void punishAndTryCancelAndBlockRespawn(HawkPlayer offender, double vlAmnt, BlockDigEvent event, Placeholder... placeholders) {
        punish(offender, vlAmnt, true, event, placeholders);
        if (offender.getVL(this) >= cancelThreshold)
            event.resync();
    }
}
