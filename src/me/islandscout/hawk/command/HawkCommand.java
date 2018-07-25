package me.islandscout.hawk.command;

import me.islandscout.hawk.Hawk;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HawkCommand implements CommandExecutor {


    //TODO: Work on args




    private final Hawk hawk;

    public HawkCommand(Hawk hawk) {
        this.hawk = hawk;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(commandSender instanceof Player) {
            hawk.getGuiManager().sendMenuWindow((Player) commandSender);
        }

        System.gc();

        sendHeader(commandSender);

        commandSender.sendMessage(ChatColor.GOLD + "/hawk cps <player> <iterations>: " + ChatColor.GRAY + "analyze CPS.");
        commandSender.sendMessage(ChatColor.GOLD + "/hawk exempt <player>|<-l>: " + ChatColor.GRAY + "toggle exempting player from checks.");
        commandSender.sendMessage(ChatColor.GOLD + "/hawk kick <player> <reason>: " + ChatColor.GRAY + "kick a player.");
        commandSender.sendMessage(ChatColor.GOLD + "/hawk notify: " + ChatColor.GRAY + "toggle violation notifications.");
        commandSender.sendMessage(ChatColor.GOLD + "/hawk reload: " + ChatColor.GRAY + "reload configuration.");
        commandSender.sendMessage(ChatColor.GOLD + "/hawk unban <player>: " + ChatColor.GRAY + "unban player banned by Hawk.");
        commandSender.sendMessage(ChatColor.GOLD + "/hawk unmute <player>: " + ChatColor.GRAY + "unmute player muted by Hawk.");
        //sender.sendMessage(ChatColor.DARK_AQUA + "/hawk update: " + ChatColor.GRAY + "install update.");

        sendFooter(commandSender, 0, 0);


        return true;
    }



    private void sendHeader(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&l#&f&m-----------------&r &7&l[ &eHawk AntiCheat &7&l]&r &f&m-----------------&7&l#"));
    }

    private void sendFooter(CommandSender sender, int pageNumber, int maxPageNumber) {

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7&l#&f&m---------------------------------------------------&7&l#"));
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "[HAWK] Developed by Islandscout. 2015-2018");
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + Hawk.BUILD_NAME);
    }
}
