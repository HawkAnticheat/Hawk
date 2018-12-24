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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ViolationLogger {

    private final Hawk hawk;
    private File storageFile;
    private final List<String> buffer = new ArrayList<>();
    private final boolean enabled;

    public ViolationLogger(Hawk hawk, boolean enabled) {
        this.hawk = hawk;
        this.enabled = enabled;
    }

    public void prepare(File loggerFile) {
        storageFile = loggerFile;
        if (!storageFile.exists() && enabled) {
            try {
                //noinspection ResultOfMethodCallIgnored
                storageFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logMessage(String message) {
        if (!enabled) return;
        message = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('§', message));
        Calendar date = Calendar.getInstance();
        String hour = date.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + date.get(Calendar.HOUR_OF_DAY) : "" + date.get(Calendar.HOUR_OF_DAY);
        String minute = date.get(Calendar.MINUTE) < 10 ? "0" + date.get(Calendar.MINUTE) : "" + date.get(Calendar.MINUTE);
        String second = date.get(Calendar.SECOND) < 10 ? "0" + date.get(Calendar.SECOND) : "" + date.get(Calendar.SECOND);
        buffer.add("[" + (date.get(Calendar.MONTH) + 1) + "/" + date.get(Calendar.DAY_OF_MONTH) + "/" + date.get(Calendar.YEAR) + "] [" + hour + ":" + minute + ":" + second + "] " + message);
    }

    void updateFile() {
        if (!enabled) return;
        if (buffer.size() == 0) return;
        List<String> asyncList = new ArrayList<>(buffer);
        buffer.clear();
        BukkitScheduler hawkLogger = Bukkit.getServer().getScheduler();
        hawkLogger.runTaskAsynchronously(hawk, () -> {
            try (FileWriter fw = new FileWriter(storageFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {
                for (String aBuffer : asyncList) {
                    out.println(aBuffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
