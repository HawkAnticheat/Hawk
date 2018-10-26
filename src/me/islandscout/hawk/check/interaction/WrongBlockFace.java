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

package me.islandscout.hawk.check.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.BlockPlacementCheck;
import me.islandscout.hawk.event.BlockPlaceEvent;

/** This check prevents players from interacting on
 * unavailable locations on blocks. Players must be
 * looking at the face of the block they want to interact
 * with.
 */
public class WrongBlockFace extends BlockPlacementCheck {

    public WrongBlockFace() {
        super("wrongblockface", true, 0, 10, 0.99, 5000, "%player% failed wrongblockface; interacted on invalid block face, VL: %vl%", null);
    }

    @Override
    protected void check(BlockPlaceEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        if(e.getTargetedBlockFaceNormal().dot(pp.getLocation().getDirection()) >= 0) {
            punishAndTryCancelAndBlockRespawn(pp, e);
        }
    }
}
