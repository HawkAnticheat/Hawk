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

package me.islandscout.hawk.check.movement.position;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.MoveEvent;
import me.islandscout.hawk.util.Direction;
import me.islandscout.hawk.util.MathPlus;
import me.islandscout.hawk.wrap.entity.WrappedEntity;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class SprintDirection extends MovementCheck {

    //TODO If jump and dY is 0, that means they POSSIBLY could have jumped; not always. eg: SprintDirection falses with extreme negative jump boost or jumping into a 2-block-high space.

    private Map<UUID, Long> lastSprintTickMap;
    private Set<UUID> collisionHorizontalSet;

    public SprintDirection() {
        super("sprintdirection", true, 5, 5, 0.999, 5000, "%player% failed sprint direction, VL %vl%", null);
        lastSprintTickMap = new HashMap<>();
        collisionHorizontalSet = new HashSet<>();
    }

    @Override
    protected void check(MoveEvent e) {
        HawkPlayer pp = e.getHawkPlayer();

        boolean collisionHorizontal = collidingHorizontally(e);
        Vector moveHoriz = e.getTo().toVector().subtract(e.getFrom().toVector()).setY(0);

        if(!pp.isSprinting())
            lastSprintTickMap.put(pp.getUuid(), pp.getCurrentTick());

        Set<Material> collidedMats = WrappedEntity.getWrappedEntity(e.getPlayer()).getCollisionBox(e.getFrom().toVector()).getMaterials(pp.getWorld());
        if(pp.isSwimming() || e.isTeleportAccept() || e.hasAcceptedKnockback() ||
                (collisionHorizontal && !collisionHorizontalSet.contains(pp.getUuid())) ||
                pp.getCurrentTick() - lastSprintTickMap.getOrDefault(pp.getUuid(), pp.getCurrentTick()) < 2 ||
                moveHoriz.lengthSquared() < 0.04 || collidedMats.contains(Material.LADDER) ||
                collidedMats.contains(Material.VINE)) {
            return;
        }

        float yaw = e.getTo().getYaw();
        Vector prevVelocity = pp.getVelocity().clone();
        if(e.hasHitSlowdown()) {
            prevVelocity.multiply(0.6);
        }
        double dX = e.getTo().getX() - e.getFrom().getX();
        double dZ = e.getTo().getZ() - e.getFrom().getZ();
        float friction = e.getFriction();
        dX /= friction;
        dZ /= friction;
        if(e.isJump()) {
            float yawRadians = yaw * 0.017453292F;
            dX += (MathPlus.sin(yawRadians) * 0.2F);
            dZ -= (MathPlus.cos(yawRadians) * 0.2F);
        }

        //Div by 1.7948708571637845???? What the hell are these numbers?

        dX -= prevVelocity.getX();
        dZ -= prevVelocity.getZ();

        Vector moveForce = new Vector(dX, 0, dZ);
        Vector yawVec = MathPlus.getDirection(yaw, 0);

        if(MathPlus.angle(yawVec, moveForce) > Math.PI / 4 + 0.3) { //0.3 is arbitrary. Prevents falses due to silly stuff in game
            punishAndTryRubberband(pp, e);
        }
        else {
            reward(pp);
        }

        if(collisionHorizontal)
            collisionHorizontalSet.add(pp.getUuid());
        else
            collisionHorizontalSet.remove(pp.getUuid());
    }

    private boolean collidingHorizontally(MoveEvent e) {
        for(Direction dir : e.getBoxSidesTouchingBlocks()) {
            if(dir == Direction.EAST || dir == Direction.NORTH || dir == Direction.SOUTH || dir == Direction.WEST)
                return true;
        }
        return false;
    }

    @Override
    public void removeData(Player p) {
        lastSprintTickMap.remove(p.getUniqueId());
        collisionHorizontalSet.remove(p.getUniqueId());
    }
}
