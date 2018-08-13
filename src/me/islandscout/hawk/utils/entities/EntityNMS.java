package me.islandscout.hawk.utils.entities;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.utils.AABB;
import org.bukkit.entity.Entity;

public abstract class EntityNMS {

    protected Entity entity;
    protected AABB aabb;

    EntityNMS(Entity entity) {
        this.entity = entity;
    }

    public static EntityNMS getEntityNMS(Entity entity) {
        if(Hawk.getServerVersion() == 8)
            return new EntityNMS8(entity);
        else
            return new EntityNMS7(entity);
    }


    public AABB getCollisionBox() {
        return aabb;
    }
}
