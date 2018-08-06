package me.islandscout.hawk.command;

import me.islandscout.hawk.checks.Check;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ChecksArgument extends Argument {

    ChecksArgument() {
        super("checks", "", "Provides a list of Hawk checks.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> chkNames = new ArrayList<>();
        for(Check check : hawk.getCheckManager().getCheckList()) {
            chkNames.add((check.isEnabled() ? ChatColor.GREEN : ChatColor.RED) + check.getName());
        }
        sender.sendMessage(ChatColor.GOLD + "Checks: " + String.join(", ", chkNames));
        return true;
    }
}
