package me.islandscout.hawk;

import me.islandscout.hawk.checks.CheckManager;
import me.islandscout.hawk.command.HawkCommand;
import me.islandscout.hawk.events.PositionEvent;
import me.islandscout.hawk.events.external.HawkViolationEvent;
import me.islandscout.hawk.gui.GUIManager;
import me.islandscout.hawk.listener.BukkitListener;
import me.islandscout.hawk.utils.ConfigHelper;
import me.islandscout.hawk.modules.*;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Hawk extends JavaPlugin {

    //Words cannot describe how awful I think this game is.

    private CheckManager checkManager;
    private Scheduler scheduler;
    private SQL sql;
    private Hawk plugin;
    private PacketCore packetCore;
    private TextLogger textLogger;
    private FileConfiguration messages;
    private GUIManager guiManager;
    private LagCompensator lagCompensator;
    private Map<UUID, HawkPlayer> profiles;
    private static int SERVER_VERSION;
    public static String FLAG_PREFIX;
    public static String BASE_PERMISSION = "hawk";
    public static String BUILD_NAME;
    public static String FLAG_CLICK_COMMAND;
    private boolean callBukkitEvents;
    private boolean sendJSONMessages;

    @Override
    public void onEnable(){
        plugin = this;
        BUILD_NAME = getDescription().getVersion();
        setServerVersion();
        loadModules();
        saveConfigs();
        getLogger().info("Hawk Anticheat has been enabled. Copyright 2018 Islandscout. All rights reserved.");
    }

    @Override
    public void onDisable() {
        unloadModules();
        plugin = null;
        this.getLogger().info("Hawk Anticheat has been disabled.");
    }

    public void loadModules() {
        getLogger().info("Loading modules...");
        getServer().getPluginManager().registerEvents(new BukkitListener(this), this);
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "messages.yml"));
        FLAG_PREFIX = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&cHAWK:", messages, "prefix"));
        FLAG_CLICK_COMMAND = ConfigHelper.getOrSetDefault("tp %player%", getConfig(), "flagClickCommand");
        callBukkitEvents = ConfigHelper.getOrSetDefault(false, getConfig(), "callBukkitEvents");
        sendJSONMessages = ConfigHelper.getOrSetDefault(false, getConfig(), "sendJSONMessages");
        if(sendJSONMessages && getServerVersion() == 7) {
            sendJSONMessages = false;
            Bukkit.getLogger().warning("Hawk cannot send JSON flag messages on a 1.7.10 server! Please use 1.8.8 to use this feature.");
        }
        profiles = new ConcurrentHashMap<>();
        sql = new SQL(this);
        sql.createTableIfNotExists();
        guiManager = new GUIManager(this);
        lagCompensator = new LagCompensator(this);
        startLoggerFile();
        checkManager = new CheckManager(plugin);
        checkManager.loadChecks();
        scheduler = new Scheduler(this);
        scheduler.startSchedulers();
        packetCore = new PacketCore(SERVER_VERSION, this);
        packetCore.setupListenerOnlinePlayers();
        registerCommand();
    }

    public void unloadModules() {
        getLogger().info("Unloading modules...");
        plugin.packetCore.killListener();
        plugin.getCommand("hawk").setExecutor(null);
        HandlerList.unregisterAll(this);
        HawkViolationEvent.getHandlerList().unregister(plugin);
        guiManager = null;
        lagCompensator = null;
        checkManager = null;
        PositionEvent.discardData();
        profiles = null;
        sql.closeConnection();
        sql = null;
        Bukkit.getScheduler().cancelTasks(this);
        scheduler = null;
        textLogger = null;
    }

    public void saveConfigs() {
        saveConfig();
        try {
            messages.save(new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "messages.yml"));
        }
        catch (IOException e) {
            getLogger().severe("Could not save messages to messages.yml");
            e.printStackTrace();
        }
    }

    private void registerCommand(){
        plugin.getCommand("hawk").setExecutor(new HawkCommand(this));
        Map<String, Map<String, Object>> map = getDescription().getCommands();
        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
            PluginCommand command = getCommand(entry.getKey());
            command.setPermission(Hawk.BASE_PERMISSION + ".cmd");
            if(messages.isSet("unknownCommandMsg"))
                command.setPermissionMessage(ChatColor.translateAlternateColorCodes('&', messages.getString("unknownCommandMsg")));
            else {
                messages.set("unknownCommandMsg", "Unknown command. Type \"/help\" for help.");
                command.setPermissionMessage(ChatColor.translateAlternateColorCodes('&', messages.getString("unknownCommandMsg")));
            }
        }
    }

    private void startLoggerFile() {
        boolean enabled = ConfigHelper.getOrSetDefault(true, getConfig(), "logToFile");
        String pluginFolder = plugin.getDataFolder().getAbsolutePath();
        File storageFile = new File(pluginFolder + File.separator + "logs.txt");
        textLogger = new TextLogger(this, enabled);
        textLogger.prepare(storageFile);
    }

    private void setServerVersion() {
        if(Bukkit.getBukkitVersion().substring(0, 4).equals("1.7.")) SERVER_VERSION = 7;
        else if(Bukkit.getBukkitVersion().substring(0, 4).equals("1.8.")) SERVER_VERSION = 8;
        else SERVER_VERSION = 0;

    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public CheckManager getCheckManager() { return checkManager; }

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
        if(result == null) {
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

    public PacketCore getPacketCore() {
        return packetCore;
    }

    public TextLogger getTextLogger() {
        return textLogger;
    }

    public LagCompensator getLagCompensator() {
        return lagCompensator;
    }

    public boolean canCallBukkitEvents() {
        return callBukkitEvents;
    }

    public boolean canSendJSONMessages() {
        return sendJSONMessages;
    }
}
