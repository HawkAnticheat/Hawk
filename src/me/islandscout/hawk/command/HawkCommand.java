package me.islandscout.hawk.command;

import me.islandscout.hawk.Hawk;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HawkCommand implements CommandExecutor {

    private List<Argument> arguments;

    private final Hawk hawk;
    private static final String NO_PERMISSION = ChatColor.RED + "You do not have permission to perform this action.";
    static final String PLAYER_ONLY = ChatColor.RED + "Only players can perform this action.";

    public HawkCommand(Hawk hawk) {
        this.hawk = hawk;
        arguments = new ArrayList<>();
        arguments.add(new PingArgument());
        arguments.add(new KickArgument());
        arguments.add(new ReloadArgument());
        arguments.add(new NotifyArgument());

        Argument.setHawkReference(hawk);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length > 0) {
            for(Argument arg : arguments) {
                String argName = arg.getName();
                if(argName.equalsIgnoreCase(args[0])) {
                    if(!sender.hasPermission(Hawk.BASE_PERMISSION + ".cmd." + argName)) {
                        sender.sendMessage(NO_PERMISSION);
                        return true;
                    }
                    else {
                        arg.process(sender, cmd, label, args);
                        return true;
                    }
                }
            }
            if(args[0].equalsIgnoreCase("help")) {
                if(args.length > 1) {
                    int pageNumber = -1;
                    try {
                        pageNumber = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignore) {
                    }
                    if (pageNumber < 0) {
                        sender.sendMessage(ChatColor.RED + "Invalid page number.");
                        return true;
                    } else {
                        sendUsage(sender, pageNumber);
                    }
                }
                else {
                    sendUsage(sender, 0);
                }
            }
            else {
                sendUsage(sender, 0);
                sender.sendMessage(ChatColor.RED + "Unknown argument.");
            }
        }
        else {
            sendUsage(sender, 0);
            if(sender instanceof Player) {
                hawk.getGuiManager().sendMenuWindow((Player) sender);
            }
        }
        return true;
    }

    private void sendUsage(CommandSender sender, int pageNumber) {
        int ENTRIES_PER_PAGE = 5;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&m-----------------&r &8&l[ &eHawk AntiCheat &8&l]&r &7&m-----------------"));
        int maxPage = (arguments.size() - 1) / ENTRIES_PER_PAGE;
        if(pageNumber > maxPage)
            pageNumber = maxPage;
        int argsIndex = pageNumber * ENTRIES_PER_PAGE;
        int pageMaxIndex = argsIndex + ENTRIES_PER_PAGE;
        for(int i = argsIndex; i < pageMaxIndex && i < arguments.size(); i++) {
            Argument argument = arguments.get(i);
            sender.sendMessage(ChatColor.GOLD + "/hawk " + argument.getUsage() + ": " + ChatColor.GRAY + argument.getDescription());
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&m----------------------&r &8[ &e" + pageNumber + ":" + maxPage + " &8] &7&m----------------------"));
    }
}
