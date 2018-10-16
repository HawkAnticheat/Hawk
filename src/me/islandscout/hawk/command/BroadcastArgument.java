package me.islandscout.hawk.command;

import me.islandscout.hawk.Hawk;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class BroadcastArgument extends Argument {

    public BroadcastArgument() {
        super("broadcast", "<message>", "Broadcast a message.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> list = new LinkedList<>(Arrays.asList(args));
        list.remove(0);
        String msg = Hawk.FLAG_PREFIX + ChatColor.translateAlternateColorCodes('&', String.join(" ", list));
        Bukkit.broadcastMessage(msg);
        return true;
    }
}
