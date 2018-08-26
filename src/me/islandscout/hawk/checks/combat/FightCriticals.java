package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncEntityInteractionCheck;
import me.islandscout.hawk.events.InteractAction;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class FightCriticals extends AsyncEntityInteractionCheck {

    //TODO: Perhaps get jump height rather than fall distance? Might eliminate false pos when jumping on blocks and attacking.

    public FightCriticals() {
        super("fightcriticals", "&7%player% failed fight criticals. VL: %vl%");
    }

    @Override
    protected void check(InteractEntityEvent e) {
        if(e.getInteractAction() == InteractAction.ATTACK) {
            Player attacker = e.getPlayer();
            HawkPlayer att = hawk.getHawkPlayer(attacker);
            Location loc = att.getLocation().clone();

            Block below = ServerUtils.getBlockAsync(loc.add(0, -0.3, 0));
            Block above = ServerUtils.getBlockAsync(loc.add(0, 2.3, 0));
            if(below == null || above == null)
                return;
            if((att.getFallDistance() < 0.3 && attacker.getFallDistance() != 0 && below.getType().isSolid() && !above.getType().isSolid())) {
                punish(attacker, true, e);
                return;
            }
            reward(attacker);
        }
    }
}
