package me.islandscout.hawk.command;

import me.islandscout.hawk.checks.Check;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ViolationsArgument extends Argument {

    public ViolationsArgument() {
        super("vl", "<player> <check>", "Get the VL of a player for a specified check.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length < 3)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if(target == null) {
            sender.sendMessage(ChatColor.RED + "Unknown player \"" + args[1] + "\"");
            return true;
        }
        for(Check check : hawk.getCheckManager().getCheckList()) {
            if(check.getName().equalsIgnoreCase(args[2])) {
                sender.sendMessage(ChatColor.GOLD + target.getName() + "'s VL for " + check.getName() + ": " + hawk.getHawkPlayer(target).getVL(check));
                return true;
            }
        }
        sender.sendMessage(ChatColor.RED + "Unknown check \"" + args[2] + "\"");
        return true;
    }
}
