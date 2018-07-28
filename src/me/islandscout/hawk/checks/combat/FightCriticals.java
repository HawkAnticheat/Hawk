package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncEntityInteractionCheck;
import me.islandscout.hawk.events.InteractAction;
import me.islandscout.hawk.events.InteractEntityEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FightCriticals extends AsyncEntityInteractionCheck {

    //TODO: You'll want to make a custom getFallDistance() method in HawkPlayer.

    public FightCriticals(Hawk hawk) {
        super(hawk, "fightcriticals", "&7%player% failed fight critical. VL: %vl%");
    }

    @Override
    protected void check(InteractEntityEvent e) {
        if(e.getInteractAction() == InteractAction.ATTACK) {
            Player attacker = e.getPlayer();
            HawkPlayer att = hawk.getHawkPlayer(attacker);
            Location loc = att.getLocation().clone();
            if((attacker.getFallDistance() < 0.3 && attacker.getFallDistance() != 0 && loc.add(0, -0.3, 0).getBlock().getType().isSolid() && !loc.add(0, 2.3, 0).getBlock().getType().isSolid())) {
                punish(attacker, true, e);
                return;
            }
            reward(attacker);
        }
    }
}
