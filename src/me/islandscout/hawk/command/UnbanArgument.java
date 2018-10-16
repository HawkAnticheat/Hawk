package me.islandscout.hawk.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class UnbanArgument extends Argument {

    UnbanArgument() {
        super("unban", "<player>", "Unban a player from Hawk's ban manager.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        return false;
    }
}
