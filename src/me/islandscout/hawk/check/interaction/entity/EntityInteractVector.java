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

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.CustomCheck;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.InteractAction;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.ServerUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityInteractVector extends CustomCheck {

    //In 1.8, right-clicking an entity will send an entity use packet
    // with the vector of the hit scan result relative to the hitbox.
    // With this information, we can determine and enforce at which side
    // of the box the player is interacting.
    //In combination with an attack packet on the same tick, we can
    // also determine at which side of the box the player is attacking.
    // Furthermore, with the player's look vector, we can
    // can know where inside the box the player is trying to aim,
    // potentially exposing aim assistance cheats. With some assistance
    // with opponent location history and the player's current position,
    // we can get a decent approximation of where the opponent is on the
    // player's screen when they hit.

    //interact is always after attack in the client tick

    //If you interact while attacking, you MUST have a hit vec.

    private Map<UUID, Vector> lastHitVecMap;

    public EntityInteractVector() {
        super("lol2", "%player% is hacking");
        this.lastHitVecMap = new HashMap<>();
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
            if(hitVec != null) { //TODO In vanilla this will always be true if player attacked on same tick. Enforce this.

                HawkPlayer pp = e.getHawkPlayer();

                lastHitVecMap.put(e.getPlayer().getUniqueId(), hitVec);

            }
        }
    }

    private void processMove(MoveEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        if(lastHitVecMap.containsKey(uuid)) {

            Vector headPos = e.getHawkPlayer().getHeadPosition();
            Vector headDir = MathPlus.getDirection(e.getFrom().getYaw(), e.getTo().getPitch());

            //TODO verify that hitVec is on a visible side of the box

            //At this point, all possible positions of the victim reduces to a line. We will use the lag compensator
            // to pick the pair of historical positions with a lerp path closest to the line. By projecting the position
            // on the lerp closest to the line onto the line, we reveal the approximate position of the victim on the
            // player's screen.

            //TODO if we cannot find a reasonable position, flag for direction

            //We most definitely won't get an interaction every tick, so we won't have enough data to do linear
            // regression on angle deltas. However, we can still do stats on angle error distribution.

            //TODO angle error distribution
        }

        lastHitVecMap.remove(uuid);
    }
}
