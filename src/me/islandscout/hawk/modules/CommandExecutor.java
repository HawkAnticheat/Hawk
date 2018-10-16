package me.islandscout.hawk.modules;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.checks.Check;
import me.islandscout.hawk.utils.MathPlus;
import me.islandscout.hawk.utils.Placeholder;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandExecutor {

    public static void runACommand(List<String> command, Check check, Player p, HawkPlayer pp, Hawk hawk, Placeholder... placeholders) {
        if (command.size() == 0 || command.get(0).length() == 0) return;
        for (String aCommand : command) {
            if (aCommand.length() == 0) return;
            String[] parts = aCommand.split(":");
            if (parts.length < 3) {
                hawk.getLogger().warning("Failed command execution: Invalid command syntax in " + check + " configuration!");
                return;
            }

            //ignore colons in command
            for (int i = 3; i < parts.length; i++)
                parts[2] += ":" + parts[i];

            try {
                if (parts[0].charAt(0) == '>' && parts[0].charAt(1) == '=') {
                    if (pp.getVL(check) >= Integer.parseInt(parts[0].substring(2))) {
                        execute(parts, p, hawk, check, placeholders);
                    }
                    return;
                }
                if (parts[0].charAt(0) == '<' && parts[0].charAt(1) == '=') {
                    if (pp.getVL(check) <= Integer.parseInt(parts[0].substring(2))) {
                        execute(parts, p, hawk, check, placeholders);
                    }
                    return;
                }
                if (parts[0].charAt(0) == '>') {
                    if (pp.getVL(check) > Integer.parseInt(parts[0].substring(1))) {
                        execute(parts, p, hawk, check, placeholders);
                    }
                    return;
                }
                if (parts[0].charAt(0) == '<') {
                    if (pp.getVL(check) < Integer.parseInt(parts[0].substring(1))) {
                        execute(parts, p, hawk, check, placeholders);
                    }
                    return;
                }
                if (pp.getVL(check) == Integer.parseInt(parts[0])) {
                    execute(parts, p, hawk, check, placeholders);
                }
            } catch (NumberFormatException e) {
                hawk.getLogger().warning("Failed command execution: Invalid command syntax in " + check + " configuration!");
            }
        }
    }

    private static void execute(String[] parts, Player player, Hawk hawk, Check check, Placeholder... placeholders) {
        String preCmd = parts[2]
                .replace("%player%", player.getName())
                .replace("%ping%", ServerUtils.getPing(player) + "")
                .replace("%check%", check + "")
                .replace("%tps%", MathPlus.round(ServerUtils.getTps(), 2) + "");
        for (Placeholder placeholder : placeholders)
            preCmd = preCmd.replace("%" + placeholder.getKey() + "%", placeholder.getValue().toString());
        final String cmd = preCmd;
        Bukkit.getScheduler().scheduleSyncDelayedTask((hawk), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd), Long.parseLong(parts[1]) * 20);
    }
}
