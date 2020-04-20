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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.islandscout.hawk.Hawk;
import org.bukkit.Bukkit;

public class BungeeBridge {

    private final Hawk hawk;
    private final boolean enabled;

    public BungeeBridge(Hawk hawk, boolean enabled) {
        this.hawk = hawk;
        if (enabled)
            hawk.getServer().getMessenger().registerOutgoingPluginChannel(hawk, "BungeeCord");
        this.enabled = enabled;
    }

    public void sendAlertForBroadcast(String msg) {
        if (!enabled)
            return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("HawkACAlert");
        out.writeUTF(msg);
        Bukkit.getServer().sendPluginMessage(hawk, "BungeeCord", out.toByteArray());
    }

}
