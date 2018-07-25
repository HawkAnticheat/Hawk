package me.islandscout.hawk.api;

import me.islandscout.hawk.Hawk;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public final class HawkAPI {

    public HawkAPI() {}

    public static Hawk plugin;

    public static void addExempt(Player player) {
        plugin.getCheckManager().getExemptList().addPlayer(player.getUniqueId());
    }

    public static void removeExempt(Player player) {
        plugin.getCheckManager().getExemptList().removePlayer(player.getUniqueId());
    }

    public static void exemptContainsPlayer(Player player) {
        plugin.getCheckManager().getExemptList().containsPlayer(player.getUniqueId());
    }

    public static void addExempt(UUID playerUuid) {
        plugin.getCheckManager().getExemptList().addPlayer(playerUuid);
    }

    public static void removeExempt(UUID playerUuid) {
        plugin.getCheckManager().getExemptList().removePlayer(playerUuid);
    }

    public static void exemptContainsPlayer(UUID playerUuid) {
        plugin.getCheckManager().getExemptList().containsPlayer(playerUuid);
    }

    public static List<Player> getExemptPlayers() {
        return plugin.getCheckManager().getExemptList().getPlayers();
    }
}
