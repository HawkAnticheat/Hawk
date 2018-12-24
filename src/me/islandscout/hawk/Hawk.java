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

package me.islandscout.hawk;

import me.islandscout.hawk.command.HawkCommand;
import me.islandscout.hawk.event.PositionEvent;
import me.islandscout.hawk.module.*;
import me.islandscout.hawk.util.ConfigHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Hawk extends JavaPlugin {

    //I take that back: this game does have its problems, but it's still fun to play on.
    //If you don't like my HashMap soup, then guess what: I don't care.
    //Made with passion in U.S.A.

    private CheckManager checkManager;
    private Scheduler scheduler;
    private SQL sql;
    private Hawk plugin;
    private PacketCore packetCore;
    private ViolationLogger violationLogger;
    private FileConfiguration messages;
    private GUIManager guiManager;
    private LagCompensator lagCompensator;
    private BanManager banManager;
    private MuteManager muteManager;
    private MouseRecorder mouseRecorder;
    private Map<UUID, HawkPlayer> profiles;
    private static int SERVER_VERSION;
    public static String FLAG_PREFIX;
    public static final String BASE_PERMISSION = "hawk";
    public static String BUILD_NAME;
    public static String FLAG_CLICK_COMMAND;
    private boolean sendJSONMessages;
    private boolean playSoundOnFlag;

    @Override
    public void onEnable() {
        plugin = this;
        BUILD_NAME = getDescription().getVersion();
        setServerVersion();
        loadModules();
        saveConfigs();
        getLogger().info("Hawk Anticheat has been enabled. Copyright 2018 Islandscout.");
    }

    @Override
    public void onDisable() {
        unloadModules();
        plugin = null;
        this.getLogger().info("Hawk Anticheat has been disabled.");
    }

    public void loadModules() {
        getLogger().info("Loading modules...");
        getServer().getPluginManager().registerEvents(new PlayerManager(this), this);
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "messages.yml"));
        FLAG_PREFIX = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&cHAWK: &7", messages, "prefix"));
        sendJSONMessages = ConfigHelper.getOrSetDefault(false, getConfig(), "sendJSONMessages");
        playSoundOnFlag = ConfigHelper.getOrSetDefault(false, getConfig(), "playSoundOnFlag");
        FLAG_CLICK_COMMAND = ConfigHelper.getOrSetDefault("tp %player%", getConfig(), "flagClickCommand");
        if (sendJSONMessages && getServerVersion() == 7) {
            sendJSONMessages = false;
            Bukkit.getLogger().warning("Hawk cannot send JSON flag messages on a 1.7.10 server! Please use 1.8.8 to use this feature.");
        }
        profiles = new ConcurrentHashMap<>();
        sql = new SQL(this);
        sql.createTableIfNotExists();
        guiManager = new GUIManager(this);
        lagCompensator = new LagCompensator(this);
        banManager = new BanManager(this);
        banManager.loadBannedPlayers();
        muteManager = new MuteManager(this);
        muteManager.loadMutedPlayers();
        startLoggerFile();
        checkManager = new CheckManager(plugin);
        checkManager.loadChecks();
        scheduler = new Scheduler(this);
        scheduler.startSchedulers();
        packetCore = new PacketCore(SERVER_VERSION, this);
        packetCore.setupListenerForOnlinePlayers();
        mouseRecorder = new MouseRecorder(this);
        registerCommand();
    }

    public void unloadModules() {
        getLogger().info("Unloading modules...");
        plugin.packetCore.killListener();
        plugin.getCommand("hawk").setExecutor(null);
        HandlerList.unregisterAll(this);
        guiManager.stop();
        guiManager = null;
        lagCompensator = null;
        checkManager = null;
        banManager.saveBannedPlayers();
        banManager = null;
        muteManager.saveMutedPlayers();
        muteManager = null;
        PositionEvent.discardData();
        profiles = null;
        sql.closeConnection();
        sql = null;
        Bukkit.getScheduler().cancelTasks(this);
        scheduler = null;
        violationLogger = null;
    }

    public void saveConfigs() {
        saveConfig();
        try {
            messages.save(new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "messages.yml"));
        } catch (IOException e) {
            getLogger().severe("Could not save messages to messages.yml");
            e.printStackTrace();
        }
    }

    private void registerCommand() {
        PluginCommand cmd = plugin.getCommand("hawk");
        cmd.setExecutor(new HawkCommand(this));
        cmd.setPermission(Hawk.BASE_PERMISSION + ".cmd");
        if (messages.isSet("unknownCommandMsg"))
            cmd.setPermissionMessage(ChatColor.translateAlternateColorCodes('&', messages.getString("unknownCommandMsg")));
        else {
            messages.set("unknownCommandMsg", "Unknown command. Type \"/help\" for help.");
            cmd.setPermissionMessage(ChatColor.translateAlternateColorCodes('&', messages.getString("unknownCommandMsg")));
        }
    }

    private void startLoggerFile() {
        boolean enabled = ConfigHelper.getOrSetDefault(true, getConfig(), "logToFile");
        String pluginFolder = plugin.getDataFolder().getAbsolutePath();
        File storageFile = new File(pluginFolder + File.separator + "logs.txt");
        violationLogger = new ViolationLogger(this, enabled);
        violationLogger.prepare(storageFile);
    }

    private void setServerVersion() {
        switch (Bukkit.getBukkitVersion().substring(0, 4)) {
            case "1.7.":
                SERVER_VERSION = 7;
                break;
            case "1.8.":
                SERVER_VERSION = 8;
                break;
            default:
                SERVER_VERSION = 0;
                break;
        }
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public SQL getSql() {
        return plugin.sql;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public static int getServerVersion() {
        return SERVER_VERSION;
    }

    public HawkPlayer getHawkPlayer(Player p) {
        HawkPlayer result = profiles.get(p.getUniqueId());
        if (result == null) {
            addProfile(p);
            result = profiles.get(p.getUniqueId());
        }
        return result;
    }

    public Collection<HawkPlayer> getHawkPlayers() {
        return profiles.values();
    }

    public void addProfile(Player p) {
        profiles.put(p.getUniqueId(), new HawkPlayer(p, this));
    }

    public void removeProfile(UUID uuid) {
        profiles.remove(uuid);
    }

    public ViolationLogger getViolationLogger() {
        return violationLogger;
    }

    public LagCompensator getLagCompensator() {
        return lagCompensator;
    }

    public BanManager getBanManager() {
        return banManager;
    }

    public MuteManager getMuteManager() {
        return muteManager;
    }

    public PacketCore getPacketCore() {
        return packetCore;
    }

    public MouseRecorder getMouseRecorder() {
        return mouseRecorder;
    }

    public boolean canSendJSONMessages() {
        return sendJSONMessages;
    }

    public boolean canPlaySoundOnFlag() {
        return playSoundOnFlag;
    }
}
