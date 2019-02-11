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
import me.islandscout.hawk.util.MathPlus;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SprintDirection extends MovementCheck {

    //Not the best method in the world for checking for omni-sprint, but it works.

    public SprintDirection() {
        super("sprintdirection", true, 0, 5, 0.999, 5000, "%player% failed sprint direction, VL %vl%", null);
    }

    @Override
    protected void check(MoveEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        if(!pp.isSprinting()) {
            return;
        }
        Vector look = MathPlus.getDirection(e.getTo().getYaw(), e.getTo().getPitch()).setY(0);
        Vector move = new Vector(e.getTo().getX() - e.getFrom().getX(), 0, e.getTo().getZ() - e.getFrom().getZ());
        Vector lastMove = pp.getVelocity().clone().setY(0);
        double lastSpeed = lastMove.length();
        double currSpeed = move.length();

        double moveChange = move.angle(lastMove);
        double speedChange = Math.max(lastSpeed, currSpeed) / Math.min(lastSpeed, currSpeed) - 1;
        if(Double.isInfinite(speedChange))
            speedChange = 1;
        double value = look.angle(move) / (5 * (speedChange + moveChange) + 1);

        if(!e.hasAcceptedKnockback()) {
            if(e.isOnGroundReally() && pp.isOnGround()) {
                if(value > 1.0) {
                    punishAndTryRubberband(pp, e, p.getLocation());
                }
            }
            else {
                if(value > 2) {
                    punishAndTryRubberband(pp, e, p.getLocation());
                }
            }

        }
    }
}
