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

import me.islandscout.hawk.check.Check;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ChktoggleArgument extends Argument {

    public ChktoggleArgument() {
        super("chktoggle", "<check>", "Toggles a check.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2)
            return false;
        for (Check check : hawk.getCheckManager().getChecks()) {
            if (check.getName().equalsIgnoreCase(args[1])) {
                check.setEnabled(!check.isEnabled());
                sender.sendMessage(ChatColor.GOLD + "Check \"" + check.getName() + "\" toggled " + (check.isEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                return true;
            }
        }
        sender.sendMessage(ChatColor.RED + "Unknown check \"" + args[1] + "\"");
        return true;
    }
}
