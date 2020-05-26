package me.islandscout.hawk.check.movement.position;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;

public class Step extends MovementCheck {

    public Step() {
        super("step", true, 0, 5, 0.995, 5000, "&7%player% failed step, VL: %vl%", null);
    }

    @Override
    protected void check(MoveEvent e) {

        if(e.isStep() || !(e.isOnGround() && e.getHawkPlayer().isOnGround()) || e.hasTeleported() || e.hasAcceptedKnockback()) {
            return;
        }

        HawkPlayer pp = e.getHawkPlayer();
        double dY = e.getTo().getY() - e.getFrom().getY();

        if(dY > 0.6 || dY < -0.0784) {
            punishAndTryRubberband(pp, e);
        }
        else {
            reward(pp);
        }
    }
}
