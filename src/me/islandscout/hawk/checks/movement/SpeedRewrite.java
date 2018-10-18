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

package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.MovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeedRewrite extends MovementCheck {

    private final Map<UUID, Double> threshold;
    private final double GROUND_DEFAULT;
    private final double AIR_DEFAULT;
    private final double FLY_DEFAULT;

    public SpeedRewrite() {
        super("speednew", "%player% failed ground speed");
        threshold = new HashMap<>();
        GROUND_DEFAULT = Math.pow(0.28635, 2);
        AIR_DEFAULT = Math.pow(0.2888889, 2);
        FLY_DEFAULT = 1;
    }

    int i = 0;
    boolean ready = false;

    @Override
    protected void check(PositionEvent event) {
        if (!event.hasDeltaPos())
            return;
        Player p = event.getPlayer();
        HawkPlayer pp = event.getHawkPlayer();
        double speedSquared = Math.pow(event.getTo().getX() - event.getFrom().getX(), 2) + Math.pow(event.getTo().getZ() - event.getFrom().getZ(), 2);

        if (speedSquared != 0)
            ready = true;
        if (ready) {
            //Debug.broadcastMessage("(" + i + ", " + Math.sqrt(speedSquared) + ")");
            i++;
        }

        if (speedSquared > GROUND_DEFAULT) {
            //punishAndTryRubberband(pp, event, p.getLocation());
        }
    }
}
