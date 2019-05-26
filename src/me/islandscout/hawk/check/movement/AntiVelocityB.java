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
import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;

/**
 * The AntiVelocityB check monitors the abuse of an exploit
 * in the client which alters received knockback significantly.
 * The exploit is as follows: sprint-jump on the same tick that you
 * receive knockback. This is difficult to achieve consistently
 * without external assistance; those that can do it consistently
 * are most likely cheating.
 */
public class AntiVelocityB extends MovementCheck implements Cancelless {

    public AntiVelocityB() {
        super("antivelocityb", false, -1, 5, 0.999, 5000, "%player% may be using anti-velocity. VL: %vl%", null);
    }

    @Override
    protected void check(MoveEvent event) {
        HawkPlayer pp = event.getHawkPlayer();
        if(event.hasAcceptedKnockback() && event.hasJumped()) { //TODO make sure to ignore kbs with a Y of 0.42 and not under a block
            //TODO add to a ratio
        }
    }
}
