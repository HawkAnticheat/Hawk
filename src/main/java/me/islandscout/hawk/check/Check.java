/*
 * This file is part of Hawk Anticheat.
 * Copyright (C) 2018 Hawk Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.check;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import me.islandscout.hawk.event.Event;
import me.islandscout.hawk.event.bukkit.HawkFlagEvent;
import me.islandscout.hawk.module.CommandExecutor;
import me.islandscout.hawk.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

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

    protected boolean enabled;
    protected int cancelThreshold;
    protected int flagThreshold;
    protected final double vlPassMultiplier;
    protected long flagCooldown; //in milliseconds
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
        this.permission = Hawk.BASE_PERMISSION + ".bypass." + name;
        this.name = name;
        FileConfiguration checkConfig = hawk.getChecksConfig();
        FileConfiguration msgs = hawk.getMessages();
        configPath = this.name + ".";
        this.enabled = ConfigHelper.getOrSetDefault(enabled, checkConfig, configPath + "enabled");
        if (!(this instanceof Cancelless))
            this.cancelThreshold = ConfigHelper.getOrSetDefault(cancelThreshold, checkConfig, configPath + "cancelThreshold");
        this.flagThreshold = ConfigHelper.getOrSetDefault(flagThreshold, checkConfig, configPath + "flagThreshold");
        this.vlPassMultiplier = ConfigHelper.getOrSetDefault(vlPassMultiplier, checkConfig, configPath + "vlPassMultiplier");
        this.flagCooldown = ConfigHelper.getOrSetDefault(flagCooldown, checkConfig, configPath + "flagCooldown");
        if (punishCommands == null)
            punishCommands = Collections.emptyList();
        this.punishCommands = ConfigHelper.getOrSetDefault(new ArrayList<>(punishCommands), checkConfig, configPath + "punishCommands");
        String msgPath = "flags." + this.name;
        this.flag = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault(flag, msgs, msgPath));
        this.lastFlagTimes = new HashMap<>();
        if (this instanceof Listener)
            Bukkit.getPluginManager().registerEvents((Listener) this, hawk);
        hawk.getCheckManager().getChecks().add(this);
    }

    public void checkEvent(E e) {
        boolean exempt = hawk.getCheckManager().getExemptedPlayers().contains(e.getPlayer().getUniqueId());
        boolean forced = hawk.getCheckManager().getForcedPlayers().contains(e.getPlayer().getUniqueId());
        if (!enabled || ((e.getPlayer().hasPermission(permission) || exempt) && !forced))
            return;
        check(e);
    }

    //assume player does not have permission to bypass and this check is enabled.
    protected abstract void check(E e);

    protected void punish(HawkPlayer offender, boolean tryCancel, E e, Placeholder... placeholders) {
        punish(offender, 1, tryCancel, e, placeholders);
    }

    protected void punish(HawkPlayer offender, double vlAmnt, boolean tryCancel, E e, Placeholder... placeholders) {
        if (tryCancel && canCancel() && offender.getVL(this) >= cancelThreshold)
            e.setCancelled(true);
        punish(offender, vlAmnt, placeholders);
    }

    private void punish(HawkPlayer pp, double vlAmnt, Placeholder... placeholders) {
        Player offender = pp.getPlayer();
        pp.addVL(this, vlAmnt);

        flag(offender, pp, placeholders);

        hawk.getCommandExecutor().runACommand(punishCommands, this, vlAmnt, offender, pp, hawk, placeholders);
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

        final String finalFlag = flag;
        new BukkitRunnable() {
            public void run() {
                HawkFlagEvent event = new HawkFlagEvent(violation);
                Bukkit.getServer().getPluginManager().callEvent(event);

                if(!event.isCancelled()) {
                    //Running asynchronous so flagging does not cause lag on check thread.
                    Hawk.getPlugin().getExecutorThread().execute(() -> {
                        broadcastMessage(finalFlag, violation);
                        logToConsole(finalFlag);
                        logToFile(finalFlag);


                        if (hawk.getSQLModule().isRunning())
                            hawk.getSQLModule().addToBuffer(violation);
                    });
                }
            }
        }.runTask(Hawk.getPlugin());
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
                if (pp.canReceiveAlerts()) {
                    if (hawk.canPlaySoundOnFlag())
                        pp.getPlayer().playSound(pp.getPosition().toLocation(pp.getWorld()), Sound.NOTE_PIANO, 1, 1);
                    msg.sendMessage(pp.getPlayer());
                }
            }
        } else {
            for (HawkPlayer pp : hawk.getHawkPlayers()) {
                if (pp.canReceiveAlerts()) {
                    if (hawk.canPlaySoundOnFlag())
                        pp.getPlayer().playSound(pp.getPosition().toLocation(pp.getWorld()), Sound.NOTE_PIANO, 1, 1);
                    pp.getPlayer().sendMessage(Hawk.FLAG_PREFIX + message);
                }
            }
        }
        hawk.getBungeeBridge().sendAlertForBroadcast(message);
    }

    private void logToConsole(String message) {
        Bukkit.getConsoleSender().sendMessage(Hawk.FLAG_PREFIX + message);
    }

    private void logToFile(String message) {
        hawk.getViolationLogger().logMessage(ChatColor.RESET + "" + message);
    }

    protected Object customSetting(String name, String localConfigPath, Object defaultValue) {
        return ConfigHelper.getOrSetDefault(defaultValue, hawk.getChecksConfig(), configPath + localConfigPath + "." + name);
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
        return cancelThreshold > -1 && !(this instanceof Cancelless);
    }

    public void setCancelThreshold(int cancelThreshold) {
        this.cancelThreshold = cancelThreshold;
    }

    public boolean canFlag() {
        return flagThreshold > -1;
    }

    public int getFlagThreshold() {
        return flagThreshold;
    }

    public void setFlagThreshold(int flagThreshold) {
        this.flagThreshold = flagThreshold;
    }

    public long getFlagCooldown() {
        return flagCooldown;
    }

    public void setFlagCooldown(int flagCooldown) {
        this.flagCooldown = flagCooldown;
    }

    public int getCancelThreshold() {
        return cancelThreshold;
    }

    public String getBypassPermission() {
        return permission;
    }

    public static void setHawkReference(Hawk plugin) {
        hawk = plugin;
    }

    //to be overridden by checks
    public void removeData(Player p) {}

    @Override
    public String toString() {
        return name;
    }
}

