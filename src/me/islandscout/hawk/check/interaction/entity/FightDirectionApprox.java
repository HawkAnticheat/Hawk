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

package me.islandscout.hawk.check.interaction.entity;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.EntityInteractionCheck;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FightDirectionApprox extends EntityInteractionCheck {

    private final int PING_LIMIT;
    private final boolean LAG_COMPENSATION;
    private final boolean CHECK_OTHER_ENTITIES;
    private final double BOX_EXPAND;

    public FightDirectionApprox() {
        super("fightdirectionapprox", true, 0, 10, 0.9, 5000, "%player% failed fight direction, VL: %vl%", null);
        PING_LIMIT = (int) customSetting("pingLimit", "", -1);
        LAG_COMPENSATION = (boolean) customSetting("lagCompensation", "", true);
        CHECK_OTHER_ENTITIES = (boolean) customSetting("checkOtherEntities", "", true);
        BOX_EXPAND = (double) customSetting("boxExpand", "", 0.2);
    }

    @Override
    protected void check(InteractEntityEvent e) {
        HawkPlayer pp = e.getHawkPlayer();
        Entity victimEntity = e.getEntity();
        if (!(victimEntity instanceof Player) && !CHECK_OTHER_ENTITIES)
            return;
        int ping = ServerUtils.getPing(e.getPlayer());
        if (PING_LIMIT > -1 && ping > PING_LIMIT)
            return;
        Vector pos = pp.getPosition().clone().add(new Vector(0, pp.isSneaking() ? 1.54 : 1.62, 0));
        //As I always say in math class, converting polar coordinates to rectangular coordinates is a pain in
        //the ass. If you want to use the previous direction vector, you cannot make an accurate cut through the AABB
        //using only the previous and extrapolated direction vectors. You'd think you could since the extrapolation
        //on yaw and pitch is linear, but the current direction vector won't lie between the other two. Remember Non-
        //Euclidean geometry!
        Vector dir = MathPlus.getDirection(pp.getYaw(), pp.getPitch());
        //Note: in MC 1.8, the cursor yaw is not updated per frame, but rather per tick.
        //For ray-hitbox checks, this means that we do not need to extrapolate the yaw,
        //but for this check it does not matter whether we do it or not.
        Vector extraDir = MathPlus.getDirection(pp.getYaw() + pp.getDeltaYaw(), pp.getPitch() + pp.getDeltaPitch());

        Location victimLoc;
        if(LAG_COMPENSATION)
            victimLoc = hawk.getLagCompensator().getHistoryLocation(ping, victimEntity);
        else
            victimLoc = e.getEntity().getLocation();

        AABB victimAABB = WrappedEntity.getWrappedEntity(e.getEntity()).getHitbox(victimLoc.toVector());
        victimAABB.expand(BOX_EXPAND, BOX_EXPAND, BOX_EXPAND);

        //this is a crappy workaround to one of the issues with AABB#betweenRays. I'll keep this here until it is rewritten.
        Vector toVictim = victimLoc.toVector().setY(0).subtract(pos.clone().setY(0));
        boolean behind = toVictim.clone().normalize().dot(dir.clone().setY(0).normalize()) < 0 && toVictim.lengthSquared() > victimAABB.getMax().clone().setY(0).subtract(victimAABB.getMin().clone().setY(0)).lengthSquared();

        if(victimAABB.betweenRays(pos, dir, extraDir) && !behind) {
            reward(pp);
        }
        else {
            punish(pp, true, e);
        }
    }
}
