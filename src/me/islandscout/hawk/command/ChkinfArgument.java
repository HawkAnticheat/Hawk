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

package me.islandscout.hawk.command;

import me.islandscout.hawk.checks.Cancelless;
import me.islandscout.hawk.checks.Check;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChkinfArgument extends Argument {

    ChkinfArgument() {
        super("chkinfo", "<check>", "Displays information about specified check.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2)
            return false;
        for (Check check : hawk.getCheckManager().getChecks()) {
            if (check.getName().equalsIgnoreCase(args[1])) {
                sender.sendMessage(ChatColor.GOLD + "Basic information about check \"" + check.getName() + "\":");
                sender.sendMessage(ChatColor.GOLD + "ID: " + check.getId());
                sender.sendMessage(ChatColor.GOLD + "Status: " + (check.isEnabled() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                sender.sendMessage(ChatColor.GOLD + "Category: " + check.getClass().getSuperclass().getSimpleName());
                sender.sendMessage(ChatColor.GOLD + "Cancel: " + (check instanceof Cancelless ? ChatColor.GRAY + "N/A" : ((check.canCancel() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"))));
                sender.sendMessage(ChatColor.GOLD + "Flag: " + ((check.canFlag() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED")));

                boolean bypass = sender.hasPermission(check.getBypassPermission()) || ((sender instanceof Player) && hawk.getCheckManager().getExemptedPlayers().contains(((Player) sender).getUniqueId()));

                sender.sendMessage(ChatColor.GOLD + "You " + (!bypass ? "do not " : "") + "have permission to bypass this check.");
                return true;
            }
        }
        sender.sendMessage(ChatColor.RED + "Unknown check \"" + args[1] + "\"");
        return true;
    }
}
