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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MuteManager implements Listener {

    private final Hawk hawk;
    private final Map<UUID, MuteEntry> mutes;
    private final File dataFile;

    public MuteManager(Hawk hawk) {
        this.hawk = hawk;
        mutes = new HashMap<>();
        dataFile = new File(hawk.getDataFolder().getAbsolutePath() + File.separator + "muted_players.txt");
        Bukkit.getPluginManager().registerEvents(this, hawk);
    }

    //since this will be called on the main thread, it's best to call this on plugin enable
    public void loadMutedPlayers() {
        if (!dataFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                dataFile.createNewFile();
            } catch (IOException e) {
                hawk.getLogger().severe("Failed to create muted_players.txt");
                e.printStackTrace();
                return;
            }
        }
        BufferedReader buf;
        try {
            buf = new BufferedReader(new FileReader(dataFile));
        } catch (FileNotFoundException e) {
            hawk.getLogger().severe("Failed to locate muted_players.txt");
            e.printStackTrace();
            return;
        }

        //PARSE FILE BEGIN
        String line = readLine(buf);
        long currTime = System.currentTimeMillis();
        while (line != null) {
            String[] parts = line.split(" ", 3);
            long expireTime = Long.parseLong(parts[1]);
            if (expireTime > currTime) {
                mutes.put(UUID.fromString(parts[0]), new MuteEntry(expireTime, parts[2]));
            }
            line = readLine(buf);
        }
        //PARSE FILE END

        try {
            buf.close();
        } catch (IOException e) {
            hawk.getLogger().severe("Failed to read muted_players.txt");
            e.printStackTrace();
        }
    }


    private String readLine(BufferedReader buf) {
        String result = null;
        try {
            result = buf.readLine();
        } catch (IOException e) {
            hawk.getLogger().severe("Failed to read muted_players.txt");
            e.printStackTrace();
        }
        return result;
    }

    public MuteEntry getMuteInfo(UUID uuid) {
        return mutes.get(uuid);
    }

    public void mute(UUID uuid, long expireTime, String reason) {
        MuteEntry muteEntry = new MuteEntry(expireTime, reason);
        mutes.put(uuid, muteEntry);
    }

    public void pardon(UUID uuid) {
        mutes.remove(uuid);
    }

    public void purgeBans() {
        mutes.clear();
    }

    //since this will be called on the main thread, it's best to call this on plugin disable
    public void saveMutedPlayers() {
        try (FileWriter fw = new FileWriter(dataFile, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            for (Map.Entry<UUID, MuteEntry> entry : mutes.entrySet()) {
                MuteEntry muteEntry = entry.getValue();
                out.println(entry.getKey() + " " + muteEntry.expireTime + " " + muteEntry.reason);
            }
        } catch (IOException e) {
            hawk.getLogger().severe("Failed to write to muted_players.txt");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        MuteEntry entry = getMuteInfo(e.getPlayer().getUniqueId());
        if (entry == null)
            return;
        if (entry.expireTime <= System.currentTimeMillis()) {
            pardon(e.getPlayer().getUniqueId());
            return;
        }
        e.setCancelled(true);
        e.getPlayer().sendMessage(ChatColor.RED + "You have been muted.");
    }

    public class MuteEntry {

        private final long expireTime;
        private final String reason;

        private MuteEntry(long expireTime, String reason) {
            this.expireTime = expireTime;
            this.reason = reason;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public String getReason() {
            return reason;
        }
    }

    public Map<UUID, MuteEntry> getMutes() {
        return mutes;
    }
}
