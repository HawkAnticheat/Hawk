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
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FightDirectionApprox extends EntityInteractionCheck {

    //PASSED (9/11/18)

    private final int PING_LIMIT;
    private final boolean LAG_COMPENSATION;
    private final boolean CHECK_OTHER_ENTITIES;

    public FightDirectionApprox() {
        super("fightdirectionapprox", "%player% failed fight direction. VL: %vl%");
        PING_LIMIT = (int) customSetting("pingLimit", "", -1);
        LAG_COMPENSATION = (boolean) customSetting("lagCompensation", "", true);
        CHECK_OTHER_ENTITIES = (boolean) customSetting("checkOtherEntities", "", false);
    }

    @Override
    protected void check(InteractEntityEvent e) {
        Entity victimEntity = e.getEntity();
        if (!(victimEntity instanceof Player) && !CHECK_OTHER_ENTITIES)
            return;
        int ping = ServerUtils.getPing(e.getPlayer());
        if (PING_LIMIT > -1 && ping > PING_LIMIT)
            return;
        HawkPlayer att = e.getHawkPlayer();

        Vector attackerLocation;
        Vector direction;
        if(ServerUtils.getClientVersion(att.getPlayer()) == 7) {
            attackerLocation = att.getPredictedLocation().toVector().setY(0);
            direction = att.getPredictedLocation().getDirection().clone().setY(0);
        }
        else {
            attackerLocation = att.getPosition().clone().setY(0);
            direction = new Location(null, 0, 0, 0, att.getYaw(), att.getPitch()).getDirection().setY(0);
        }

        Vector victimLocation;
        if (LAG_COMPENSATION)
            victimLocation = hawk.getLagCompensator().getHistoryLocation(ping, victimEntity).toVector().setY(0);
        else
            victimLocation = victimEntity.getLocation().toVector().setY(0);

        //trigonometry is so much fun
        double triangleAltitude = victimLocation.subtract(attackerLocation).length();
        if (triangleAltitude < 1)
            return;
        //Define isosceles triangle. The base will be located at victim's X & Z location,
        //and will have a width approximately the horizontal diagonal length of the victim's hitbox.
        //The vertex point will be at the attacker's X & Z location. This triangle will
        //be facing vertically (all vertices having the same Y coordinate).
        final double TRIANGLE_HALF_BASE_WIDTH = 0.65;
        final double TRIANGLE_BASE_HEIGHT_OFFSET = 0.3;
        double maxAngleOffset = Math.atan(TRIANGLE_HALF_BASE_WIDTH / (triangleAltitude - TRIANGLE_BASE_HEIGHT_OFFSET));

        double angleOffset = direction.angle(victimLocation);

        if (angleOffset > maxAngleOffset) {
            punish(e.getHawkPlayer(), 1, true, e);
        } else {
            reward(e.getHawkPlayer());
        }
    }
}
