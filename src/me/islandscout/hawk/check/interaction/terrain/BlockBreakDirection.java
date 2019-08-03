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

package me.islandscout.hawk.check.interaction.terrain;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.BlockDigCheck;
import me.islandscout.hawk.event.BlockDigEvent;
import me.islandscout.hawk.util.*;
import me.islandscout.hawk.util.block.BlockNMS;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class BlockBreakDirection extends BlockDigCheck {

    private final boolean DEBUG_HITBOX;
    private final boolean DEBUG_RAY;
    private final boolean CHECK_DIG_START;
    private final boolean CHECK_DIG_CANCEL;
    private final boolean CHECK_DIG_COMPLETE;

    public BlockBreakDirection() {
        super("blockbreakdirection", true, 5, 10, 0.9, 5000, "%player% failed block break direction, VL: %vl%", null);
        DEBUG_HITBOX = (boolean) customSetting("hitbox", "debug", false);
        DEBUG_RAY = (boolean) customSetting("ray", "debug", false);
        CHECK_DIG_START = (boolean) customSetting("checkDigStart", "", false);
        CHECK_DIG_CANCEL = (boolean) customSetting("checkDigCancel", "", false);
        CHECK_DIG_COMPLETE = (boolean) customSetting("checkDigComplete", "", true);
    }

    @Override
    protected void check(BlockDigEvent e) {
        Player p = e.getPlayer();
        HawkPlayer pp = e.getHawkPlayer();
        Location bLoc = e.getBlock().getLocation();
        Vector pos = pp.getPosition().clone().add(new Vector(0, pp.isSneaking() ? 1.54 : 1.62, 0));
        Vector dir = MathPlus.getDirection(pp.getYaw(), pp.getPitch());
        Vector extraDir = MathPlus.getDirection(pp.getYaw() + pp.getDeltaYaw(), pp.getPitch() + pp.getDeltaPitch());

        switch (e.getDigAction()) {
            case START:
                if(CHECK_DIG_START || (p.getGameMode() == GameMode.CREATIVE && CHECK_DIG_COMPLETE))
                    break;
                return;
            case CANCEL:
                if(CHECK_DIG_CANCEL)
                    break;
                return;
            case COMPLETE:
                if(CHECK_DIG_COMPLETE)
                    break;
                return;
        }

        Vector min = bLoc.toVector();
        Vector max = bLoc.toVector().add(new Vector(1, 1, 1));
        AABB targetAABB = new AABB(min, max);

        if (DEBUG_HITBOX)
            targetAABB.highlight(hawk, p.getWorld(), 0.25);
        if (DEBUG_RAY)
            new Ray(pos, extraDir).highlight(hawk, p.getWorld(), 6F, 0.3);

        if(targetAABB.betweenRays(pos, dir, extraDir)) {
            reward(pp);
        }
        else {
            punish(pp, true, e);
        }
    }
}
