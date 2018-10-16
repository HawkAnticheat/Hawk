package me.islandscout.hawk.command;

import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PingArgument extends Argument {

    PingArgument() {
        super("ping", "[player]", "Displays ping of target player.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 1) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Unknown player \"" + args[1] + "\"");
                return true;
            }
            HawkPlayer pp = hawk.getHawkPlayer(target);
            sender.sendMessage(ChatColor.GOLD + target.getName() + "'s ping: " + ServerUtils.getPing(target) + "ms");
            sender.sendMessage(ChatColor.GOLD + target.getName() + "'s jitter: " + pp.getPingJitter() + "ms");
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(HawkCommand.PLAYER_ONLY);
                return true;
            }
            HawkPlayer pp = hawk.getHawkPlayer((Player) sender);
            sender.sendMessage(ChatColor.GOLD + "Your ping: " + ServerUtils.getPing((Player) sender) + "ms");
            sender.sendMessage(ChatColor.GOLD + "Your jitter: " + pp.getPingJitter() + "ms");
        }
        return true;
    }
}
