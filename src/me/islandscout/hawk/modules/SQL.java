package me.islandscout.hawk.modules;

import me.islandscout.hawk.Hawk;
import me.islandscout.hawk.utils.ConfigHelper;
import me.islandscout.hawk.utils.Violation;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQL {

    private Connection conn;
    private Hawk hawk;
    private ArrayList<Violation> violations = new ArrayList<>();
    private int postInterval;
    private boolean enabled;

    public SQL(Hawk hawk) {
        this.hawk = hawk;
        enabled = ConfigHelper.getOrSetDefault(false, hawk.getConfig(), "sql.enabled");
        postInterval = ConfigHelper.getOrSetDefault(60, hawk.getConfig(), "sql.updateInterval");
        String host = ConfigHelper.getOrSetDefault("127.0.0.1", hawk.getConfig(), "sql.host");
        String port = ConfigHelper.getOrSetDefault("3389", hawk.getConfig(), "sql.port");
        String user = ConfigHelper.getOrSetDefault("", hawk.getConfig(), "sql.username");
        String database = ConfigHelper.getOrSetDefault("", hawk.getConfig(), "sql.database");
        String password = ConfigHelper.getOrSetDefault("", hawk.getConfig(), "sql.password");
        this.openConnection(host, port, user, database, password);
    }

    private void openConnection(String hostname, String port, String username, String database, String password) {
        if(!enabled) return;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, username, password);
            hawk.getLogger().info("Connected to SQL server.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        if (conn != null) {
            try {
                conn.close();
                hawk.getLogger().info("Closed SQL connection.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void createTableIfNotExists() {
        if(!enabled) return;
        try {
            PreparedStatement create = conn.prepareStatement("CREATE TABLE IF NOT EXISTS `hawkviolations` ( `id` INT(8) NOT NULL AUTO_INCREMENT, `uuid` VARCHAR(32) NOT NULL , `check` VARCHAR(32) NOT NULL , `ping` INT(5) NOT NULL , `vl` INT(5) NOT NULL , `server` VARCHAR(255) NOT NULL , `time` TIMESTAMP NOT NULL , PRIMARY KEY (`id`))");
            create.executeUpdate();
            hawk.getLogger().info("SQL logging enabled successfully.");
        }
        catch (Exception e) {
            hawk.getLogger().warning("An error occurred while attempting to check if table \"hawkviolations\" exists!");
            e.printStackTrace();
        }
    }

    public void addToBuffer(Violation violation) {
        if(!enabled) return;
        violations.add(violation);
    }

    private short loop = 1;

    void postBuffer() {
        if(!enabled) return;
        if(loop < postInterval) {
            loop++;
            return;
        }
        loop = 1;
        if(violations.size() == 0) return;
        List<Violation> asyncList = new ArrayList<>();
        asyncList.addAll(violations);
        violations.clear();
        BukkitScheduler hawkLogger = Bukkit.getServer().getScheduler();
        hawkLogger.runTaskAsynchronously(hawk, () -> { //run async
            Timestamp timestamp;
            StringBuilder statementBuild = new StringBuilder();
            statementBuild.append("INSERT INTO `hawkviolations` (`id`, `uuid`, `check`, `ping`, `vl`, `server`, `time`) VALUES "); //begin statement

            int i = 0;
            for(Violation loopViolation : asyncList) { //generate the rest of the statement as bulk
                timestamp = new Timestamp(loopViolation.getTime());
                statementBuild.append("(NULL, '").append(loopViolation.getPlayerUuid()).append("', '").append(loopViolation.getCheck()).append("', '").append(loopViolation.getPing()).append("', '").append(loopViolation.getVl()).append("', '").append(loopViolation.getServer()).append("', '").append(timestamp).append("'), ");
                if(statementBuild.length() > 8192) { //if exceeds certain length, stop, then post, then make a new statement if there is still more data to send
                    post(statementBuild.substring(0, statementBuild.length() - 2));
                    statementBuild.setLength(0);
                    if(i < asyncList.size() - 1) statementBuild.append("INSERT INTO `hawkviolations` (`id`, `uuid`, `check`, `ping`, `vl`, `server`, `time`) VALUES ");
                }
                else if(i == asyncList.size() - 1) { //else, post when there are no more violations left in the buffer
                    post(statementBuild.substring(0, statementBuild.length() - 2));
                }
                i++;
            }
        });
    }

    private void post(String statement) {
        if(!enabled) return;
        try {
            PreparedStatement post = conn.prepareStatement(statement);
            post.executeUpdate();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
