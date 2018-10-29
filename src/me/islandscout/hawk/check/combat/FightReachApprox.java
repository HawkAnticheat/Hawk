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

package me.islandscout.hawk.check.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.EntityInteractionCheck;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.util.ConfigHelper;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.Placeholder;
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class FightReachApprox extends EntityInteractionCheck {

    private final double MAX_REACH;
    private final int PING_LIMIT;
    private final boolean LAG_COMPENSATION;
    private final boolean CHECK_OTHER_ENTITIES;

    public FightReachApprox() {
        super("fightreachapprox", "%player% failed fight reach (approximate). Reach: %distance%m VL: %vl%");
        MAX_REACH = Math.pow(ConfigHelper.getOrSetDefault(4, hawk.getConfig(), "checks.fightreachapprox.maxReach"), 2);
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
        double feetFeetDistanceSquared = victimLocation.distanceSquared(attackerLocation);
        double eyeFeetDistanceSquared = victimLocation.distanceSquared(attackerLocation.clone().add(0, 1.62, 0));
        double maxReach = MAX_REACH;
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE)
            maxReach += 17.64; //MC1.7: 1.8, MC1.8: 1.5
        if (feetFeetDistanceSquared > maxReach && eyeFeetDistanceSquared > maxReach) {
            punish(att, true, e, new Placeholder("distance", MathPlus.round(Math.sqrt(feetFeetDistanceSquared), 2)));
        } else {
            reward(att);
        }
    }
}
