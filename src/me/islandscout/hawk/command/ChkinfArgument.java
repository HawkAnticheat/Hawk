package me.islandscout.hawk.command;

import me.islandscout.hawk.checks.Cancelless;
import me.islandscout.hawk.checks.Check;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ChkinfArgument extends Argument {

    ChkinfArgument() {
        super("chkinfo", "<check>", "Displays information about specified check.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length < 2)
            return false;
        for(Check check : hawk.getCheckManager().getCheckList()) {
            if(check.getName().equalsIgnoreCase(args[1])) {
                sender.sendMessage(ChatColor.GOLD + "Basic information about check \"" + check.getName() + "\":");
                sender.sendMessage(ChatColor.GOLD + "Status: " + (check.isEnabled() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                sender.sendMessage(ChatColor.GOLD + "Category: " + check.getClass().getSuperclass().getSimpleName());
                sender.sendMessage(ChatColor.GOLD + "Cancel: " + (check instanceof Cancelless ? ChatColor.GRAY + "N/A" : ((check.canCancel() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"))));
                sender.sendMessage(ChatColor.GOLD + "Flag: " + ((check.isFlaggable() ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED")));
                return true;
            }
        }
        sender.sendMessage(ChatColor.RED + "Unknown check \"" + args[1] + "\"");
        return true;
    }
}
