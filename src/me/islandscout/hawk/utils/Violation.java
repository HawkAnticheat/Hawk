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

package me.islandscout.hawk.utils;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.Check;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Violation {

    private final Player player;
    private final Check check;
    private final long time;
    private final short ping;
    private final short vl;
    private final float tps;
    private final String server;

    public Violation(HawkPlayer pp, Check check, short vl) {
        this.player = pp.getPlayer();
        this.check = check;
        this.time = System.currentTimeMillis();
        this.ping = (short) ServerUtils.getPing(pp.getPlayer());
        this.vl = vl;
        this.tps = (float) ServerUtils.getTps();
        this.server = Bukkit.getServerName();
    }

    public Player getPlayer() {
        return player;
    }

    public Check getCheck() {
        return check;
    }

    public long getTime() {
        return time;
    }

    public short getPing() {
        return ping;
    }

    public short getVl() {
        return vl;
    }

    public String getServer() {
        return server;
    }

    public float getTps() {
        return tps;
    }
}
