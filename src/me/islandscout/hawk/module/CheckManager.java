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

package me.islandscout.hawk.module;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.check.*;
import me.islandscout.hawk.check.combat.*;
import me.islandscout.hawk.check.interaction.*;
import me.islandscout.hawk.check.interaction.entity.EntityInteractDirection;
import me.islandscout.hawk.check.interaction.entity.EntityInteractReach;
import me.islandscout.hawk.check.interaction.inventory.InventoryActions;
import me.islandscout.hawk.check.interaction.item.ItemSwitchSpeed;
import me.islandscout.hawk.check.interaction.item.ItemUseSpeed;
import me.islandscout.hawk.check.interaction.terrain.*;
import me.islandscout.hawk.check.movement.*;
import me.islandscout.hawk.check.movement.look.*;
import me.islandscout.hawk.check.movement.position.*;
import me.islandscout.hawk.check.tick.TickRate;
import me.islandscout.hawk.event.*;
import org.bukkit.entity.Player;

import java.util.*;

public class CheckManager {

    private final Set<UUID> exemptedPlayers;
    private final Set<UUID> forcedPlayers;

    //make these HashSets?
    private final List<Check> checks;
    private final List<BlockDigCheck> blockDigChecks;
    private final List<BlockInteractionCheck> blockInteractionChecks;
    private final List<CustomCheck> customChecks;
    private final List<EntityInteractionCheck> entityInteractionChecks;
    private final List<MovementCheck> movementChecks;

    public CheckManager(Hawk hawk) {
        Check.setHawkReference(hawk);
        exemptedPlayers = new HashSet<>();
        forcedPlayers = new HashSet<>();
        checks = new ArrayList<>();
        blockDigChecks = new ArrayList<>();
        blockInteractionChecks = new ArrayList<>();
        customChecks = new ArrayList<>();
        entityInteractionChecks = new ArrayList<>();
        movementChecks = new ArrayList<>();
    }

    //initialize checks
    public void loadChecks() {
        new TickRate();
        new FightHitbox();
        new Phase();
        new NoClip();
        //new FlyOld();
        new Fly();
        new Step();
        new BlockBreakSpeedSurvival();
        new Inertia();
        new BlockBreakDirection();
        new WrongBlock();
        new GroundSpoof();
        new FightSpeed();
        new FightAccuracy();
        new AimbotHeuristic();
        new FightNoSwing();
        new AntiVelocityBasic();
        new AntiVelocityJump();
        new InvalidPitch();
        new EntityInteractReach();
        new EntityInteractDirection();
        new BlockInteractDirection();
        new BlockInteractOcclusion();
        new BlockInteractSpeed();
        new WrongBlockFace();
        new InvalidPlacement();
        new ItemSwitchSpeed();
        new ActionToggleSpeed();
        new Speed();
        //new SmallHop();
        //new FastFall();
        new MultiAction();
        new SprintDirection();
        //new SwimVertical();
        new ClickDuration();
        new FightSpeedConsistency();
        new AimbotPrecision();
        new ItemUseSpeed();
        new FightSynchronized();
        new FightMulti();
        new AimbotConvergence();
        new Strafe();
        new FabricatedMove();
        new FabricatedBlockInteract();
        //new InventoryMove();
        new InventoryActions();
        //new AimbotExperimental();
    }

    public void unloadChecks() {
        checks.clear();
        blockDigChecks.clear();
        blockInteractionChecks.clear();
        customChecks.clear();
        entityInteractionChecks.clear();
        movementChecks.clear();
    }

    //iterate through appropriate checks
    void dispatchEvent(Event e) {
        for (CustomCheck check : customChecks) {
            check.checkEvent(e);
        }
        if (e instanceof MoveEvent) {
            for (MovementCheck check : movementChecks)
                check.checkEvent((MoveEvent) e);
        } else if (e instanceof InteractEntityEvent) {
            for (EntityInteractionCheck check : entityInteractionChecks)
                check.checkEvent((InteractEntityEvent) e);
        } else if (e instanceof BlockDigEvent) {
            for (BlockDigCheck check : blockDigChecks)
                check.checkEvent((BlockDigEvent) e);
        } else if (e instanceof InteractWorldEvent) {
            for (BlockInteractionCheck check : blockInteractionChecks)
                check.checkEvent((InteractWorldEvent) e);
        }
    }

    public void removeData(Player p) {
        for (Check check : checks)
            check.removeData(p);
    }

    public List<Check> getChecks() {
        return checks;
    }

    public List<BlockDigCheck> getBlockDigChecks() {
        return blockDigChecks;
    }

    public List<BlockInteractionCheck> getBlockInteractionChecks() {
        return blockInteractionChecks;
    }

    public List<CustomCheck> getCustomChecks() {
        return customChecks;
    }

    public List<EntityInteractionCheck> getEntityInteractionChecks() {
        return entityInteractionChecks;
    }

    public List<MovementCheck> getMovementChecks() {
        return movementChecks;
    }

    public Set<UUID> getExemptedPlayers() {
        return exemptedPlayers;
    }

    public Set<UUID> getForcedPlayers() {
        return forcedPlayers;
    }

    public void addExemption(UUID uuid) {
        forcedPlayers.remove(uuid);
        exemptedPlayers.add(uuid);
    }

    public void addForced(UUID uuid) {
        exemptedPlayers.remove(uuid);
        forcedPlayers.add(uuid);
    }

    public void removeExemption(UUID uuid) {
        exemptedPlayers.remove(uuid);
    }

    public void removeForced(UUID uuid) {
        forcedPlayers.remove(uuid);
    }
}
