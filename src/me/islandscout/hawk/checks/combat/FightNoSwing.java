package me.islandscout.hawk.checks.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncCustomCheck;
import me.islandscout.hawk.events.ArmSwingEvent;
import me.islandscout.hawk.events.Event;
import me.islandscout.hawk.events.InteractAction;
import me.islandscout.hawk.events.InteractEntityEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FightNoSwing extends AsyncCustomCheck {

    private Map<UUID, Long> lastClientTickSwung;
    private static final int THRESHOLD = 3;

    public FightNoSwing() {
        super("fightnoswing", "&7%player% failed noswing. VL: %vl%");
        lastClientTickSwung = new HashMap<>();
    }

    @Override
    protected void check(Event event) {
        if(event instanceof ArmSwingEvent)
            processSwing((ArmSwingEvent)event);
        else if(event instanceof InteractEntityEvent)
            processHit((InteractEntityEvent)event);

    }

    private void processSwing(ArmSwingEvent e) {
        lastClientTickSwung.put(e.getPlayer().getUniqueId(), hawk.getHawkPlayer(e.getPlayer()).getCurrentTick());
    }

    private void processHit(InteractEntityEvent e) {
        if(e.getInteractAction() != InteractAction.ATTACK)
            return;
        Player p = e.getPlayer();
        HawkPlayer pp = hawk.getHawkPlayer(p);
        if(!lastClientTickSwung.containsKey(p.getUniqueId()) || pp.getCurrentTick() - lastClientTickSwung.get(p.getUniqueId()) > THRESHOLD) {
            punish(p, true, e);
        }
        else {
            reward(p);
        }
    }
}
