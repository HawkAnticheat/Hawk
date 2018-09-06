package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.checks.AsyncEntityInteractionCheck;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.*;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class FightReachApprox extends AsyncEntityInteractionCheck {

    private final double MAX_REACH;
    private final int PING_LIMIT;
    private final boolean LAG_COMPENSATION;

    public FightReachApprox() {
        super("fightreachapprox", "&7%player% failed fight reach (approximate). Reach: %distance%m VL: %vl%");
        MAX_REACH = Math.pow(ConfigHelper.getOrSetDefault(4, hawk.getConfig(), "checks.fightreachapprox.maxReach"), 2);
        PING_LIMIT = ConfigHelper.getOrSetDefault(-1, hawk.getConfig(), "checks.fightreachapprox.pingLimit");
        LAG_COMPENSATION = ConfigHelper.getOrSetDefault(true, hawk.getConfig(), "checks.fightreachapprox.lagCompensation");
    }

    @Override
    protected void check(InteractEntityEvent e) {
        Entity victimEntity = e.getEntity();
        int ping = ServerUtils.getPing(e.getPlayer());
        if(PING_LIMIT > -1 && ping > PING_LIMIT)
            return;
        Location victimLocation;
        if(victimEntity instanceof Player && LAG_COMPENSATION)
            victimLocation = hawk.getLagCompensator().getHistoryLocation(ping, (Player)victimEntity);
        else
            victimLocation = victimEntity.getLocation();
        double distanceSquared = victimLocation.distanceSquared(e.getHawkPlayer().getLocation());
        if(distanceSquared > MAX_REACH) {
            punish(e.getHawkPlayer(), true, e, new Placeholder("distance", MathPlus.round(Math.sqrt(distanceSquared), 2)));
        }
        else {
            reward(e.getHawkPlayer());
        }
    }
}
