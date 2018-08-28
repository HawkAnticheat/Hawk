package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class InteractEntityEvent extends Event {

    private InteractAction interactAction;
    private Entity entity;

    public InteractEntityEvent(Player p, HawkPlayer pp, InteractAction action, Entity entity) {
        super(p, pp);
        interactAction = action;
        this.entity = entity;
    }

    public InteractAction getInteractAction() {
        return interactAction;
    }

    public Entity getEntity() {
        return entity;
    }
}
