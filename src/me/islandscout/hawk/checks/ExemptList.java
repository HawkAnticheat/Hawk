package me.islandscout.hawk.checks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExemptList {

    private List<UUID> players = new ArrayList<>();

    public void addPlayer(UUID uuid) {
        players.add(uuid);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public boolean containsPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    /*
    public boolean isExempted(UUID uuid, Check) {
        return true;
    }
     */

    public List<Player> getPlayers() {
        List<Player> ps = new ArrayList<>();
        for(UUID uuid : players) {
            ps.add(Bukkit.getPlayer(uuid));
        }
        return ps;
    }
}
