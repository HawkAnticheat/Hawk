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

import me.islandscout.hawk.check.Check;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class UnfilteredFlagsArgument extends Argument {

    UnfilteredFlagsArgument() {
        super("unfilteredflags", "", "Send flag on any violation. Must reload Hawk to revert!");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        List<Check> checks = hawk.getCheckManager().getChecks();
        for(Check check : checks) {
            check.setFlagThreshold(0);
            check.setFlagCooldown(0);
        }
        sender.sendMessage(ChatColor.GOLD + "Players will now send flags on any violation. You must reload Hawk to revert this!");
        return true;
    }
}
