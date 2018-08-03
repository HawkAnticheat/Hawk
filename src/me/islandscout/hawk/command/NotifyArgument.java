package me.islandscout.hawk.command;

import me.islandscout.hawk.HawkPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NotifyArgument extends Argument {

    public NotifyArgument() {
        super("notify", "", "Toggle violation notifications.");
    }

    @Override
    public void process(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage(HawkCommand.PLAYER_ONLY);
            return;
        }
        HawkPlayer pp = hawk.getHawkPlayer((Player)sender);
        pp.setReceiveFlags(!pp.canReceiveFlags());
        sender.sendMessage(ChatColor.GOLD + "In-game notifications toggled " + (pp.canReceiveFlags() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
    }
}
