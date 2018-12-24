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

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.check.MovementCheck;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.util.AdjacentBlocks;
import me.islandscout.hawk.util.ConfigHelper;
import me.islandscout.hawk.util.packet.WrappedPacket;
import org.bukkit.Location;

public class GroundSpoof extends MovementCheck {

    //PASSED (9/13/18)

    private final boolean STRICT;
    private final boolean PREVENT_NOFALL;

    public GroundSpoof() {
        super("groundspoof", true, -1, 3, 0.995, 5000, "%player% failed ground spoof. VL: %vl%", null);
        STRICT = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "checks.groundspoof.strict");
        PREVENT_NOFALL = ConfigHelper.getOrSetDefault(true, hawk.getConfig(), "checks.groundspoof.preventNoFall");
    }

    @Override
    protected void check(PositionEvent event) {
        if (!event.isOnGroundReally()) {
            if (event.isOnGround()) {

                //This tolerance allows for a bypass (which is caught by other movement checks).
                //Set STRICT to true to patch the bypass, but to also create more false positives.
                //Unfortunately, this issue is caused by how movement works in Minecraft, and cannot be fixed easily.
                Location checkLoc = event.getFrom().clone();
                checkLoc.setY(event.getTo().getY());
                if(!STRICT && AdjacentBlocks.onGroundReally(checkLoc, -1, false))
                    return;

                if (event.isOnClientBlock() == null) {
                    punishAndTryRubberband(event.getHawkPlayer(), event, event.getPlayer().getLocation());
                    if (PREVENT_NOFALL)
                        setNotOnGround(event);
                }

            } else {
                reward(event.getHawkPlayer());
            }
        }
    }

    //TODO: hmm... perhaps do this after all checks??? (prevent any conflicts from happening)
    private void setNotOnGround(PositionEvent e) {
        WrappedPacket packet = e.getWrappedPacket();
        if (Hawk.getServerVersion() == 7) {
            switch (packet.getType()) {
                case FLYING:
                    packet.setByte(0, 0);
                    return;
                case POSITION:
                    packet.setByte(32, 0);
                    return;
                case LOOK:
                    packet.setByte(8, 0);
                    return;
                case POSITION_LOOK:
                    packet.setByte(40, 0);
            }
        } else {
            switch (packet.getType()) {
                case FLYING:
                    packet.setByte(0, 0);
                    return;
                case POSITION:
                    packet.setByte(24, 0);
                    return;
                case LOOK:
                    packet.setByte(8, 0);
                    return;
                case POSITION_LOOK:
                    packet.setByte(32, 0);
            }
        }
    }
}
