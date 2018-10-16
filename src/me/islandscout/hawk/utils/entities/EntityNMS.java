package me.islandscout.hawk.utils.entities;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.utils.AABB;
import org.bukkit.entity.Entity;

public abstract class EntityNMS {

    AABB collisionBox;
    protected int id;
    //hitbox appears to grow 0.1249 per side. verify?

    EntityNMS() {
    }

    public static EntityNMS getEntityNMS(Entity entity) {
        if (Hawk.getServerVersion() == 8)
            return new EntityNMS8(entity);
        else
            return new EntityNMS7(entity);
    }


    public AABB getCollisionBox() {
        return collisionBox;
    }
}
