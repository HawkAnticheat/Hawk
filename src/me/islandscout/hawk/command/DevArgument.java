/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.command;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.utils.ServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

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
        if (sender instanceof Player) {
            int clientVer = ServerUtils.getClientVersion((Player) sender);
            sender.sendMessage("Possible client ver.: 1." + clientVer + ".x");
            sender.sendMessage("Ping: " + ServerUtils.getPing((Player) sender) + "ms");
        } else {
            sender.sendMessage("Possible client ver.: N/A");
            sender.sendMessage("Ping: N/A");
        }
        sender.sendMessage("TPS: " + ServerUtils.getTps());
        sender.sendMessage("Load: " + ServerUtils.getStress());
        List<String> plugNames = new ArrayList<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            plugNames.add(plugin.getName()); //we want ALL loaded plugins, even disabled plugins
        }
        sender.sendMessage("Plugins loaded (" + Bukkit.getPluginManager().getPlugins().length + "): "
                + String.join(", ", plugNames));
        return true;
    }
}
