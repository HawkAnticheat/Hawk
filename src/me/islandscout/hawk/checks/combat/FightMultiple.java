package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncEntityInteractionCheck;
import me.islandscout.hawk.events.InteractAction;
import me.islandscout.hawk.events.InteractEntityEvent;
import me.islandscout.hawk.utils.Debug;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FightMultiple extends AsyncEntityInteractionCheck {

    private Map<UUID, Long> lastClientTick;

    public FightMultiple() {
        super("fightmultiple", "&7%player% is definitely using killaura. VL %vl%");
        lastClientTick = new HashMap<>();
    }

    @Override
    protected void check(InteractEntityEvent e) {
        if(e.getInteractAction() != InteractAction.ATTACK)
            return;
        HawkPlayer pp = e.getHawkPlayer();
        long lastTick = lastClientTick.getOrDefault(e.getPlayer().getUniqueId(), 0L);
        long currentTick = pp.getCurrentTick();
        if(currentTick == lastTick)
            punish(pp, true, e);
        else {
            reward(pp);
        }
        lastClientTick.put(e.getPlayer().getUniqueId(), pp.getCurrentTick());
    }
}
