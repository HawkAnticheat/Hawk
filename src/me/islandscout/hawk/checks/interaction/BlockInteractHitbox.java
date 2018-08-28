package me.islandscout.hawk.checks.interaction;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.AsyncBlockPlacementCheck;
import me.islandscout.hawk.events.BlockPlaceEvent;
import me.islandscout.hawk.utils.AABB;
import me.islandscout.hawk.utils.Placeholder;
import me.islandscout.hawk.utils.Ray;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BlockInteractHitbox extends AsyncBlockPlacementCheck {

    public BlockInteractHitbox() {
        super("blockinteracthitbox", "&7%player% failed block interact hitbox. %type% VL: %vl%");
    }

    @Override
    protected void check(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Location pLoc = pp.getLocation();
        Location targetLocation = e.getTargetBlockLocation();

        Vector min = targetLocation.toVector();
        Vector max = targetLocation.toVector().add(new Vector(1, 1, 1));
        AABB aabb = new AABB(min, max);

        Ray ray = new Ray(p.getEyeLocation().toVector(), pLoc.getDirection());

        Vector intersection = aabb.intersectsRay(ray, 0, 6);

        if(intersection == null) {
            punish(pp, true, e, new Placeholder("type", "Did not hit hitbox."));
        }
        else if(new Vector(intersection.getX() - pLoc.getX(), intersection.getY() - pLoc.getY(), intersection.getZ() - pLoc.getZ()).lengthSquared() > 36) {
            punish(pp, true, e, new Placeholder("type", "Reached too far."));
        }
        else {
            reward(pp);
        }
    }
}
