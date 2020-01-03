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

import me.islandscout.hawk.Hawk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class BanArgument extends Argument {

    public BanArgument() {
        super("ban", "<player> <seconds> <reason>", "Ban a player using Hawk's ban manager.");
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
        if (target.hasPermission(Hawk.BASE_PERMISSION + ".admin")) {
            sender.sendMessage(ChatColor.RED + "You may not ban that player.");
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

        hawk.getBanManager().ban(target.getUniqueId(), expireTime, reason);
        target.kickPlayer(reason);
        sender.sendMessage(ChatColor.GOLD + target.getName() + " has been banned for " + seconds + (seconds == 1 ? " second." : " seconds."));
        return true;
    }
}
