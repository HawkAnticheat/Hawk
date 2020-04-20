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

import me.islandscout.hawk.check.Cancelless;
import me.islandscout.hawk.check.Check;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChkinfoArgument extends Argument {

    ChkinfoArgument() {
        super("chkinfo", "<check>", "Displays information about specified check.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2)
            return false;
        for (Check check : hawk.getCheckManager().getChecks()) {
            if (check.getName().equalsIgnoreCase(args[1])) {
                sender.sendMessage(ChatColor.GOLD + "Basic information about check \"" + check.getName() + "\":");
                sender.sendMessage(ChatColor.GOLD + "Status: " + (check.isEnabled() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                sender.sendMessage(ChatColor.GOLD + "Category: " + check.getClass().getSuperclass().getSimpleName());
                sender.sendMessage(ChatColor.GOLD + "Cancel: " + (check instanceof Cancelless ? ChatColor.GRAY + "N/A" : ((check.canCancel() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"))));
                sender.sendMessage(ChatColor.GOLD + "Flag: " + ((check.canFlag() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED")));

                boolean bypass;
                if (sender instanceof Player) {
                    UUID uuid = ((Player) sender).getUniqueId();
                    bypass = !hawk.getCheckManager().getForcedPlayers().contains(uuid) && (sender.hasPermission(check.getBypassPermission()) || hawk.getCheckManager().getExemptedPlayers().contains(uuid));
                } else
                    bypass = true;

                sender.sendMessage(ChatColor.GOLD + "You " + (!bypass ? "do not " : "") + "have permission to bypass this check.");
                return true;
            }
        }
        sender.sendMessage(ChatColor.RED + "Unknown check \"" + args[1] + "\"");
        return true;
    }
}
