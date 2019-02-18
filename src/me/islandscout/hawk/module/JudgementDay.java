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
import me.islandscout.hawk.util.ConfigHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.util.*;

public class JudgementDay {

    //Allows hourly, daily, and weekly schedule setup for punishing players.

    private Set<UUID> convicts;
    private int taskId;
    private Hawk hawk;
    private Schedule schedule;
    private boolean justExecuted;
    private String cmd;
    boolean enabled;

    public JudgementDay(Hawk hawk) {
        convicts = new HashSet<>();
        this.hawk = hawk;
        enabled = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "judgementDay.enabled");
        cmd = ConfigHelper.getOrSetDefault("ban %player%", hawk.getConfig(), "judgementDay.command");
        String ugh = ConfigHelper.getOrSetDefault("SUNDAY 0 0", hawk.getConfig(), "judgementDay.schedule");
        String[] please = ugh.split(" ");
        int sDayOfWeek = please[0].equals("*") ? -1 : DayOfWeek.valueOf(please[0].toUpperCase()).getValue();
        int sHour = please[1].equals("*") ? -1 : Integer.parseInt(please[1]);
        int sMinute = please[2].equals("*") ? -1 : Integer.parseInt(please[2]);
        schedule = new Schedule(sDayOfWeek, sHour, sMinute);
    }

    public void start() {
        if(!enabled)
            return;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(hawk, new Runnable() {
            @Override
            public void run() {

                if(schedule.isNow()) {
                    if(!justExecuted) {
                        punishTime();
                    }
                    justExecuted = true;
                }
                else {
                    justExecuted = false;
                }

            }
        }, 0L, 20L);
    }

    public void stop() {
        if(!enabled)
            return;
        Bukkit.getScheduler().cancelTask(taskId);
    }

    private void punishTime() {
        for(UUID uuid : convicts) {
            Player p = Bukkit.getPlayer(uuid);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
        }
    }

    public void add() {
        if(!enabled)
            return;

    }

    public void remove() {
        if(!enabled)
            return;

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
            if(minute != -1 && compareMinute != minute) {
                return false;
            }
            return true;
        }
    }
}
