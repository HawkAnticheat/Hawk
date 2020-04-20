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
import me.islandscout.hawk.check.Check;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ViolationsArgument extends Argument {

    ViolationsArgument() {
        super("vl", "<player> [check]", "Get the VLs of a player.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Unknown player \"" + args[1] + "\"");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.GOLD + "" + target.getName() + "'s top 5 VLs:");

            HawkPlayer pp = hawk.getHawkPlayer(target);
            List<Map.Entry<Check, Double>> list = new LinkedList<>(pp.getVLs().entrySet());
            list.sort(Comparator.comparing(Map.Entry::getValue));

            int line = 0;
            for (int i = list.size() - 1; i >= 0 && line < 5; i--) {
                Map.Entry<Check, Double> entry = list.get(i);
                int vl = (int) (double) entry.getValue();
                if (vl == 0)
                    continue;
                sender.sendMessage(ChatColor.GOLD + "" + entry.getKey() + ": " + vl);
                line++;
            }

            sender.sendMessage(ChatColor.GOLD + "----");
            return true;
        }
        for (Check check : hawk.getCheckManager().getChecks()) {
            if (check.getName().equalsIgnoreCase(args[2])) {
                sender.sendMessage(ChatColor.GOLD + target.getName() + "'s VL for " + check.getName() + ": " + hawk.getHawkPlayer(target).getVL(check));
                return true;
            }
        }
        sender.sendMessage(ChatColor.RED + "Unknown check \"" + args[2] + "\"");
        return true;
    }
}
