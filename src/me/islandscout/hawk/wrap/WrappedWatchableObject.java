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

package me.islandscout.hawk.wrap;

/**
 * A wrapper for Minecraft's WatchableObject class
 */
public class WrappedWatchableObject {

    private final int objectType;
    private final int dataValueId;
    private Object watchedObject;
    private boolean watched;

    public WrappedWatchableObject(int type, int id, Object object) {
        this.dataValueId = id;
        this.watchedObject = object;
        this.objectType = type;
        this.watched = true;
    }

    public int getIndex() {
        return this.dataValueId;
    }

    public void setObject(Object object) {
        this.watchedObject = object;
    }

    public Object getObject() {
        return this.watchedObject;
    }

    public int getObjectType() {
        return this.objectType;
    }

    public boolean isWatched() {
        return this.watched;
    }

    public void setWatched(boolean status) {
        this.watched = status;
    }
}
