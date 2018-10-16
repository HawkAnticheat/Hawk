package me.islandscout.hawk.checks.movement;

import me.islandscout.hawk.checks.MovementCheck;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.utils.Debug;

public class Bouncy extends MovementCheck {

    //Debug class

    public Bouncy() {
        super("bouncy", "foo");
    }

    private double maxYPos;
    private double velocityOnImpact;
    private boolean falling;

    @Override
    protected void check(PositionEvent event) {
        double deltaY = event.getTo().getY() - event.getFrom().getY();
        if (!event.isOnGround())
            velocityOnImpact = event.getTo().getY() - event.getFrom().getY();
        if (!falling && deltaY < 0) {
            falling = true;
            maxYPos = event.getFrom().getY();
        }
        if (falling && deltaY >= 0) {
            falling = false;
            Debug.broadcastMessage("BOUNCE VELOCITY: " + (event.getTo().getY() - event.getFrom().getY()));
            Debug.broadcastMessage("----");
        }
        if (event.isOnGround() && falling) {
            Debug.broadcastMessage("MAX Y: " + (maxYPos - event.getTo().getBlockY()));
            Debug.broadcastMessage("APPROX IMPACT VELOCITY: " + velocityOnImpact);
        }
    }
}
