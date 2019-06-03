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

package me.islandscout.hawk.module;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentScheduler {

    //Allows hourly, daily, and weekly schedule setup for punishing players.

    //TODO perhaps add BungeeCord compatibility? If one player gets banned on one server, notify other servers.

    private static final int AUTO_SAVE_INTERVAL = 60;

    private volatile Map<UUID, Pair<String, Boolean>> convicts; //UUID mapped to reason and whether they're authorized for punishment or not
    private int taskId;
    private int currentSecond;
    private Hawk hawk;
    private Schedule schedule;
    private boolean justExecuted;
    private String cmd;
    private boolean enabled;
    private boolean ignoreIfServerOverloaded;
    private int pingThreshold;
    private boolean requireAuthorization;
    private TextFileReader fileReader;

    private final boolean AUTO_SAVE;
    private final String DEFAULT_REASON;
    private final String USER_ADDED;
    private final String USER_REMOVED;
    private final String USER_AUTHORIZED;
    private final String USER_NOT_FOUND;
    private final String LIST_EMPTY;
    private final String DISABLED;
    private final String SERVER_OVERLOADED;
    private final String PING_LIMIT_EXCEEDED;

    public PunishmentScheduler(Hawk hawk) {
        convicts = new ConcurrentHashMap<>();
        this.hawk = hawk;

        USER_ADDED = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&6%player% has been added to the punishment system.", hawk.getMessages(), "punishmentScheduler.userAdded"));
        USER_REMOVED = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&6%player% has been removed from the punishment system.", hawk.getMessages(), "punishmentScheduler.userRemoved"));
        USER_AUTHORIZED = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&6%player% has been authorized for punishment.", hawk.getMessages(), "punishmentScheduler.userAuthorized"));
        USER_NOT_FOUND = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&6%player% was not found in the punishment system.", hawk.getMessages(), "punishmentScheduler.userNotFound"));
        LIST_EMPTY = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&6The list is empty.", hawk.getMessages(), "punishmentScheduler.listEmpty"));
        DISABLED = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&cError: The punishment system is disabled.", hawk.getMessages(), "punishmentScheduler.disabled"));
        SERVER_OVERLOADED = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&cError: The server is overloaded.", hawk.getMessages(), "punishmentScheduler.serverOverloaded"));
        PING_LIMIT_EXCEEDED = ChatColor.translateAlternateColorCodes('&', ConfigHelper.getOrSetDefault("&cError: %player%'s ping is too high.", hawk.getMessages(), "punishmentScheduler.pingTooHigh"));

        enabled = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "punishmentScheduler.enabled");
        cmd = ConfigHelper.getOrSetDefault("ban %player% %reason%", hawk.getConfig(), "punishmentScheduler.command");
        DEFAULT_REASON = ConfigHelper.getOrSetDefault("Illegal game modification", hawk.getConfig(), "punishmentScheduler.defaultReason");
        String rawSchedule = ConfigHelper.getOrSetDefault("SUNDAY 0 0", hawk.getConfig(), "punishmentScheduler.schedule");
        ignoreIfServerOverloaded = ConfigHelper.getOrSetDefault(true, hawk.getConfig(), "punishmentScheduler.ignoreIfServerOverloaded");
        pingThreshold = ConfigHelper.getOrSetDefault(-1, hawk.getConfig(), "punishmentScheduler.pingThreshold");
        requireAuthorization = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "punishmentScheduler.requireAuthorization");
        AUTO_SAVE = ConfigHelper.getOrSetDefault(true, hawk.getConfig(), "punishmentScheduler.autoSave");

        String[] schedule = rawSchedule.split(" ");
        int sDayOfWeek = schedule[0].equals("*") ? -1 : DayOfWeek.valueOf(schedule[0].toUpperCase()).getValue();
        int sHour = schedule[1].equals("*") ? -1 : Integer.parseInt(schedule[1]);
        int sMinute = schedule[2].equals("*") ? -1 : Integer.parseInt(schedule[2]);
        this.schedule = new Schedule(sDayOfWeek, sHour, sMinute);

        fileReader = new TextFileReader(hawk, "convicts.txt");
    }

    public void start() {
        if(!enabled)
            return;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(hawk, () -> {

            if(schedule.isNow()) {
                if(!justExecuted) {
                    punishTime();
                }
                justExecuted = true;
            }
            else {
                justExecuted = false;
            }

            currentSecond++;
            if(AUTO_SAVE && currentSecond % AUTO_SAVE_INTERVAL == 0) {
                saveAsynchronously();
            }

        }, 0L, 20L);
    }

    public void stop() {
        if(!enabled)
            return;
        Bukkit.getScheduler().cancelTask(taskId);
    }

    private void punishTime() {
        for (UUID currUuid : convicts.keySet()) {
            Pair<String, Boolean> pair = convicts.get(currUuid);
            if (pair.getValue()) {
                OfflinePlayer p = Bukkit.getOfflinePlayer(currUuid);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()).replace("%reason%", pair.getKey()));
                convicts.remove(currUuid);
            }

        }
    }

    public void add(Player p, String reason) {
        if(ignoreIfServerOverloaded && ServerUtils.getStress() > 1) {
            hawk.broadcastAlertToAdmins(SERVER_OVERLOADED);
            return;
        }
        if(pingThreshold > -1 && ServerUtils.getPing(p) > pingThreshold) {
            hawk.broadcastAlertToAdmins(PING_LIMIT_EXCEEDED.replace("%player%", p.getName()));
            return;
        }
        boolean authorized = !requireAuthorization;
        String processedReason = reason == null ? DEFAULT_REASON : reason;
        convicts.put(p.getUniqueId(), new Pair<>(processedReason, authorized));
        hawk.broadcastAlertToAdmins(USER_ADDED.replace("%player%", p.getName()));
    }

    public void remove(OfflinePlayer p) {
        convicts.remove(p.getUniqueId());
        hawk.broadcastAlertToAdmins(USER_REMOVED.replace("%player%", p.getName()));
    }

    public void authorize(OfflinePlayer p) {
        UUID uuid = p.getUniqueId();
        if(convicts.containsKey(uuid)) {
            convicts.get(uuid).setValue(true);
            hawk.broadcastAlertToAdmins(USER_AUTHORIZED.replace("%player%", p.getName()));
        }
        else {
            hawk.broadcastAlertToAdmins(USER_NOT_FOUND.replace("%player%", p.getName()));
        }
    }

    public void info(CommandSender admin, OfflinePlayer target) {
        Pair<String, Boolean> info = convicts.get(target.getUniqueId());
        if(info == null) {
            admin.sendMessage(USER_NOT_FOUND.replace("%player%", target.getName()));
            return;
        }
        admin.sendMessage(ChatColor.GOLD + "Punishment information about " + target.getName() + ":");
        admin.sendMessage(ChatColor.GOLD + "Authorized: " + (info.getValue() ? ChatColor.GREEN: ChatColor.GRAY) + "" + info.getValue());
        admin.sendMessage(ChatColor.GOLD + "Reason: " + ChatColor.RESET + info.getKey());
    }

    public void list(CommandSender admin) {
        if(convicts.size() == 0) {
            admin.sendMessage(LIST_EMPTY);
            return;
        }
        List<String> names = new ArrayList<>();
        for(UUID uuid : convicts.keySet()) {
            boolean authorized = convicts.get(uuid).getValue();
            names.add((authorized ? ChatColor.GREEN : ChatColor.GRAY) + Bukkit.getOfflinePlayer(uuid).getName());
        }
        admin.sendMessage(String.join(", ", names));
    }

    public void load() {
        Bukkit.getScheduler().runTaskAsynchronously(hawk, () -> {
            try {
                fileReader.load();
                String input = fileReader.readLine();
                while(input != null) {
                    String[] parts = input.split(" ", 3);

                    if(parts.length < 3)
                        continue;

                    UUID uuid = UUID.fromString(parts[0]);
                    boolean authorized = Boolean.parseBoolean(parts[1]);
                    String reason = parts[2];

                    convicts.put(uuid, new Pair<>(reason, authorized));

                    input = fileReader.readLine();
                }
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    //Do NOT call on plugin disable if you don't want errors!
    public void saveAsynchronously() {
        Bukkit.getScheduler().runTaskAsynchronously(hawk, this::saveSynchronously);
    }

    public void saveSynchronously() {
        try {
            List<String> data = new ArrayList<>();

            for(UUID uuid : convicts.keySet()) {
                Pair<String, Boolean> value = convicts.get(uuid);
                String line = uuid + " " + value.getValue() + " " + value.getKey();
                data.add(line);
            }

            fileReader.overwrite(data);
            //hawk.getLogger().info("Successfully saved users for PunishmentScheduler");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean status) {
        if(!status)
            stop();
        enabled = status;
    }

    public Map<UUID, Pair<String, Boolean>> getConvicts() {
        return convicts;
    }

    private class Schedule {

        private int dayOfWeek;
        private int hour;
        private int minute;

        Schedule(int dayOfWeek, int hour, int minute) {
            this.dayOfWeek = dayOfWeek;
            this.hour = hour;
            this.minute = minute;
        }

        boolean isNow() {
            Calendar now = Calendar.getInstance();
            int compareDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
            int compareHour = now.get(Calendar.HOUR_OF_DAY);
            int compareMinute = now.get(Calendar.MINUTE);
            if(dayOfWeek != -1 && compareDayOfWeek != dayOfWeek) {
                return false;
            }
            if(hour != -1 && compareHour != hour) {
                return false;
            }
            return minute == -1 || compareMinute == minute;
        }
    }
}
