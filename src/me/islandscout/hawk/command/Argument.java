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

package me.islandscout.hawk.command;

import me.islandscout.hawk.Hawk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

abstract class Argument implements Comparable<Argument> {
    private final String name;
    private final String description;
    private final String syntax;
    static Hawk hawk;

    Argument(String name, String syntax, String description) {
        this.name = name;
        this.description = description;
        this.syntax = syntax;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return name + (syntax.length() == 0 ? "" : " " + syntax);
    }

    public abstract boolean process(CommandSender sender, Command cmd, String label, String[] args);

    @Override
    public int compareTo(Argument other) {
        return name.compareTo(other.name);
    }
}
