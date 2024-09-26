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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MuteArgument extends Argument {

    public MuteArgument() {
        super("mute", "<player> <seconds> <reason>", "Mute a player using Hawk's mute manager.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 4)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Unknown player \"" + args[1] + "\"");
            return true;
        }
        String permission = Hawk.BASE_PERMISSION + ".admin";
        if (target.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "You cannot mute that player, they have the permission \"" + permission + "\"");
            return true;
        }
        long expireTime = -1;
        long seconds = -1;
        try {
            seconds = Long.parseLong(args[2]);
            expireTime = seconds * 1000 + System.currentTimeMillis();
        } catch (NumberFormatException ignore) {
        }
        if (seconds < 1) {
            sender.sendMessage(ChatColor.RED + "Third argument must be a positive integer.");
            return true;
        }

        List<String> list = (new LinkedList<>(Arrays.asList(args))).subList(3, args.length);
        String reason = ChatColor.translateAlternateColorCodes('&', String.join(" ", list));

        hawk.getMuteManager().mute(target.getUniqueId(), expireTime, reason);
        target.sendMessage(ChatColor.RED + "You have been muted for the duration of " + seconds + (seconds == 1 ? " second " : " seconds ") + "for: " + reason);
        sender.sendMessage(ChatColor.GOLD + target.getName() + " has been muted for " + seconds + (seconds == 1 ? " second." : " seconds."));
        return true;
    }
}
