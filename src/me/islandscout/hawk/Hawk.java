package me.islandscout.hawk;

import me.islandscout.hawk.api.HawkAPI;
import me.islandscout.hawk.checks.CheckManager;
import me.islandscout.hawk.command.HawkCommand;
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

public class Hawk extends JavaPlugin {

    //TO DO: There seems to be a bug in which if you tp, your last location (via Bukkit) won't update until you move. This is a problem for combat checks.
    //This might be fixed. Check to make sure.

    //TODO: You should make a reporting system that will multiply VLs based on the amount of reports received.

    private CheckManager checkManager;
    private Scheduler scheduler;
    private SQL sql;
    private Hawk plugin;
    private PacketCore packetCore;
    private TextLogger textLogger;
    private FileConfiguration messages;
    private GUIManager guiManager;
    private LagCompensator lagCompensator;
    private Map<UUID, HawkPlayer> profiles; //TODO: make this expire
    private static int SERVER_VERSION;
    public static String FLAG_PREFIX;
    public static String BASE_PERMISSION = "hawk";
    public static String BUILD_NAME;

    @Override
    public void onEnable(){
        plugin = this;
        BUILD_NAME = getDescription().getVersion();
        setServerVersion();
        loadModules();
        setupAPI();
        saveConfigs();
        getLogger().info("Hawk Anti-Cheat has been enabled. Copyright 2018 Islandscout. All rights reserved.");
    }

    @Override
    public void onDisable() {
        unloadModules();
        plugin = null;
        this.getLogger().info("Hawk Anti-Cheat has been disabled.");
    }

    public void loadModules() {
        getServer().getPluginManager().registerEvents(new BukkitListener(this), this);
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "messages.yml"));
        FLAG_PREFIX = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&cHAWK:", messages, "prefix"));
        profiles = new HashMap<>();
        guiManager = new GUIManager(this);
        lagCompensator = new LagCompensator(this);
        startLoggerFile();
        checkManager = new CheckManager(plugin);
        checkManager.loadChecks();
        sql = new SQL(this);
        sql.createTableIfNotExists();
        scheduler = new Scheduler(this);
        scheduler.startSchedulers();
        packetCore = new PacketCore(SERVER_VERSION, this);
        packetCore.setupListenerOnlinePlayers();
        registerCommand();
    }

    public void unloadModules() {
        plugin.getCommand("hawk").setExecutor(null);
        HandlerList.unregisterAll(this);
        //HawkViolationEvent.getHandlerList().unregister(plugin);
        profiles = null;
        guiManager = null;
        lagCompensator = null;
        checkManager = null;
        plugin.packetCore.killListener();
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

    private void setupAPI() {
        HawkAPI.plugin = this;
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

    public Scheduler getScheduler() { return scheduler; }

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
}
