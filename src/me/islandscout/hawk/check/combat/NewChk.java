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

package me.islandscout.hawk.check.combat;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.EntityInteractionCheck;
import me.islandscout.hawk.event.InteractEntityEvent;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.util.AABB;
import me.islandscout.hawk.util.Ray;
import me.islandscout.hawk.util.ServerUtils;
import me.islandscout.hawk.util.entity.EntityNMS;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class NewChk extends EntityInteractionCheck {

    public NewChk() {
        super("new0", "%player% failed entity interaction direction, VL %vl%");
    }

    @Override
    protected void check(InteractEntityEvent e) {
        HawkPlayer pp = e.getHawkPlayer(); //TODO check-other-entities config
        int ping = ServerUtils.getPing(e.getPlayer()); //TODO ping limit config
        Vector extraPos = pp.getPosition().clone().add(pp.getVelocity()); //TODO check for sneaking
        Vector dir = MathPlus.getDirection(pp.getYaw(), pp.getPitch());
        Vector extraDir = MathPlus.getDirection(pp.getYaw() + pp.getDeltaYaw(), pp.getPitch() + pp.getDeltaPitch()); //TODO You don't need to extrapolate yaw for 1.8

        Location victimLoc = hawk.getLagCompensator().getHistoryLocation(ping, e.getEntity()); //TODO don't forget to add config option for lag compensation
        AABB victimAABB = EntityNMS.getEntityNMS(e.getEntity()).getHitbox(victimLoc.toVector()); //TODO expand this a little bit due to precision error


        if(dir.dot(extraDir) < 0.001) {
            //Directions are very similar; do a simple ray check.
            if(victimAABB.intersectsRay(new Ray(extraPos, extraDir), 0, Float.MAX_VALUE) == null) {
                //check failed
                //STOP check
            }
        }

        else {

            //check if box even collides with plane
            Vector headPlaneNormal = extraDir.clone().crossProduct(dir);
            Vector[] vertecies = victimAABB.getVertices();
            boolean hitHeadPlane = false;
            boolean above = false;
            boolean below = false;
            for(Vector vertex : vertecies) {

                //Eh, what the hell. Let's move the vertices now.
                //Imagine moving everything in this system so that the plane
                //is at <0,0,0>. This will make it easier to compute stuff.
                vertex.subtract(extraPos);

                if(vertex.dot(headPlaneNormal) > 0) {
                    above = true;
                }
                else {
                    below = true;
                }
                if(above && below) {
                    hitHeadPlane = true;
                    break;
                }
            }

            if(!hitHeadPlane) {
                //check failed
                //STOP check
            }

            //check if box is between both vectors
            Vector extraDirToDirNormal = headPlaneNormal.clone().crossProduct(extraDir);
            Vector dirToExtraDirNormal = dir.clone().crossProduct(headPlaneNormal);
            boolean betweenVectors = false;
            boolean frontOfExtraDirToDir = false;
            boolean frontOfDirToExtraDir = false;
            for(Vector vertex : vertecies) {

                if(!frontOfExtraDirToDir && vertex.dot(extraDirToDirNormal) >= 0) {
                    frontOfExtraDirToDir = true;
                }
                if(!frontOfDirToExtraDir && vertex.dot(dirToExtraDirNormal) >= 0) {
                    frontOfDirToExtraDir = true;
                }

                if(frontOfExtraDirToDir && frontOfDirToExtraDir) {
                    betweenVectors = true;
                    break;
                }
            }

            if(!betweenVectors) {
                //check failed
                //STOP check
            }
        }
    }
}
