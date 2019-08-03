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
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.packet.WrappedPacket;

/*
 * Check written by Havesta
 *
 * SyntheticMove checks if a (C05 packet) or a (C06 packet with updated POSITION) is sent by the client
 * (C06 = position and rotation changed, C04 = position changed, C05 = rotation changed)
 *  with the same rotation as he had before... that means that the player
 *  A: got teleported
 *  or B: is cheating and just cancelling packets and sending new ones
 *  (often used in killaura, scaffold, tower and autopotion)
 *  inspired by HeroCode: https://www.youtube.com/watch?v=3MN9EkPjOZ0
 */
public class SyntheticMove extends MovementCheck {

    //Stops lazy cheaters. One VL deserves an autoban.

    public SyntheticMove() {
        super("syntheticmove", true, 2, 5, 0.999, 5000, "%player% failed synthetic-move, VL: %vl%", null);
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        //Also ignore the move after the tp to fix false positives - Islandscout
        if(pp.getCurrentTick() - pp.getLastTeleportAcceptTick() > 1) {
            WrappedPacket packet = e.getWrappedPacket();
            switch(packet.getType()) {
                case POSITION:
                    //We can extend the check to position packets by checking
                    //the velocity since the last flying packet - Islandscout
                    if(!e.hasDeltaPos() && pp.getVelocity().lengthSquared() > 0) {
                        punishAndTryRubberband(e.getHawkPlayer(), e, e.getPlayer().getLocation());
                    } else {
                        reward(e.getHawkPlayer());
                    }
                    break;
                case LOOK:
                    if(!e.hasDeltaRot()) {
                        punishAndTryRubberband(e.getHawkPlayer(), e, e.getPlayer().getLocation());
                    } else {
                        reward(e.getHawkPlayer());
                    }
                    break;
                case POSITION_LOOK:
                    if(!e.hasDeltaRot() || (!e.hasDeltaPos() && pp.getVelocity().lengthSquared() > 0)) {
                        punishAndTryRubberband(e.getHawkPlayer(), e, e.getPlayer().getLocation());
                    } else {
                        reward(e.getHawkPlayer());
                    }
                    break;
            }
        }
    }
}
