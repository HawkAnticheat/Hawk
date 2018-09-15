package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.checks.AsyncEntityInteractionCheck;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.ConfigHelper;
import me.islandscout.hawk.utils.Debug;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class FightDirectionApprox extends AsyncEntityInteractionCheck {

    //PASSED (9/11/18)

    //Define isosceles triangle. The base will be located at victim's X & Z location,
    //and will have a width approximately the horizontal diagonal length of the victim's hitbox.
    //The vertex point will be at the attacker's X & Z location. This triangle will
    //be facing up.
    private final double TRIANGLE_HALF_BASE_WIDTH = 0.65;
    private final double TRIANGLE_BASE_HEIGHT_OFFSET = 0.3;
    private final int PING_LIMIT;
    private final boolean LAG_COMPENSATION;
    private final boolean CHECK_OTHER_ENTITIES;

    public FightDirectionApprox() {
        super("fightdirectionapprox", "&7%player% failed fight direction. VL: %vl%");
        PING_LIMIT = ConfigHelper.getOrSetDefault(-1, hawk.getConfig(), "checks.fightdirectionapprox.pingLimit");
        LAG_COMPENSATION = ConfigHelper.getOrSetDefault(true, hawk.getConfig(), "checks.fightdirectionapprox.lagCompensation");
        CHECK_OTHER_ENTITIES = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.fightdirectionapprox.checkOtherEntities");
    }

    @Override
    protected void check(InteractEntityEvent e) {
        Entity victimEntity = e.getEntity();
        if(!(victimEntity instanceof Player) && !CHECK_OTHER_ENTITIES)
            return;
        int ping = ServerUtils.getPing(e.getPlayer());
        if(PING_LIMIT > -1 && ping > PING_LIMIT)
            return;
        Vector victimLocation;
        Vector attackerLocation = e.getHawkPlayer().getLocation().toVector().setY(0);
        Vector direction = e.getHawkPlayer().getLocation().getDirection().clone().setY(0);
        if(victimEntity instanceof Player && LAG_COMPENSATION)
            victimLocation = hawk.getLagCompensator().getHistoryLocation(ping, (Player)victimEntity).toVector().setY(0);
        else
            victimLocation = victimEntity.getLocation().toVector().setY(0);

        //trigonometry is so much fun
        double triangleAltitude = victimLocation.subtract(attackerLocation).length();
        if(triangleAltitude < 1)
            return;
        double maxAngleOffset = Math.atan(TRIANGLE_HALF_BASE_WIDTH / (triangleAltitude - TRIANGLE_BASE_HEIGHT_OFFSET));

        double angleOffset = direction.angle(victimLocation);

        if(angleOffset > maxAngleOffset) {
            punish(e.getHawkPlayer(), true, e);
        }
        else {
            reward(e.getHawkPlayer());
        }
    }
}
