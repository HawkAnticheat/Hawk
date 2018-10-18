/*
 * This file is part of Hawk Anticheat.
 *
 * Hawk Anticheat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hawk Anticheat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Hawk Anticheat.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.islandscout.hawk.modules;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.HawkPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BanManager implements Listener {

    //I don't have intentions of making a "judgement day" system in Hawk, but if
    //you would like to think of this as one, then by all means, go ahead.

    private final Hawk hawk;
    private final List<BanEntry> bans; //Perhaps store cached data into a HashSet? Faster than binary search?
    private final File dataFile;

    public BanManager(Hawk hawk) {
        this.hawk = hawk;
        bans = new ArrayList<>();
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
        while (line != null) {
            String[] parts = line.split(" ", 3);
            long expireTime = Long.parseLong(parts[1]);
            if (expireTime > System.currentTimeMillis()) {
                bans.add(new BanEntry(UUID.fromString(parts[0]), expireTime, parts[2]));
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

        Collections.sort(bans); //binary search???
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
        BanEntry dummy = new BanEntry(uuid, 0, "");
        int index = Collections.binarySearch(bans, dummy);
        if (index < 0)
            return null;
        return bans.get(index);
    }

    public void ban(UUID uuid, long expireTime, String reason) {
        BanEntry banEntry = new BanEntry(uuid, expireTime, reason);
        int index = Collections.binarySearch(bans, banEntry);
        boolean alreadyBanned = index >= 0;
        if (!alreadyBanned) {
            index = -index - 1;
            bans.add(index, banEntry);
        } else {
            bans.set(index, banEntry);
        }
    }

    public void pardon(UUID uuid) {
        BanEntry banEntry = new BanEntry(uuid, 0, "");
        int index = Collections.binarySearch(bans, banEntry);
        if (index >= 0) {
            bans.remove(index);
        }
    }

    public void purgeBans() {
        bans.clear();
    }

    //since this will be called on the main thread, it's best to call this on plugin disable
    public void saveBannedPlayers() {
        try (FileWriter fw = new FileWriter(dataFile, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            for (BanEntry entry : bans) {
                out.println(entry.uuid + " " + entry.expireTime + " " + entry.reason);
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
        for (HawkPlayer pp : hawk.getHawkPlayers()) {
            //if(pp.canReceiveNotifications())
            //Debug.sendToPlayer(pp.getPlayer(), e.getPlayer() + " tried to join, but is banned!");
        }
    }

    public class BanEntry implements Comparable<BanEntry> {

        private final UUID uuid;
        private final long expireTime;
        private final String reason;

        private BanEntry(UUID uuid, long expireTime, String reason) {
            this.uuid = uuid;
            this.expireTime = expireTime;
            this.reason = reason;
        }

        public UUID getUuid() {
            return uuid;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public int compareTo(BanEntry other) {
            return this.uuid.compareTo(other.uuid);
        }
    }

    public List<BanEntry> getBans() {
        return bans;
    }
}
