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

import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.Debug;

public class Bouncy extends MovementCheck {

    //Debug class

    public Bouncy() {
        super("bouncy", "foo");
    }

    private double maxYPos;
    private double velocityOnImpact;
    private boolean falling;

    @Override
    protected void check(MoveEvent event) {
        double deltaY = event.getTo().getY() - event.getFrom().getY();
        if (!event.isOnGround())
            velocityOnImpact = event.getTo().getY() - event.getFrom().getY();
        if (!falling && deltaY < 0) {
            falling = true;
            maxYPos = event.getFrom().getY();
        }
        if (falling && deltaY >= 0) {
            falling = false;
            Debug.broadcastMessage("BOUNCE VELOCITY: " + (event.getTo().getY() - event.getFrom().getY()));
            Debug.broadcastMessage("----");
        }
        if (event.isOnGround() && falling) {
            Debug.broadcastMessage("MAX Y: " + (maxYPos - event.getTo().getBlockY()));
            Debug.broadcastMessage("APPROX IMPACT VELOCITY: " + velocityOnImpact);
        }
    }
}
