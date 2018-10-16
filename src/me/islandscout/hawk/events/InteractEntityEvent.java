package me.islandscout.hawk.events;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.packets.WrappedPacket;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class InteractEntityEvent extends Event {

    private final InteractAction interactAction;
    private final Entity entity;

    public InteractEntityEvent(Player p, HawkPlayer pp, InteractAction action, Entity entity, WrappedPacket packet) {
        super(p, pp, packet);
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
