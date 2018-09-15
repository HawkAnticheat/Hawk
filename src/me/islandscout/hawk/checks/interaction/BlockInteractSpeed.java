package me.islandscout.hawk.checks.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncBlockPlacementCheck;
import me.islandscout.hawk.events.BlockPlaceEvent;
import me.islandscout.hawk.utils.Debug;
import org.bukkit.entity.Player;

import java.util.*;

public class BlockInteractSpeed extends AsyncBlockPlacementCheck {

    private Map<UUID, Long> lastPlaceTick;

    public BlockInteractSpeed() {
        super("blockplacespeed", "&7%player% failed block place speed. VL: %vl%");
        lastPlaceTick = new HashMap<>();
    }

    @Override
    protected void check(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        if(pp.getCurrentTick() == lastPlaceTick.getOrDefault(p.getUniqueId(), 0L))
            punishAndTryCancelAndBlockDestroy(pp, e);
        else
            reward(pp);

        lastPlaceTick.put(p.getUniqueId(), pp.getCurrentTick());
    }

    @Override
    public void removeData(Player p) {
        lastPlaceTick.remove(p.getUniqueId());
    }
}
