package me.islandscout.hawk.utils.entities;

import me.islandscout.hawk.utils.AABB;
import net.minecraft.server.v1_7_R4.AxisAlignedBB;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class EntityNMS7 extends EntityNMS {

    public EntityNMS7(Entity entity) {
        super();
        AxisAlignedBB bb = ((CraftEntity) entity).getHandle().boundingBox;
        Vector min;
        Vector max;
        if (bb != null) {
            min = new Vector(bb.a, bb.b, bb.c);
            max = new Vector(bb.d, bb.e, bb.f);
        } else {
            min = new Vector(0, 0, 0);
            max = new Vector(0, 0, 0);
        }
        collisionBox = new AABB(min, max);
    }
}
