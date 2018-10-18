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

package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.EntityInteractionCheck;
import me.islandscout.hawk.events.InteractAction;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class FightCriticals extends EntityInteractionCheck {

    //TODO: Perhaps get jump height rather than fall distance? Might eliminate false pos when jumping on blocks and attacking.

    public FightCriticals() {
        super("fightcriticals", "%player% failed fight criticals. VL: %vl%");
    }

    @Override
    protected void check(InteractEntityEvent e) {
        if (e.getInteractAction() == InteractAction.ATTACK) {
            HawkPlayer att = e.getHawkPlayer();
            Location loc = att.getLocation().clone();

            Block below = ServerUtils.getBlockAsync(loc.add(0, -0.3, 0));
            Block above = ServerUtils.getBlockAsync(loc.add(0, 2.3, 0));
            if (below == null || above == null)
                return;
            if (AdjacentBlocks.onGroundReally(att.getLocation(), -1, true) && !att.isOnGround() ||
                    (att.getFallDistance() < 0.3 && att.getFallDistance() != 0 && below.getType().isSolid() && !above.getType().isSolid())) {
                punish(att, true, e);
                return;
            }
            reward(att);
        }
    }
}
