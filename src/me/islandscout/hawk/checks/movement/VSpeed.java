package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.checks.AsyncMovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.Debug;
import org.bukkit.ChatColor;

public class VSpeed extends AsyncMovementCheck {

    private static final double MAX = 0.5625;
    private static final double MIN = -3.92;

    public VSpeed(Hawk hawk) {
        super(hawk, "vspeed", "&7%player% failed vertical speed.");
    }

    @Override
    protected void check(PositionEvent event) {
        double deltaY = event.getTo().getY() - event.getFrom().getY();
        if(deltaY > MAX || deltaY < MIN) {
            Debug.broadcastMessage(ChatColor.RED + "HAX");
        }
    }
}
