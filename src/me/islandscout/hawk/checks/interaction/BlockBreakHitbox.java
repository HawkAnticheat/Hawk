package me.islandscout.hawk.checks.interaction;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncBlockDigCheck;
import me.islandscout.hawk.events.DigAction;
import me.islandscout.hawk.events.BlockDigEvent;
import me.islandscout.hawk.utils.*;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BlockBreakHitbox extends AsyncBlockDigCheck {

    public BlockBreakHitbox(Hawk hawk) {
        super(hawk, "blockbreakhitbox", "&7%player% failed block break hitbox. %type% VL: %vl%");
    }

    @Override
    protected void check(BlockDigEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = hawk.getHawkPlayer(p);
        Location pLoc = pp.getLocation();
        Location bLoc = e.getBlock().getLocation();

        Vector min = bLoc.toVector();
        Vector max = bLoc.toVector().add(new Vector(1, 1, 1));
        AABB aabb = new AABB(min, max);

        Ray ray = new Ray(p.getEyeLocation().toVector(), pLoc.getDirection());

        Vector intersection = aabb.intersectsRay(ray, 0, 6);

        if(intersection == null) {
            cancelDig(p, e, new Placeholder("type", "Did not hit hitbox."));
        }
        else if(new Vector(intersection.getX() - pLoc.getX(), intersection.getY() - pLoc.getY(), intersection.getZ() - pLoc.getZ()).lengthSquared() > 36) {
            cancelDig(p, e, new Placeholder("type", "Reached too far."));
        }
        else {
            reward(p);
        }
    }

    private void cancelDig(Player p, BlockDigEvent e, Placeholder... placeholder) {
        if(p.getGameMode() == GameMode.CREATIVE) {
            punishAndTryCancelAndBlockRespawn(p, e, placeholder);
        }
        else if(e.getDigAction() == DigAction.COMPLETE) {
            punishAndTryCancelAndBlockRespawn(p, e, placeholder);
        }
        else if(e.getDigAction() == DigAction.START) {
            punishAndTryCancel(p, e, placeholder);
        }
    }
}
