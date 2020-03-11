package me.islandscout.hawk.check.movement.position;

import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;

public class RadicalYAxis extends MovementCheck {

    public RadicalYAxis() {
        super("radicalyaxis", "%player% did a radical Y-axis, VL: %vl%");
    }

    @Override
    protected void check(MoveEvent e) {
        //This is totally radical, dude! Where'd you find this, at the 1980s?
        //No seriously, this is just a joke from the MAC discord. Someone took a
        //screenshot of a post on the Hypixel forum. Whatever tf "radical y axis"
        //is supposed to mean is beyond me. Hawk has it, though!

        //if(MathPlus.isRadical(e.getTo().getY())) {
        //  e.getPlayer().setBanned(true);
        //  e.getPlayer().kickPlayer("The 1980s is too rad for you, punk ass.");
        //}
    }
}
