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

import me.islandscout.hawk.check.BlockInteractionCheck;
import me.islandscout.hawk.event.InteractWorldEvent;
import org.bukkit.util.Vector;

public class SyntheticBlockInteract extends BlockInteractionCheck {

    public SyntheticBlockInteract() {
        super("syntheticblockinteract", true, 0, 2, 0.999, 5000, "%player% failed synthetic-block-interact, VL: %vl%", null);
    }

    @Override
    protected void check(InteractWorldEvent e) {

        if(e.getTargetedBlockFace() == InteractWorldEvent.BlockFace.INVALID) {
            fail(e);
            return;
        }

        Vector cursorPos = e.getCursorPositionOnTargetedBlock();
        //Disable these for now; currently there is no efficient way to get block hitboxes.
        //boolean onFace = false;
        for(double value : new double[] {cursorPos.getX(), cursorPos.getY(), cursorPos.getZ()}) {
            if(value % 0.0625 != 0 || value > 1 || value < 0) {
                fail(e);
                return;
            }
            /*
            if(value == 0 || value == 1)
                onFace = true;
            */
        }

        /*
        if(!onFace) {
            fail(e);
            return;
        }
        */

        /*
        Vector cursorPosOffset = cursorPos.clone().subtract(new Vector(0.5, 0.5, 0.5)).multiply(2);
        Vector normal = e.getTargetedBlockFaceNormal();
        double[] cursorPosOffsetArray = new double[] {cursorPosOffset.getX(), cursorPosOffset.getY(), cursorPosOffset.getZ()};
        double[] normalArray = new double[] {normal.getX(), normal.getY(), normal.getZ()};
        boolean onCorrectFace = false;
        for(int i = 0; i < 3; i ++) {
            double normalComponent = normalArray[i];
            double cursorPosComponent = cursorPosOffsetArray[i];
            if(normalComponent == cursorPosComponent) {
                onCorrectFace = true;
                break;
            }
        }

        if(!onCorrectFace) {
            fail(e);
            return;
        }
        */

        reward(e.getHawkPlayer());

    }

    private void fail(InteractWorldEvent e) {
        punishAndTryCancelAndBlockRespawn(e.getHawkPlayer(), e);
    }
}
