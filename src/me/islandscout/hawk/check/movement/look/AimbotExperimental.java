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

package me.islandscout.hawk.check.movement.look;

import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AimbotExperimental extends MovementCheck {

    public AimbotExperimental() {
        super("aimbotD", "%player% failed aimbotD, VL: %vl%");
    }

    @Override
    protected void check(MoveEvent e) {
        Player victim = Bukkit.getPlayer("Islandscout");
        if (victim == null) {
            return;
        }

        //TODO make sure to account for when the attacker is moving
        Vector pos = e.getFrom().toVector();
        float deltaYaw = e.getTo().getYaw() - e.getFrom().getYaw();
        float deltaPitch = e.getTo().getPitch() - e.getFrom().getPitch();

        int ping = ServerUtils.getPing(e.getPlayer()) + 50;
        Vector relEntityPosB = hawk.getLagCompensator().getHistoryLocationNoLerp(ping, victim).toVector();
        Vector relEntityVelocity = hawk.getLagCompensator().getHistoryVelocity(ping, victim);
        Vector relEntityPosA = relEntityPosB.clone().add(relEntityVelocity);
        Vector toRelEntityPosA = relEntityPosA.clone().subtract(pos);
        Vector toRelEntityPosB = relEntityPosB.clone().subtract(pos);
        int dir = (int) Math.signum(toRelEntityPosA.clone().crossProduct(toRelEntityPosB.clone()).getY());
        double distBSquared = pos.distanceSquared(relEntityPosB);
        double distASquared = pos.distanceSquared(relEntityPosA);
        //law of cosines
        double bestDeltaYaw = dir * Math.toDegrees(Math.acos((distASquared + distBSquared - relEntityVelocity.lengthSquared()) / (2 * Math.sqrt(distASquared * distBSquared))));

        //TODO integrate these delta yaws and then compare them

        //Debug.broadcastMessage( dir + " " + Math.abs(bestDeltaYaw - deltaYaw));
    }
}
