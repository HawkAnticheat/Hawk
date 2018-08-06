package me.islandscout.hawk.command;

import me.islandscout.hawk.Hawk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MsgArgument extends Argument {

    public MsgArgument() {
        super("msg", "<player> <message>", "Send a player a message.");
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
        List<String> list = new LinkedList<>(Arrays.asList(args));
        list.remove(0);
        list.remove(0);
        String msg = Hawk.FLAG_PREFIX + ChatColor.RESET + " " + ChatColor.translateAlternateColorCodes('&', String.join(" ", list));
        target.sendMessage(msg);
        sender.sendMessage(ChatColor.GOLD + "Sent message to " + target.getName() + ".");
        return true;
    }
}
