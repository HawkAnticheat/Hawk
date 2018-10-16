package me.islandscout.hawk.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class UnmuteArgument extends Argument {

    UnmuteArgument() {
        super("unmute", "<player>", "Unmute a player from Hawk's mute manager.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        return false;
    }
}
