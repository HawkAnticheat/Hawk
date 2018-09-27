package me.islandscout.hawk.command;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DevArgument extends Argument {

    //something crude but simple to easily troubleshoot errors

    DevArgument() {
        super("dev", "", "Displays information about the server and client.");
    }

    @Override
    public boolean process(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("Server ver.: " + Bukkit.getVersion());
        sender.sendMessage("Bukkit ver.: " + Bukkit.getBukkitVersion());
        String nmsPackage = "";
        if (Hawk.getServerVersion() == 8)
            nmsPackage = net.minecraft.server.v1_8_R3.MinecraftServer.class.getPackage().getName();
        else if (Hawk.getServerVersion() == 7)
            nmsPackage = net.minecraft.server.v1_7_R4.MinecraftServer.class.getPackage().getName();
        sender.sendMessage("NMS ver.: " + nmsPackage.substring(nmsPackage.lastIndexOf(".") + 1));
        sender.sendMessage("Hawk ver.: " + Hawk.BUILD_NAME);
        if(sender instanceof Player) {
            int clientVer = ServerUtils.getClientVersion((Player)sender);
            sender.sendMessage("Possible client ver.: 1." + clientVer + ".x");
            sender.sendMessage("Ping: " + ServerUtils.getPing((Player)sender) + "ms");
        }
        else {
            sender.sendMessage("Possible client ver.: N/A");
            sender.sendMessage("Ping: N/A");
        }
        sender.sendMessage("TPS: " + ServerUtils.getTps());
        sender.sendMessage("Load: " + ServerUtils.getStress());
        return true;
    }
}
