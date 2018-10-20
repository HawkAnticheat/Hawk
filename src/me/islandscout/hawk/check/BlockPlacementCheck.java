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

package me.islandscout.hawk.check;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.event.BlockPlaceEvent;
import me.islandscout.hawk.util.Placeholder;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.util.block.BlockNMS7;
import me.islandscout.hawk.util.block.BlockNMS8;
import org.bukkit.block.Block;

import java.util.List;

public abstract class BlockPlacementCheck extends Check<BlockPlaceEvent> {

    protected BlockPlacementCheck(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        super(name, enabled, cancelThreshold, flagThreshold, vlPassMultiplier, flagCooldown, flag, punishCommands);
        hawk.getCheckManager().getBlockPlacementChecks().add(this);
    }

    protected BlockPlacementCheck(String name, String flag) {
        this(name, true, 0, 5, 0.9, 5000, flag, null);
    }

    protected void punishAndTryCancelAndBlockDestroy(HawkPlayer offender, BlockPlaceEvent event, Placeholder... placeholders) {
        punish(offender, true, event, placeholders);
        Block b = ServerUtils.getBlockAsync(event.getLocation());
        if (offender.getVL(this) < cancelThreshold || b == null)
            return;
        if (Hawk.getServerVersion() == 7) {
            BlockNMS7.getBlockNMS(b).sendPacketToPlayer(offender.getPlayer());
        } else if (Hawk.getServerVersion() == 8) {
            BlockNMS8.getBlockNMS(b).sendPacketToPlayer(offender.getPlayer());
        }
    }
}
