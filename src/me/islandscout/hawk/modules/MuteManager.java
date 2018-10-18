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
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MuteManager implements Listener {

    private final Hawk hawk;
    private final List<MuteEntry> mutes; //Perhaps store cached data into a HashSet? Faster than binary search?
    private final File dataFile;

    public MuteManager(Hawk hawk) {
        this.hawk = hawk;
        mutes = new ArrayList<>();
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
        while (line != null) {
            String[] parts = line.split(" ", 3);
            long expireTime = Long.parseLong(parts[1]);
            if (expireTime > System.currentTimeMillis()) {
                mutes.add(new MuteEntry(UUID.fromString(parts[0]), expireTime, parts[2]));
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

        Collections.sort(mutes); //binary search???
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
        MuteEntry dummy = new MuteEntry(uuid, 0, "");
        int index = Collections.binarySearch(mutes, dummy);
        if (index < 0)
            return null;
        return mutes.get(index);
    }

    public void mute(UUID uuid, long expireTime, String reason) {
        MuteEntry muteEntry = new MuteEntry(uuid, expireTime, reason);
        int index = Collections.binarySearch(mutes, muteEntry);
        boolean alreadyBanned = index >= 0;
        if (!alreadyBanned) {
            index = -index - 1;
            mutes.add(index, muteEntry);
        } else {
            mutes.set(index, muteEntry);
        }
    }

    public void pardon(UUID uuid) {
        MuteEntry muteEntry = new MuteEntry(uuid, 0, "");
        int index = Collections.binarySearch(mutes, muteEntry);
        if (index >= 0) {
            mutes.remove(index);
        }
    }

    public void purgeBans() {
        mutes.clear();
    }

    //since this will be called on the main thread, it's best to call this on plugin disable
    public void saveMutedPlayers() {
        try (FileWriter fw = new FileWriter(dataFile, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            for (MuteEntry entry : mutes) {
                out.println(entry.uuid + " " + entry.expireTime + " " + entry.reason);
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
        //Debug.sendToPlayer(e.getPlayer(), "You're not supposed to chat.");
    }

    public class MuteEntry implements Comparable<MuteEntry> {

        private final UUID uuid;
        private final long expireTime;
        private final String reason;

        private MuteEntry(UUID uuid, long expireTime, String reason) {
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
        public int compareTo(MuteEntry other) {
            return this.uuid.compareTo(other.uuid);
        }
    }

    public List<MuteEntry> getMutes() {
        return mutes;
    }
}
