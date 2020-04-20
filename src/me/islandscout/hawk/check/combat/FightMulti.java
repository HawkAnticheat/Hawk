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

package me.islandscout.hawk.check.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.EntityInteractionCheck;
import me.islandscout.hawk.event.InteractEntityEvent;

public class FightMulti extends EntityInteractionCheck {

    public FightMulti() {
        super("fightmulti", true, 0, 5, 0.999, 5000, "%player% failed fight multi, VL %vl%", null);
    }

    @Override
    protected void check(InteractEntityEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        if (pp.getEntitiesInteractedInThisTick().size() > 0 && !pp.getEntitiesInteractedInThisTick().contains(e.getEntity())) {
            punish(pp, true, e);
        } else {
            reward(pp);
        }
    }
}
