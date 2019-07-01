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

package me.islandscout.hawk.util;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.Set;

/**
 * ClientBlocks allow Hawk to track the placement of blocks
 * before they get processed by the server. This is very
 * useful for mitigating client-server synchronization delay
 * caused by the server taking nearly up to 100ms to process
 * a block. This should especially be used in movement
 * checks to significantly reduce annoying false-positives,
 * but caution is advised since ClientBlocks may be spoofed.
 */
public class ClientBlock {

    public static final long CLIENTTICKS_UNTIL_EXPIRE = 5;
    public static final int MAX_PER_PLAYER = 16;
    private final Material material;
    private final long initTick;

    public ClientBlock(long clientTick, Material material) {
        this.material = material;
        initTick = clientTick;
    }

    public Material getMaterial() {
        return material;
    }

    public long getInitTick() {
        return initTick;
    }
}
