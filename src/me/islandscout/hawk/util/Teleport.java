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
import org.bukkit.util.Vector;

public class Teleport {

    private Cause cause; //We want to know whether this was caused by a movement setback.
    private long tick;
    private HawkPlayer pp;
    private Vector pos;
    private float yaw, pitch;

    //A player may be in the process of rubberbanding, but the packet hasn't been sent yet.
    // This info is important for the proper function of movement setbacks.
    private PacketStatus status;

    public Teleport(Cause cause, HawkPlayer pp, Vector pos, float yaw, float pitch) {
        this.cause = cause;
        this.pp = pp;
        this.tick = pp.getCurrentTick();
        this.pos = pos;
        this.yaw = yaw;
        this.pitch = pitch;
        this.status = cause == Cause.OTHER ? PacketStatus.PACKET_SENT : PacketStatus.PACKET_WAITING;
    }

    public Cause getCause() {
        return cause;
    }

    public HawkPlayer getHawkPlayer() {
        return pp;
    }

    public long getTick() {
        return tick;
    }

    public Vector getPos() {
        return pos;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public PacketStatus getStatus() {
        return status;
    }

    public void setStatus(PacketStatus status) {
        this.status = status;
    }

    public enum Cause {
        ANTICHEAT_RESYNC,
        OTHER
    }

    public enum PacketStatus {
        PACKET_WAITING,
        PACKET_SENT
    }

}
