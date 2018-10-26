/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.module;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.check.*;
import me.islandscout.hawk.check.combat.*;
import me.islandscout.hawk.check.interaction.*;
import me.islandscout.hawk.check.movement.*;
import me.islandscout.hawk.event.*;
import org.bukkit.entity.Player;

import java.util.*;

public class CheckManager {

    //I thought about making a new thread to eliminate some load from the NIO thread, but I'd run into concurrency issues.
    //I'd have to worry about cancelling, packet modding, and concurrent HawkPlayer data (race condition). It's not worth the trouble.

    private final Hawk hawk;

    private final Set<UUID> exemptedPlayers;

    //make these HashSets?
    private final List<Check> checks;
    private final List<BlockDigCheck> blockDigChecks;
    private final List<BlockPlacementCheck> blockPlacementChecks;
    private final List<CustomCheck> customChecks;
    private final List<EntityInteractionCheck> entityInteractionChecks;
    private final List<MovementCheck> movementChecks;

    public CheckManager(Hawk hawk) {
        this.hawk = hawk;
        Check.setHawkReference(hawk);
        exemptedPlayers = new HashSet<>();
        checks = new ArrayList<>();
        blockDigChecks = new ArrayList<>();
        blockPlacementChecks = new ArrayList<>();
        customChecks = new ArrayList<>();
        entityInteractionChecks = new ArrayList<>();
        movementChecks = new ArrayList<>();
    }

    //initialize checks and save any changes in configs to files.
    //can be used to reload checks
    public void loadChecks() {
        unloadChecks();

        new FightHitbox();
        new FightCriticals();
        new Phase();
        new Fly();
        new BlockBreakSpeed();
        new ClockSpeed();
        new Speed();
        new FightSynchronized();
        new Inertia();
        new BlockBreakHitbox();
        new WrongBlock();
        new LiquidExit();
        new GroundSpoof();
        new FightSpeed();
        new FightAccuracy();
        new FightAimbot();
        new FightNoSwing();
        new AntiVelocity();
        new InvalidPitch();
        new FightReachApprox();
        new FightDirectionApprox();
        new BlockInteractHitbox();
        new BlockInteractSpeed();
        new WrongBlockFace();
        new ImpossiblePlacement();
        //new SpeedRewrite();

        hawk.saveConfigs();
    }

    private void unloadChecks() {
        checks.clear();
        blockDigChecks.clear();
        blockPlacementChecks.clear();
        customChecks.clear();
        entityInteractionChecks.clear();
        movementChecks.clear();
    }

    //iterate through appropriate checks
    void dispatchEvent(Event e) {
        for (CustomCheck check : customChecks) {
            check.checkEvent(e);
        }
        if (e instanceof PositionEvent) {
            for (MovementCheck check : movementChecks)
                check.checkEvent((PositionEvent) e);
        } else if (e instanceof InteractEntityEvent) {
            for (EntityInteractionCheck check : entityInteractionChecks)
                check.checkEvent((InteractEntityEvent) e);
        } else if (e instanceof BlockDigEvent) {
            for (BlockDigCheck check : blockDigChecks)
                check.checkEvent((BlockDigEvent) e);
        } else if (e instanceof BlockPlaceEvent) {
            for (BlockPlacementCheck check : blockPlacementChecks)
                check.checkEvent((BlockPlaceEvent) e);
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

    public List<BlockPlacementCheck> getBlockPlacementChecks() {
        return blockPlacementChecks;
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
}
