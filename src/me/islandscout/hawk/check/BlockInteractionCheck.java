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

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.event.InteractWorldAndItemEvent;
import me.islandscout.hawk.util.Placeholder;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.util.block.BlockNMS7;
import me.islandscout.hawk.util.block.BlockNMS8;
import org.bukkit.block.Block;

import java.util.List;

public abstract class BlockInteractionCheck extends Check<InteractWorldAndItemEvent> {

    protected BlockInteractionCheck(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelThreshold, flagThreshold, vlPassMultiplier, flagCooldown, flag, punishCommands);
        hawk.getCheckManager().getBlockInteractionChecks().add(this);
    }

    protected BlockInteractionCheck(String name, String flag) {
        this(name, true, 0, 5, 0.9, 5000, flag, null);
    }

    protected void punishAndTryCancelAndBlockRespawn(HawkPlayer offender, InteractWorldAndItemEvent event, Placeholder... placeholders) {
        punish(offender, true, event, placeholders);
        if (offender.getVL(this) < cancelThreshold)
            return;
        blockRespawn(offender, event);
    }

    protected void blockRespawn(HawkPlayer offender, InteractWorldAndItemEvent event) {
        Block b = ServerUtils.getBlockAsync(event.getPlacedBlockLocation());
        if(b == null)
            return;
        if (Hawk.getServerVersion() == 7) {
            BlockNMS7.getBlockNMS(b).sendPacketToPlayer(offender.getPlayer());
        } else if (Hawk.getServerVersion() == 8) {
            BlockNMS8.getBlockNMS(b).sendPacketToPlayer(offender.getPlayer());
        }
    }
}
