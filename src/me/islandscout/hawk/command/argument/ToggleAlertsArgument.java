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

package me.islandscout.hawk.command.argument;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.command.HawkCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleAlertsArgument extends Argument {

    public ToggleAlertsArgument() {
        super("talerts", "", "Toggle alerts for yourself.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(HawkCommand.PLAYER_ONLY);
            return true;
        }
        HawkPlayer pp = hawk.getHawkPlayer((Player) sender);
        pp.setReceiveNotificationsPreference(!pp.getReceiveNotificationsPreference());
        sender.sendMessage(ChatColor.GOLD + "In-game alerts toggled " + (pp.getReceiveNotificationsPreference() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));

        String perm = Hawk.BASE_PERMISSION + ".alerts";
        if(!pp.getPlayer().hasPermission(perm)) {
            pp.getPlayer().sendMessage(ChatColor.GRAY + "NOTE: You do not have the permission \"" + perm + "\" to receive Hawk notifications/alerts.");
        }

        return true;
    }
}
