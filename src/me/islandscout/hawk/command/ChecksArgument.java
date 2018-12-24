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

import java.util.ArrayList;
import java.util.List;

public class ChecksArgument extends Argument {

    ChecksArgument() {
        super("checks", "", "Provides a list of Hawk check.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        List<Check> checks = hawk.getCheckManager().getChecks();
        List<String> chkNames = new ArrayList<>();
        int enabled = 0;
        for (Check check : checks) {
            chkNames.add((check.isEnabled() ? ChatColor.GREEN : ChatColor.RED) + check.getName());
            if (check.isEnabled())
                enabled++;
        }
        sender.sendMessage(ChatColor.GOLD + "Checks (" + enabled + "/" + checks.size() + " enabled): " + String.join(", ", chkNames));
        return true;
    }
}
