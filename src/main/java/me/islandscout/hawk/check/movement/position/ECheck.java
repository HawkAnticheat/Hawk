package me.islandscout.hawk.check.movement.position;

import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.MathPlus;

public class ECheck extends MovementCheck {

    public ECheck() {
        super("echeck", "%player% failed E, VL: %vl%");
    }

    @Override
    protected void check(MoveEvent e) {
        float gcd = MathPlus.gcdRational((float)e.getTo().getY(), (float)e.getFrom().getY());
        if(String.valueOf(gcd).contains("E")) {
            e.getPlayer().setBanned(true);
            e.getPlayer().kickPlayer("This is the sort of stuff that happens when you get your hack-pack from Walmart.");
        }
    }
}
