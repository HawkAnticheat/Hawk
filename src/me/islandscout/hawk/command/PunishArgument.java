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
import me.islandscout.hawk.util.Pair;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PunishArgument extends Argument {

    PunishArgument() {
        super("punish", "<list> | <<info> <player>> | <<'add' | 'remove' | 'authorize'> <player> [reason]>", "Manage Hawk's PunishmentScheduler");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        PunishmentScheduler pScheduler = hawk.getPunishmentScheduler();
        if(args.length < 2)
            return false;

        if(args[1].equals("list")) {
            pScheduler.list(sender);
            return true;
        }

        if(args.length < 3)
            return false;

        @SuppressWarnings("deprecation")
        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[2]);

        if(args[1].equals("info")) {
            pScheduler.info(sender, offlineTarget);
            return true;
        }

        if(args[1].equals("remove")) {
            pScheduler.remove(offlineTarget);
            return true;
        }

        if(args[1].equals("authorize")) {
            pScheduler.authorize(offlineTarget);
            return true;
        }

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

        return false;
    }
}
