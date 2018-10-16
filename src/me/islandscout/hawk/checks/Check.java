package me.islandscout.hawk.checks;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.events.Event;
import me.islandscout.hawk.events.external.HawkViolationEvent;
import me.islandscout.hawk.modules.CommandExecutor;
import me.islandscout.hawk.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

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
 * <p>
 * Make sure to register checks in CheckManager.
 */
public abstract class Check<E extends Event> {

    protected int id;
    private static int instances;
    protected boolean enabled;
    protected int cancelThreshold;
    protected int flagThreshold;
    protected final double vlPassMultiplier;
    protected final long flagCooldown; //in milliseconds
    protected static Hawk hawk;
    protected final String permission;
    protected final String name;
    protected final String configPath;
    protected final String flag;
    protected final List<String> punishCommands;
    protected final Map<UUID, Long> lastFlagTimes;

    /**
     * Default values set in these constructors. Configuration may override them.
     *
     * @param name             name of check
     * @param enabled          enable check
     * @param cancelThreshold  VL required to cancel
     * @param flagThreshold    VL required to flag
     * @param vlPassMultiplier VL pass multiplier (eg: 0.95)
     * @param flagCooldown     flag cooldown duration (in milliseconds)
     * @param flag             flag message
     * @param punishCommands   list of commands to run
     */
    Check(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {
        this.id = instances++;
        this.permission = Hawk.BASE_PERMISSION + ".bypass." + name;
        this.name = name;
        FileConfiguration hawkConfig = hawk.getConfig();
        FileConfiguration msgs = hawk.getMessages();
        configPath = "checks." + this.name + ".";
        this.enabled = ConfigHelper.getOrSetDefault(enabled, hawkConfig, configPath + "enabled");
        if (!(this instanceof Cancelless))
            this.cancelThreshold = ConfigHelper.getOrSetDefault(cancelThreshold, hawkConfig, configPath + "cancelThreshold");
        this.flagThreshold = ConfigHelper.getOrSetDefault(flagThreshold, hawkConfig, configPath + "flagThreshold");
        this.vlPassMultiplier = ConfigHelper.getOrSetDefault(vlPassMultiplier, hawkConfig, configPath + "vlPassMultiplier");
        this.flagCooldown = ConfigHelper.getOrSetDefault(flagCooldown, hawkConfig, configPath + "flagCooldown");
        if (punishCommands == null)
            punishCommands = Collections.emptyList();
        this.punishCommands = ConfigHelper.getOrSetDefault(new ArrayList<>(punishCommands), hawkConfig, configPath + "punishCommands");
        String msgPath = "flags." + this.name;
        this.flag = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault(flag, msgs, msgPath));
        this.lastFlagTimes = new HashMap<>();
        if (this instanceof Listener)
            Bukkit.getPluginManager().registerEvents((Listener) this, hawk);
        hawk.getCheckManager().getChecks().add(this);
    }

    public void checkEvent(E e) {
        if (e.getPlayer().hasPermission(permission) || hawk.getCheckManager().getExemptedPlayers().contains(e.getPlayer().getUniqueId()) || !enabled)
            return;
        check(e);
    }

    //assume player does not have permission to bypass and this check is enabled.
    protected abstract void check(E e);

    protected void punish(HawkPlayer offender, boolean tryCancel, E e, Placeholder... placeholders) {
        if (tryCancel && canCancel() && offender.getVL(this) >= cancelThreshold)
            e.setCancelled(true);
        punish(offender, placeholders);
    }

    private void punish(HawkPlayer pp, Placeholder... placeholders) {
        Player offender = pp.getPlayer();
        pp.incrementVL(this);

        flag(offender, pp, placeholders);

        CommandExecutor.runACommand(punishCommands, this, offender, pp, hawk, placeholders);
    }

    protected void reward(HawkPlayer pp) {
        pp.multiplyVL(this, vlPassMultiplier);
    }

    private void flag(Player offender, HawkPlayer pp, Placeholder... placeholders) {
        if (!canFlag())
            return;
        if (System.currentTimeMillis() - lastFlagTimes.getOrDefault(offender.getUniqueId(), 0L) < flagCooldown)
            return;
        if (pp.getVL(this) < flagThreshold)
            return;
        lastFlagTimes.put(offender.getUniqueId(), System.currentTimeMillis());
        String flag = this.flag;
        double tps = MathPlus.round(ServerUtils.getTps(), 2);
        int vl = pp.getVL(this);
        flag = flag.replace("%player%", offender.getName()).replace("%check%", this.name).replace("%tps%", tps + "").replace("%ping%", ServerUtils.getPing(offender) + "ms").replace("%vl%", vl + "");
        Violation violation = new Violation(pp, this, (short) vl);

        for (Placeholder placeholder : placeholders)
            flag = flag.replace("%" + placeholder.getKey() + "%", placeholder.getValue().toString());
        broadcastMessage(flag, violation);
        logToConsole(flag);
        logToFile(flag);


        if (hawk.getSql().isRunning())
            hawk.getSql().addToBuffer(violation);
        if (hawk.canCallBukkitEvents())
            Bukkit.getServer().getPluginManager().callEvent(new HawkViolationEvent(violation));
    }

    private void broadcastMessage(String message, Violation violation) {
        if (hawk.canSendJSONMessages()) {
            String offenderName = violation.getPlayer().getName();
            String command = Hawk.FLAG_CLICK_COMMAND.replace("%player%", offenderName);
            String commandPrompt = command.equals("") ? "" : "\n" + ChatColor.GRAY + "Click to run \"/" + command + "\"";
            JSONMessageSender msg = new JSONMessageSender(Hawk.FLAG_PREFIX + message);
            msg.setHoverMsg("Check: " + violation.getCheck() + "\nVL: " + violation.getVl() + "\nPing: " + violation.getPing() + "ms\nTPS: " + MathPlus.round(violation.getTps(), 2) + "\nPlayer: " + offenderName + commandPrompt);
            if (!commandPrompt.equals("")) msg.setClickCommand(command);
            for (HawkPlayer pp : hawk.getHawkPlayers()) {
                if (pp.canReceiveNotifications())
                    if (hawk.canPlaySoundOnFlag())
                        pp.getPlayer().playSound(pp.getLocation(), Sound.NOTE_PIANO, 1, 1);
                msg.sendMessage(pp.getPlayer());
            }
        } else {
            for (HawkPlayer pp : hawk.getHawkPlayers()) {
                if (pp.canReceiveNotifications())
                    if (hawk.canPlaySoundOnFlag())
                        pp.getPlayer().playSound(pp.getLocation(), Sound.NOTE_PIANO, 1, 1);
                pp.getPlayer().sendMessage(Hawk.FLAG_PREFIX + message);
            }
        }
    }

    private void logToConsole(String message) {
        Bukkit.getConsoleSender().sendMessage(Hawk.FLAG_PREFIX + message);
    }

    private void logToFile(String message) {
        hawk.getViolationLogger().logMessage(ChatColor.RESET + "" + message);
    }

    protected Object customSetting(String name, String localConfigPath, Object defaultValue) {
        return ConfigHelper.getOrSetDefault(defaultValue, hawk.getConfig(), configPath + localConfigPath + name);
    }

    public String getName() {
        return name;
    }

    public void setEnabled(boolean status) {
        enabled = status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean canCancel() {
        return cancelThreshold > -1;
    }

    public void setCancelThreshold(int cancelThreshold) {
        this.cancelThreshold = cancelThreshold;
    }

    public boolean canFlag() {
        return flagThreshold > -1;
    }

    public void setFlagThreshold(int flagThreshold) {
        this.flagThreshold = flagThreshold;
    }

    public int getCancelThreshold() {
        return cancelThreshold;
    }

    public int getFlagThreshold() {
        return flagThreshold;
    }

    public String getBypassPermission() {
        return permission;
    }

    public static void setHawkReference(Hawk plugin) {
        hawk = plugin;
    }

    public int getId() {
        return id;
    }

    //to be overridden by checks
    public void removeData(Player p) {}

    @Override
    public String toString() {
        return name;
    }
}

