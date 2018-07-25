package me.islandscout.hawk.utils;

import org.bukkit.Bukkit;

import java.util.UUID;

public class Violation {

    private String playerUuidShort;
    private UUID playerUUID;
    private String check;
    private long time;
    private short ping;
    private short vl;
    private String server;

    public Violation(String playerUuid, String checkNameIEPermission, long time, short ping, short vl) {
        this.playerUuidShort = playerUuid.replace("-", "");
        this.playerUUID = UUID.fromString(playerUuid);
        this.check = checkNameIEPermission.substring(12);
        this.time = time;
        this.ping = ping;
        this.vl = vl;
        this.server = Bukkit.getServerName();
    }

    public String getPlayerUuid() {
        return playerUuidShort;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getCheck() {
        return check;
    }

    public long getTime() {
        return time;
    }

    public short getPing() {
        return ping;
    }

    public short getVl() {
        return vl;
    }

    public String getServer() {
        return server;
    }
}
