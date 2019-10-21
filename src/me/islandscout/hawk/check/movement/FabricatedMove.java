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

package me.islandscout.hawk.check.movement;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.Debug;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.wrap.packet.WrappedPacket;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/*
 * Check written by Havesta
 *
 * FabricatedMove checks if a (C05 packet) or a (C06 packet with updated POSITION) is sent by the client
 * (C06 = position and rotation changed, C04 = position changed, C05 = rotation changed)
 *  with the same rotation as he had before... that means that the player
 *  A: got teleported
 *  or B: is cheating and just cancelling packets and sending new ones
 *  (often used in killaura, scaffold, tower and autopotion)
 *  inspired by HeroCode: https://www.youtube.com/watch?v=3MN9EkPjOZ0
 */
public class FabricatedMove extends MovementCheck {

    private final Map<UUID, Integer> flyingTicksMap;

    public FabricatedMove() {
        super("fabricatedmove", true, 0, 2, 0.999, 5000, "%player% failed fabricated move, VL: %vl%", null);
        flyingTicksMap = new HashMap<>();
    }

    //Verify that the distance between two succeeding move packets is greater than 0.03

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();

        if(pp.getPlayer().isInsideVehicle() || e.hasTeleported()) {
            flyingTicksMap.put(pp.getUuid(), 0);
            return;
        }

        //Also ignore some moves after the tp to fix false positives.
        //Use ping because if you send multiple TPs with the same location,
        //and if the player stands still, hawk will register the first one
        //accepted in the batch and ignore the succeeding ones. Add 10 ticks
        //just to be safe. - Islandscout
        if(pp.getCurrentTick() - pp.getLastTeleportAcceptTick() > ServerUtils.getPing(e.getPlayer()) / 50 + 10 &&
                pp.getCurrentTick() > 100) {

            WrappedPacket packet = e.getWrappedPacket();
            switch(packet.getType()) {
                case POSITION:
                    //We can extend the check to position packets by checking
                    //the velocity since the last flying packet. - Islandscout
                    if(!e.hasDeltaPos() && pp.getVelocity().lengthSquared() > 0) {
                        punishAndTryRubberband(e.getHawkPlayer(), e, e.getPlayer().getLocation());
                    } else {
                        reward(e.getHawkPlayer());
                    }
                    break;
                case LOOK:
                    if(!e.hasDeltaRot()) {
                        punishAndTryRubberband(e.getHawkPlayer(), e, e.getPlayer().getLocation());
                    } else {
                        reward(e.getHawkPlayer());
                    }
                    break;
                case POSITION_LOOK:
                    if(!e.hasDeltaRot() || (!e.hasDeltaPos() && pp.getVelocity().lengthSquared() > 0)) {
                        punishAndTryRubberband(e.getHawkPlayer(), e, e.getPlayer().getLocation());
                    } else {
                        reward(e.getHawkPlayer());
                    }
                    break;
            }

            UUID uuid = pp.getUuid();
            int flying = flyingTicksMap.getOrDefault(uuid, 0);
            if(!e.isUpdatePos()) {
                flying++;
            }
            else {
                flying = 0;
            }
            flyingTicksMap.put(uuid, flying);

            if(flying > 20)  // inspired by ToonBasic
                punishAndTryRubberband(pp, flying - 20, e, e.getPlayer().getLocation());
        }
    }

    @Override
    public void removeData(Player p) {
        flyingTicksMap.remove(p.getUniqueId());
    }
}
