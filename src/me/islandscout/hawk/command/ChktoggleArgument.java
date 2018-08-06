package me.islandscout.hawk.command;

import me.islandscout.hawk.checks.Check;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ChktoggleArgument extends Argument {

    public ChktoggleArgument() {
        super("chktoggle", "<check>", "Toggles a check.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length < 2)
            return false;
        for(Check check : hawk.getCheckManager().getCheckList()) {
            if(check.getName().equalsIgnoreCase(args[1])) {
                check.setEnabled(!check.isEnabled());
                sender.sendMessage(ChatColor.GOLD + "Check \"" + check.getName() + "\" toggled " + (check.isEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                return true;
            }
        }
        sender.sendMessage(ChatColor.RED + "Unknown check \"" + args[1] + "\"");
        return true;
    }
}
