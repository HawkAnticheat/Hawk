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

import me.islandscout.hawk.module.PunishmentScheduler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class PunishArgument extends Argument {

    PunishArgument() {
        super("punish", "<list> | <<'add' | 'remove' | 'authorize'> <player> [reason]>", "Manage Hawk's PunishmentScheduler");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        PunishmentScheduler pScheduler = hawk.getPunishmentScheduler();
        if(args.length < 2)
            return false;

        if(args[1].equals("list")) {

            return true;
        }

        if(args.length < 3)
            return false;

        Player target = Bukkit.getPlayer(args[2]);
        if(target == null) {
            sender.sendMessage(ChatColor.RED + "Player \"" + args[2] + "\" is not online");
            return true;
        }
        String reason;
        if(args.length > 3) {
            reason = String.join(" ", Arrays.asList(args).subList(3, args.length));
        }
        else
            reason = null;

        if(args[1].equals("add")) {
            pScheduler.add(target, reason);
            return true;
        }

        if(args[1].equals("remove")) {
            pScheduler.remove(target);
            return true;
        }

        if(args[1].equals("authorize")) {
            pScheduler.authorize(target);
            return true;
        }

        return false;
    }
}
