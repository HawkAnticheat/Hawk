package me.islandscout.hawk.utils.entities;

import me.islandscout.hawk.utils.AABB;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class EntityNMS8 extends EntityNMS {

    public EntityNMS8(Entity entity) {
        super();
        AxisAlignedBB bb = ((CraftEntity) entity).getHandle().getBoundingBox();
        collisionBox = new AABB(new Vector(bb.a, bb.b, bb.c), new Vector(bb.d, bb.e, bb.f));
    }
}
