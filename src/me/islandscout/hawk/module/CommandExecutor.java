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

package me.islandscout.hawk.module;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.check.Check;
import me.islandscout.hawk.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandExecutor {

    private final Set<Pair<UUID, Pair<Check, String>>> commandHistory;

    public CommandExecutor(Hawk hawk) {
        this.commandHistory = Collections.synchronizedSet(new HashSet<>());
        int cooldown = ConfigHelper.getOrSetDefault(1, hawk.getConfig(), "commandExecutor.cooldownTicks");

        hawk.getHawkSyncTaskScheduler().addRepeatingTask(commandHistory::clear, cooldown);
    }

    public void runACommand(List<String> command, Check check, double deltaVL, Player p, HawkPlayer pp, Hawk hawk, Placeholder... placeholders) {
        if (command.size() == 0 || command.get(0).length() == 0) return;
        for (String aCommand : command) {
            if (aCommand.length() == 0) return;
            String[] parts = aCommand.split(":");
            if (parts.length < 3) {
                hawk.getLogger().warning("Failed command execution: Invalid command syntax in " + check + " configuration!");
                return;
            }

            //ignore colons in command
            //TODO: um... what if there are multiple cmds in the list that meet the conditions? Either get rid of the > / < or fix this
            for (int i = 3; i < parts.length; i++)
                parts[2] += ":" + parts[i];

            try {
                if (parts[0].charAt(0) == '>' && parts[0].charAt(1) == '=') {
                    if (pp.getVL(check) >= Integer.parseInt(parts[0].substring(2))) {
                        execute(parts, p, hawk, check, placeholders);
                    }
                    return;
                }
                if (parts[0].charAt(0) == '<' && parts[0].charAt(1) == '=') {
                    if (pp.getVL(check) <= Integer.parseInt(parts[0].substring(2))) {
                        execute(parts, p, hawk, check, placeholders);
                    }
                    return;
                }
                if (parts[0].charAt(0) == '>') {
                    if (pp.getVL(check) > Integer.parseInt(parts[0].substring(1))) {
                        execute(parts, p, hawk, check, placeholders);
                    }
                    return;
                }
                if (parts[0].charAt(0) == '<') {
                    if (pp.getVL(check) < Integer.parseInt(parts[0].substring(1))) {
                        execute(parts, p, hawk, check, placeholders);
                    }
                    return;
                }
                int confVL = Integer.parseInt(parts[0]);
                double currVL = pp.getVL(check);
                if (pp.getVL(check) >= confVL && currVL - deltaVL <= confVL) {
                    execute(parts, p, hawk, check, placeholders);
                }
            } catch (NumberFormatException e) {
                hawk.getLogger().warning("Failed command execution: Invalid command syntax in " + check + " configuration!");
            }
        }
    }

    private void execute(String[] parts, Player player, Hawk hawk, Check check, Placeholder... placeholders) {
        String preCmd = parts[2]
                .replace("%player%", player.getName())
                .replace("%ping%", ServerUtils.getPing(player) + "")
                .replace("%check%", check + "")
                .replace("%tps%", MathPlus.round(ServerUtils.getTps(), 2) + "");
        for (Placeholder placeholder : placeholders)
            preCmd = preCmd.replace("%" + placeholder.getKey() + "%", placeholder.getValue().toString());
        final String cmd = preCmd;
        Pair<UUID, Pair<Check, String>> cmdInfo = new Pair<>(player.getUniqueId(), new Pair<>(check, parts[2]));
        if (!commandHistory.contains(cmdInfo))
            Bukkit.getScheduler().scheduleSyncDelayedTask((hawk), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd), Long.parseLong(parts[1]) * 20);
        commandHistory.add(cmdInfo);
    }
}
