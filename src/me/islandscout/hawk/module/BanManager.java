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
import org.bukkit.event.player.PlayerLoginEvent;

import java.io.*;
import java.util.*;

public class BanManager implements Listener {

    //I don't have intentions of making a "judgement day" system in Hawk, but if
    //you would like to think of this as one, then by all means, go ahead.

    private final Hawk hawk;
    private final Map<UUID, BanEntry> bans;
    private final File dataFile;

    public BanManager(Hawk hawk) {
        this.hawk = hawk;
        bans = new HashMap<>();
        dataFile = new File(hawk.getDataFolder().getAbsolutePath() + File.separator + "banned_players.txt");
        Bukkit.getPluginManager().registerEvents(this, hawk);
    }

    //since this will be called on the main thread, it's best to call this on plugin enable
    public void loadBannedPlayers() {
        if (!dataFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                dataFile.createNewFile();
            } catch (IOException e) {
                hawk.getLogger().severe("Failed to create banned_players.txt");
                e.printStackTrace();
                return;
            }
        }
        BufferedReader buf;
        try {
            buf = new BufferedReader(new FileReader(dataFile));
        } catch (FileNotFoundException e) {
            hawk.getLogger().severe("Failed to locate banned_players.txt");
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
                bans.put(UUID.fromString(parts[0]), new BanEntry(expireTime, parts[2]));
            }
            line = readLine(buf);
        }
        //PARSE FILE END

        try {
            buf.close();
        } catch (IOException e) {
            hawk.getLogger().severe("Failed to read banned_players.txt");
            e.printStackTrace();
        }
    }


    private String readLine(BufferedReader buf) {
        String result = null;
        try {
            result = buf.readLine();
        } catch (IOException e) {
            hawk.getLogger().severe("Failed to read banned_players.txt");
            e.printStackTrace();
        }
        return result;
    }

    public BanEntry getBanInfo(UUID uuid) {
        return bans.get(uuid);
    }

    public void ban(UUID uuid, long expireTime, String reason) {
        BanEntry banEntry = new BanEntry(expireTime, reason);
        bans.put(uuid, banEntry);
    }

    public void pardon(UUID uuid) {
        bans.remove(uuid);
    }

    public void purgeBans() {
        bans.clear();
    }

    //since this will be called on the main thread, it's best to call this on plugin disable
    public void saveBannedPlayers() {
        try (FileWriter fw = new FileWriter(dataFile, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            for (Map.Entry<UUID, BanEntry> entry : bans.entrySet()) {
                BanEntry banEntry = entry.getValue();
                out.println(entry.getKey() + " " + banEntry.expireTime + " " + banEntry.reason);
            }
        } catch (IOException e) {
            hawk.getLogger().severe("Failed to write to banned_players.txt");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onJoin(PlayerLoginEvent e) {
        BanEntry entry = getBanInfo(e.getPlayer().getUniqueId());
        if (entry == null)
            return;
        if (entry.expireTime <= System.currentTimeMillis()) {
            pardon(e.getPlayer().getUniqueId());
            return;
        }
        e.disallow(PlayerLoginEvent.Result.KICK_BANNED, ChatColor.translateAlternateColorCodes('&', entry.reason));
    }

    public class BanEntry {

        private final long expireTime;
        private final String reason;

        private BanEntry(long expireTime, String reason) {
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

    public Map<UUID, BanEntry> getBans() {
        return bans;
    }
}
