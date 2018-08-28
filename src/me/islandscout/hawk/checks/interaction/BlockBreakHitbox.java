package me.islandscout.hawk.checks.interaction;

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

    private final boolean DEBUG_HITBOX;
    private final boolean DEBUG_RAY;
    private final boolean CHECK_DIG_START;

    public BlockBreakHitbox() {
        super("blockbreakhitbox", "&7%player% failed block break hitbox. %type% VL: %vl%");
        DEBUG_HITBOX = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.blockbreakhitbox.debug.hitbox");
        DEBUG_RAY = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.blockbreakhitbox.debug.ray");
        CHECK_DIG_START = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.blockbreakhitbox.checkDigStart");
    }

    @Override
    protected void check(BlockDigEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Location eyeLoc = pp.getLocation().clone().add(0, 1.62, 0);
        Location bLoc = e.getBlock().getLocation();
        if(p.isSneaking())
            eyeLoc.add(0, -0.08, 0);

        //Extrapolate last position. (For 1.7 clients ONLY)
        //Unfortunately, there will be false positives from 1.7 users due to the nature of how the client interacts
        //with entities. There is no effective way to stop these false positives without creating bypasses.
        if(ServerUtils.getClientVersion(p) == 7) {
            Vector attackerVelocity = pp.getVelocity().clone();
            Vector attackerDeltaRotation = new Vector(pp.getDeltaYaw(), pp.getDeltaPitch(), 0);
            double moveDelay = System.currentTimeMillis() - pp.getLastMoveTime();
            if (moveDelay >= 100) {
                moveDelay = 0D;
            } else {
                moveDelay = moveDelay / 50;
            }
            attackerVelocity.multiply(moveDelay);
            attackerDeltaRotation.multiply(moveDelay);

            eyeLoc.add(attackerVelocity);

            eyeLoc.setYaw(eyeLoc.getYaw() + (float)attackerDeltaRotation.getX());
            eyeLoc.setPitch(eyeLoc.getPitch() + (float)attackerDeltaRotation.getY());
        }

        Vector min = bLoc.toVector();
        Vector max = bLoc.toVector().add(new Vector(1, 1, 1));
        AABB aabb = new AABB(min, max);

        Ray ray = new Ray(eyeLoc.toVector(), eyeLoc.getDirection());

        if(DEBUG_HITBOX)
            aabb.highlight(hawk, p.getWorld(), 0.25);
        if(DEBUG_RAY)
            ray.highlight(hawk, p.getWorld(), 6, 0.3);

        Vector intersection = aabb.intersectsRay(ray, 0, 6);

        if(intersection == null) {
            cancelDig(pp, e, new Placeholder("type", "Did not hit hitbox."));
        }
        else if(new Vector(intersection.getX() - eyeLoc.getX(), intersection.getY() - eyeLoc.getY(), intersection.getZ() - eyeLoc.getZ()).lengthSquared() > 36) {
            cancelDig(pp, e, new Placeholder("type", "Reached too far."));
        }
        else {
            reward(pp);
        }
    }

    private void cancelDig(HawkPlayer pp, BlockDigEvent e, Placeholder... placeholder) {
        if(pp.getPlayer().getGameMode() == GameMode.CREATIVE) {
            punishAndTryCancelAndBlockRespawn(pp, e, placeholder);
        }
        else if(e.getDigAction() == DigAction.COMPLETE) {
            punishAndTryCancelAndBlockRespawn(pp, e, placeholder);
        }
        else if(CHECK_DIG_START && e.getDigAction() == DigAction.START) {
            punish(pp, true, e, placeholder);
        }
    }
}
