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

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NotifyArgument extends Argument {

    public NotifyArgument() {
        super("notify", "", "Toggle violation notifications.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(HawkCommand.PLAYER_ONLY);
            return true;
        }
        HawkPlayer pp = hawk.getHawkPlayer((Player) sender);
        pp.setReceiveNotifications(!pp.canReceiveNotifications());
        sender.sendMessage(ChatColor.GOLD + "In-game notifications toggled " + (pp.canReceiveNotifications() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        return true;
    }
}
