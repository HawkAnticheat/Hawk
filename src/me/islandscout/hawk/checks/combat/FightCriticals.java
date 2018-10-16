package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.EntityInteractionCheck;
import me.islandscout.hawk.events.InteractAction;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.AdjacentBlocks;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class FightCriticals extends EntityInteractionCheck {

    //TODO: Perhaps get jump height rather than fall distance? Might eliminate false pos when jumping on blocks and attacking.

    public FightCriticals() {
        super("fightcriticals", "%player% failed fight criticals. VL: %vl%");
    }

    @Override
    protected void check(InteractEntityEvent e) {
        if (e.getInteractAction() == InteractAction.ATTACK) {
            HawkPlayer att = e.getHawkPlayer();
            Location loc = att.getLocation().clone();

            Block below = ServerUtils.getBlockAsync(loc.add(0, -0.3, 0));
            Block above = ServerUtils.getBlockAsync(loc.add(0, 2.3, 0));
            if (below == null || above == null)
                return;
            if (AdjacentBlocks.onGroundReally(att.getLocation(), -1, true) && !att.isOnGround() ||
                    (att.getFallDistance() < 0.3 && att.getFallDistance() != 0 && below.getType().isSolid() && !above.getType().isSolid())) {
                punish(att, true, e);
                return;
            }
            reward(att);
        }
    }
}
