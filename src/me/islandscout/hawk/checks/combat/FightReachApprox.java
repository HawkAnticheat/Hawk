package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.checks.AsyncEntityInteractionCheck;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.*;
import me.islandscout.hawk.utils.entities.EntityNMS;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class FightReachApprox extends AsyncEntityInteractionCheck {

    //TODO: Improve reach approximation. Seriously. This is trash.

    private final double MAX_REACH;
    private final int PING_LIMIT;
    private final boolean LAG_COMPENSATION;
    private final boolean CHECK_OTHER_ENTITIES;

    public FightReachApprox() {
        super("fightreachapprox", "&7%player% failed fight reach (approximate). Reach: %distance%m VL: %vl%");
        MAX_REACH = Math.pow(ConfigHelper.getOrSetDefault(4, hawk.getConfig(), "checks.fightreachapprox.maxReach"), 2);
        PING_LIMIT = ConfigHelper.getOrSetDefault(-1, hawk.getConfig(), "checks.fightreachapprox.pingLimit");
        LAG_COMPENSATION = ConfigHelper.getOrSetDefault(true, hawk.getConfig(), "checks.fightreachapprox.lagCompensation");
        CHECK_OTHER_ENTITIES = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.fightreachapprox.checkOtherEntities");
    }

    @Override
    protected void check(InteractEntityEvent e) {
        Entity victimEntity = e.getEntity();
        if(!(victimEntity instanceof Player) && !CHECK_OTHER_ENTITIES)
            return;
        int ping = ServerUtils.getPing(e.getPlayer());
        if(PING_LIMIT > -1 && ping > PING_LIMIT)
            return;

        Location victimLocation;
        if(victimEntity instanceof Player && LAG_COMPENSATION)
            victimLocation = hawk.getLagCompensator().getHistoryLocation(ping, (Player)victimEntity);
        else
            victimLocation = victimEntity.getLocation();
        double distanceSquared = victimLocation.distanceSquared(e.getHawkPlayer().getLocation());
        double maxReach = MAX_REACH;
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE)
            maxReach += 17.64; //MC1.7: 1.8, MC1.8: 1.5
        if(distanceSquared > maxReach) {
            punish(e.getHawkPlayer(), true, e, new Placeholder("distance", MathPlus.round(Math.sqrt(distanceSquared), 2)));
        }
        else {
            reward(e.getHawkPlayer());
        }
    }
}
