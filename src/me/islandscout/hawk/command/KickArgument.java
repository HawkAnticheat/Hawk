package me.islandscout.hawk.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class KickArgument extends Argument {

    public KickArgument() {
        super("kick", "<player> <reason>", "Kick a player.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 3)
            return false;
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Unknown player \"" + args[1] + "\"");
            return true;
        }
        List<String> list = new LinkedList<>(Arrays.asList(args));
        list.remove(0);
        list.remove(0);
        String reason = ChatColor.translateAlternateColorCodes('&', String.join(" ", list));
        target.kickPlayer(reason);
        sender.sendMessage(ChatColor.GOLD + target.getName() + " has been kicked.");
        return true;
    }
}
