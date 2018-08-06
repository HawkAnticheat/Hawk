package me.islandscout.hawk.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ReloadArgument extends Argument {

    public ReloadArgument() {
        super("reload", "", "Reload Hawk configuration, modules, and checks.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        hawk.reloadConfig();
        hawk.unloadModules();
        hawk.loadModules();
        sender.sendMessage(ChatColor.GOLD + "Reloaded configuration files and modules for Hawk.");
        return true;
    }
}
