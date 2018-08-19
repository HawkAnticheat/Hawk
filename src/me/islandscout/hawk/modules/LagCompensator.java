package me.islandscout.hawk.modules;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.utils.ConfigHelper;
import me.islandscout.hawk.utils.LocationTime;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.*;

public class LagCompensator {

    //https://developer.valvesoftware.com/wiki/Lag_compensation
    //https://www.youtube.com/watch?v=6EwaW2iz4iA
    //http://www.gabrielgambetta.com/lag-compensation.html
    //http://www.gabrielgambetta.com/client-side-prediction-live-demo.html
    
    private Map<UUID, List<LocationTime>> locationTimes;
    private int historySize;
    private int pingOffset;
    private static final int RESOLUTION = 40;

    public LagCompensator(Hawk hawk) {
        this.locationTimes = new HashMap<>();
        historySize = ConfigHelper.getOrSetDefault(20, hawk.getConfig(), "lagCompensation.historySize");
        pingOffset = ConfigHelper.getOrSetDefault(175, hawk.getConfig(), "lagCompensation.pingOffset");
    }

    //uses linear interpolation to get the best location
    public Location getHistoryLocation(int rewindMillisecs, Player player) {
        List<LocationTime> times = locationTimes.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        if(times == null) {
            return player.getLocation();
        }
        int rewindTime = rewindMillisecs + pingOffset; //player a + avg processing time.
        for(int i = times.size() - 1; i >= 0; i--) { //loop backwards
            int elapsedTime = (int)(currentTime - times.get(i).getTime());
            if(elapsedTime >= rewindTime) {
                if(i == times.size() - 1) {
                    return times.get(i).getLocation();
                }
                double nextMoveWeight = (elapsedTime - rewindTime) / (double)(elapsedTime - (currentTime - times.get(i + 1).getTime()));
                Location before = times.get(i).getLocation().clone();
                Location after = times.get(i + 1).getLocation();
                Vector interpolate = after.toVector().subtract(before.toVector());
                interpolate.multiply(nextMoveWeight);
                before.add(interpolate);
                return before;
            }
        }
        return player.getLocation(); //can't find a suitable position
    }

    /*
    public Location getHistoryLocation(int rewindMillisecs, Player player) {
        List<LocationTime> times = locationTimes.get(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        if(times == null) {
            return player.getLocation();
        }
        for(int i = times.size() - 1; i >= 0; i--) { //loop backwards
            if(currentTime - times.get(i).getTime() >= rewindMillisecs + pingOffset) { //player a + avg processing time.
                Debug.broadcastMessage(currentTime - times.get(i).getTime() + "ms");
                return times.get(i).getLocation();
            }
        }
        return null; //can't find a suitable position
    }
    */

    public void processPosition(Location loc, Player p) {
        List<LocationTime> times = locationTimes.getOrDefault(p.getUniqueId(), new ArrayList<>());
        long currTime = System.currentTimeMillis();
        if(times.size() > 0 && currTime - times.get(times.size() - 1).getTime() < RESOLUTION)
            return;
        times.add(new LocationTime(currTime, loc));
        if(times.size() > historySize) times.remove(0);
        locationTimes.put(p.getUniqueId(), times);
    }

    public int getHistorySize() {
        return historySize;
    }

    public int getPingOffset() {
        return pingOffset;
    }
}
