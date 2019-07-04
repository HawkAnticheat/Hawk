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

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.module.GUIManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HawkCommand implements CommandExecutor {

    private final List<Argument> arguments;

    private final Hawk hawk;
    static final String PLAYER_ONLY = ChatColor.RED + "Only players can perform this action.";
    private static final int ENTRIES_PER_PAGE = 5;

    public HawkCommand(Hawk hawk) {
        this.hawk = hawk;
        arguments = new ArrayList<>();
        arguments.add(new PingArgument());
        arguments.add(new KickArgument());
        arguments.add(new ReloadArgument());
        arguments.add(new ToggleAlertsArgument());
        arguments.add(new ChecksArgument());
        arguments.add(new ChkinfoArgument());
        arguments.add(new ViolationsArgument());
        arguments.add(new ChktoggleArgument());
        arguments.add(new MsgArgument());
        arguments.add(new BroadcastArgument());
        arguments.add(new DevArgument());
        arguments.add(new BanArgument());
        arguments.add(new MuteArgument());
        arguments.add(new UnbanArgument());
        arguments.add(new UnmuteArgument());
        arguments.add(new MouseRecArgument());
        arguments.add(new PunishArgument());
        arguments.add(new ExemptArgument());
        arguments.add(new ForceArgument());

        Collections.sort(arguments);

        Argument.hawk = hawk;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            //TODO: Do a binary search here
            for (Argument arg : arguments) {
                String argName = arg.getName();
                if (argName.equalsIgnoreCase(args[0])) {
                    String perm = Hawk.BASE_PERMISSION + ".cmd." + argName;
                    if (!sender.hasPermission(perm)) {
                        sender.sendMessage(String.format(Hawk.NO_PERMISSION, perm));
                        return true;
                    } else {
                        if (!arg.process(sender, cmd, label, args)) {
                            sender.sendMessage(ChatColor.RED + "Usage: /hawk " + arg.getUsage());
                        }
                        return true;
                    }
                }
            }
            if (args[0].equalsIgnoreCase("help")) {
                if (args.length > 1) {
                    int pageNumber = -1;
                    try {
                        pageNumber = Integer.parseInt(args[1]) - 1;
                    } catch (NumberFormatException ignore) {
                    }
                    if (pageNumber < 0) {
                        sender.sendMessage(ChatColor.RED + "Invalid page number.");
                        return true;
                    } else {
                        sendUsage(sender, pageNumber);
                    }
                } else {
                    sendUsage(sender, 0);
                }
            } else {
                sendUsage(sender, 0);
                sender.sendMessage(ChatColor.RED + "Unknown argument.");
            }
        } else {
            GUIManager guiManager = hawk.getGuiManager();
            if (sender instanceof Player && guiManager.isEnabled()) {
                guiManager.sendMainMenuWindow((Player) sender);
            }
            else {
                sendUsage(sender, 0);
            }
        }
        return true;
    }

    private void sendUsage(CommandSender sender, int pageNumber) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&m-----------------&r &8&l[ &eHawk Anticheat &8&l]&r &7&m-----------------"));
        int maxPage = (arguments.size() - 1) / ENTRIES_PER_PAGE;
        if (pageNumber > maxPage)
            pageNumber = maxPage;
        int argsIndex = pageNumber * ENTRIES_PER_PAGE;
        int pageMaxIndex = argsIndex + ENTRIES_PER_PAGE;
        for (int i = argsIndex; i < pageMaxIndex && i < arguments.size(); i++) {
            Argument argument = arguments.get(i);
            sender.sendMessage(ChatColor.GOLD + "/hawk " + argument.getUsage() + ": " + ChatColor.GRAY + argument.getDescription());
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&m------------------&r &8[ &ePage " + (pageNumber + 1) + " of " + (maxPage + 1) + " &8] &7&m-------------------"));
        sender.sendMessage(ChatColor.GRAY + "/hawk help <page number>                       Build " + Hawk.BUILD_NAME);
    }
}
