package me.islandscout.hawk.checks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.modules.CommandExecutor;
import me.islandscout.hawk.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * The Check class provides essential functions and utilities for
 * validating events before they are processed by CraftBukkit.
 * Every check inheriting this class will have access to the JavaPlugin
 * instance and other functions in the anticheat's framework.
 * Every check inheriting this class will also have the ability to send
 * notification flags with custom placeholders and have their own
 * configurations. The default placeholders are %player%, %check%,
 * %ping%, %tps%, and %vl%. Checks may also implement Bukkit's Listener
 * interface for listening to Bukkit events and they do not need to
 * register themselves, since CheckManager already handles that.
 *
 * Make sure to register checks in CheckManager.
 */
public abstract class Check {

    protected boolean enabled;
    protected boolean cancel;
    protected boolean flaggable;
    protected double vlPassMultiplier;
    protected int minVlFlag;
    protected long flagCooldown; //in milliseconds
    public static Hawk hawk;
    protected final String permission;
    protected final String name;
    protected String flag;
    protected List<String> punishCommands;
    protected Map<UUID, Long> lastFlagTimes;

    /**
     * Default values set in these constructors. Configuration may override them.
     * @param name name of check
     * @param enabled enable check
     * @param cancelByDefault cancel by default
     * @param flagByDefault flag by default
     * @param vlPassMultiplier VL pass multiplier (eg: 0.95)
     * @param minVlFlag minimum VL to flag
     * @param flagCooldown flag cooldown duration (in milliseconds)
     * @param flag flag message
     * @param punishCommands list of commands to run
     */
    Check(String name, boolean enabled, boolean cancelByDefault, boolean flagByDefault, double vlPassMultiplier, int minVlFlag, long flagCooldown, String flag, List<String> punishCommands) {
        this.permission = Hawk.BASE_PERMISSION + ".bypassCheck." + name;
        this.name = name;
        FileConfiguration hawkConfig = hawk.getConfig();
        FileConfiguration msgs = hawk.getMessages();
        String path = "checks." + this.name + ".";
        this.enabled = ConfigHelper.getOrSetDefault(enabled, hawkConfig, path + "enabled");
        if(!(this instanceof Cancelless))
            this.cancel = ConfigHelper.getOrSetDefault(cancelByDefault, hawkConfig, path + "cancel");
        this.flaggable = ConfigHelper.getOrSetDefault(flagByDefault, hawkConfig, path + "flag");
        this.vlPassMultiplier = ConfigHelper.getOrSetDefault(vlPassMultiplier, hawkConfig, path + "vlPassMultiplier");
        this.minVlFlag = ConfigHelper.getOrSetDefault(minVlFlag, hawkConfig, path + "minVlFlag");
        this.flagCooldown = ConfigHelper.getOrSetDefault(flagCooldown, hawkConfig, path + "flagCooldown");
        if(punishCommands == null)
            punishCommands = Collections.emptyList();
        this.punishCommands = ConfigHelper.getOrSetDefault(punishCommands, hawkConfig, path + "punishCommands");
        path = "flags." + this.name;
        this.flag = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault(flag, msgs, path));
        this.lastFlagTimes = new HashMap<>();
        hawk.getCheckManager().getCheckList().add(this);
    }

    public void setEnabled(boolean status) {
        enabled = status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean canCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }

    public boolean isFlaggable() {
        return flaggable;
    }

    public void setFlaggable(boolean flaggable) {
        this.flaggable = flaggable;
    }

    public String getPermission() {
        return permission;
    }

    public void removeData(Player p) {
        //to be overridden by checks
    }

    void punish(Player offender, Placeholder... placeholders) {
        HawkPlayer pp = hawk.getHawkPlayer(offender);
        pp.incrementVL(this);

        flag(offender, placeholders);

        CommandExecutor.runACommand(punishCommands, this, pp, hawk, placeholders);
    }

    protected void reward(Player player) {
        HawkPlayer pp = hawk.getHawkPlayer(player);
        pp.multiplyVL(this, vlPassMultiplier);
    }

    private void flag(Player offender, Placeholder... placeholders) {
        if(!flaggable)
            return;
        if(System.currentTimeMillis() - lastFlagTimes.getOrDefault(offender.getUniqueId(), 0L) < flagCooldown)
            return;
        HawkPlayer pp = hawk.getHawkPlayer(offender);
        if(pp.getVL(this) < minVlFlag)
            return;
        lastFlagTimes.put(offender.getUniqueId(), System.currentTimeMillis());
        String flag = this.flag;
        double tps = MathPlus.round(ServerUtils.getTps(), 2);
        flag = flag.replace("%player%", offender.getName()).replace("%check%", this.name).replace("%tps%", tps + "").replace("%ping%", ServerUtils.getPing(offender) + "ms").replace("%vl%", pp.getVL(this) + "");

        for(Placeholder placeholder : placeholders)
            flag = flag.replace("%" + placeholder.getKey() + "%", placeholder.getValue().toString());
        broadcastMessage(flag);
        logToConsole(flag);
        logToFile(flag);
        /*
        if(Config.sqlEnabled) hawk.getSql().addToBuffer(violation);
        Bukkit.getServer().getPluginManager().callEvent(new HawkViolationEvent(violation));
        */
        //TODO: check if SQL is enabled, then send a query to SQL database
    }

    private void broadcastMessage(String message) {
        for(Player p : Bukkit.getOnlinePlayers()){
            if(p.hasPermission("hawk.notify")) {
                HawkPlayer admin = hawk.getHawkPlayer(p);
                if(admin.canReceiveFlags())
                    p.sendMessage(Hawk.FLAG_PREFIX + " " + ChatColor.RESET + message);
            }
        }
    }

    //TODO: Please work on this
    /*private void broadcastMessageJSON(String message, Violation violation) {
        String offenderName = Bukkit.getPlayer(violation.getPlayerUUID()).getName();
        String command = Config.notificationClickCommand.replace("%player%", offenderName);
        String commandPrompt = command.equals("") ? "" : "\n" + ChatColor.GRAY + "Click to doAction \"/" + command + "\"";
        JSONMessageSender msg = new JSONMessageSender(prefix + ChatColor.RESET + "" + message);
        msg.setHoverMsg("Check: " + violation.getCheck() + "\nVL: " + violation.getVl() + "\nPing: " + violation.getPing() + "ms\nTPS: " + plugin.getCheckManager().getLagCheck().getTps() + "\nPlayer: " + offenderName + commandPrompt);
        if(!commandPrompt.equals("")) msg.setClickCommand(command);
        for(Player admin : Bukkit.getOnlinePlayers()){
            if(!plugin.getHawkCommand().getNotify().containsKey(admin)) {
                plugin.getHawkCommand().getNotify().put(admin, true);
            }
            if(!admin.hasPermission("hawk.notify") || !plugin.getHawkCommand().getNotify().get(admin)) continue;
            msg.sendMessage(admin);
        }
    }*/

    private void logToConsole(String message) {
        Bukkit.getConsoleSender().sendMessage(Hawk.FLAG_PREFIX + " " + ChatColor.RESET + "" + message);
    }

    private void logToFile(String message) {
        hawk.getTextLogger().logMessage(ChatColor.RESET + "" + message);
    }

    @Override
    public String toString() {
        return name;
    }
}

