package me.islandscout.hawk.utils.entities;

import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;

public class EntityNMS8 extends EntityNMS {

    public EntityNMS8(Entity entity) {
        super(entity);
        AxisAlignedBB bb = ((CraftEntity)entity).getHandle().getBoundingBox();
        //TODO: work on this
    }
}
