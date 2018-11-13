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
    private static final String NO_PERMISSION = ChatColor.RED + "You do not have permission to perform this action.";
    static final String PLAYER_ONLY = ChatColor.RED + "Only players can perform this action.";
    private static final int ENTRIES_PER_PAGE = 5;

    public HawkCommand(Hawk hawk) {
        this.hawk = hawk;
        arguments = new ArrayList<>();
        arguments.add(new PingArgument());
        arguments.add(new KickArgument());
        arguments.add(new ReloadArgument());
        arguments.add(new NotifyArgument());
        arguments.add(new ChecksArgument());
        arguments.add(new ChkinfArgument());
        arguments.add(new ViolationsArgument());
        arguments.add(new ChktoggleArgument());
        arguments.add(new MsgArgument());
        arguments.add(new BroadcastArgument());
        arguments.add(new DevArgument());
        arguments.add(new BanArgument());
        arguments.add(new MuteArgument());
        arguments.add(new UnbanArgument());
        arguments.add(new UnmuteArgument());

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
                    if (!sender.hasPermission(Hawk.BASE_PERMISSION + ".cmd." + argName)) {
                        sender.sendMessage(NO_PERMISSION);
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
            sendUsage(sender, 0);
            if (sender instanceof Player) {
                hawk.getGuiManager().sendMainMenuWindow((Player) sender);
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
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&m------------------&r &8[ &ePage " + (pageNumber + 1) + " of " + (maxPage + 1) + " &8] &7&m--------------------"));
        sender.sendMessage(ChatColor.GRAY + "Build " + Hawk.BUILD_NAME);
    }
}
