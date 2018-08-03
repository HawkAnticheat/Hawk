package me.islandscout.hawk.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ReloadArgument extends Argument {

    public ReloadArgument() {
        super("reload", "", "Reload Hawk configuration, modules, and checks.");
    }

    @Override
    public void process(CommandSender sender, Command cmd, String label, String[] args) {
        hawk.reloadConfig();
        hawk.loadMessages();
        hawk.getCheckManager().loadChecks();
        sender.sendMessage(ChatColor.GOLD + "Reloaded configuration files for Hawk.");
    }
}
