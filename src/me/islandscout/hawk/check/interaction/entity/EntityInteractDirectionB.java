/*
 * This file is part of Hawk Anticheat.
 * Copyright (C) 2018 Hawk Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check.interaction.entity;

import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.InteractAction;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityInteractDirectionB extends CustomCheck {

    //In 1.8, a new Vec3D field was added to the PacketPlayInUseEntity packet.
    //It is always null when attacking an entity. It represents the ray-trace
    //intersection position on the hit-box.

    //Strangely, most of the time, two PacketPlayInUseEntity packets are sent
    //during an entity interaction (right click): one with a useful Vec3D and
    //one with a null Vec3D. This appears to be a bug in the game.

    private Map<UUID, Vector> lastHitVecMap;

    public EntityInteractDirectionB() {
        super("lol", "%player% is hacking");
        lastHitVecMap = new HashMap<>();
    }

    @Override
    protected void check(Event e) {
        if(e instanceof MoveEvent) {
            processMove((MoveEvent) e);
        }
        else if(e instanceof InteractEntityEvent) {
            processHit((InteractEntityEvent) e);
        }
    }

    private void processHit(InteractEntityEvent e) {
        if(e.getInteractAction() == InteractAction.INTERACT) {
            Vector hitVec = e.getIntersectVector();
            if(hitVec != null) {
                hitVec = hitVec.clone();
                hitVec.add(hawk.getLagCompensator().getHistoryLocation(ServerUtils.getPing(e.getPlayer()), e.getEntity()).toVector());
                lastHitVecMap.put(e.getPlayer().getUniqueId(), hitVec);
            }
        }
    }

    private void processMove(MoveEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if(lastHitVecMap.containsKey(uuid)) {
            Vector headPos = e.getHawkPlayer().getHeadPosition();
            Vector dirA = MathPlus.getDirection(e.getFrom().getYaw(), e.getTo().getPitch());
            Vector dirB = lastHitVecMap.get(uuid).subtract(headPos).normalize();
            //dirA.dot(dirB) should be close to 1.0
        }

        lastHitVecMap.remove(uuid);
    }
}
