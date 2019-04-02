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
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.util.entity.EntityNMS;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FightReachApprox extends EntityInteractionCheck {

    private final double MAX_REACH;
    private final double MAX_REACH_CREATIVE;
    private final int PING_LIMIT;
    private final boolean LAG_COMPENSATION;
    private final boolean CHECK_OTHER_ENTITIES;

    public FightReachApprox() {
        super("fightreachapprox", "%player% failed fight reach (approximate). Reach: %distance%m VL: %vl%");
        MAX_REACH = ConfigHelper.getOrSetDefault(3.1, hawk.getConfig(), "checks.fightreachapprox.maxReach");
        MAX_REACH_CREATIVE = ConfigHelper.getOrSetDefault(4.9, hawk.getConfig(), "checks.fightreachapprox.maxReachCreative");
        PING_LIMIT = ConfigHelper.getOrSetDefault(-1, hawk.getConfig(), "checks.fightreachapprox.pingLimit");
        LAG_COMPENSATION = ConfigHelper.getOrSetDefault(true, hawk.getConfig(), "checks.fightreachapprox.lagCompensation");
        CHECK_OTHER_ENTITIES = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.fightreachapprox.checkOtherEntities");
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

        Location attackerLocation;
        if(ServerUtils.getClientVersion(att.getPlayer()) == 7) {
            attackerLocation = att.getPredictedLocation();
        }
        else {
            attackerLocation = att.getLocation();
        }

        Location victimLocation;
        if (victimEntity instanceof Player && LAG_COMPENSATION)
            victimLocation = hawk.getLagCompensator().getHistoryLocation(ping, (Player) victimEntity);
        else
            victimLocation = victimEntity.getLocation();

        AABB victimAABB = EntityNMS.getEntityNMS(victimEntity).getHitbox(victimLocation.toVector());
        victimAABB.expand(0.05, 0.05, 0.05);

        float headHeight = att.isSneaking() ? 1.54F : 1.62F;
        Vector attackerPos = attackerLocation.toVector().add(new Vector(0D, headHeight, 0D));

        double maxReach = att.getPlayer().getGameMode() == GameMode.CREATIVE ? MAX_REACH_CREATIVE : MAX_REACH;
        double dist = victimAABB.distanceToPosition(attackerPos);

        if (dist > maxReach) {
            punish(att, 1, true, e, new Placeholder("distance", MathPlus.round(dist, 2)));
        } else {
            reward(att);
        }
    }
}
