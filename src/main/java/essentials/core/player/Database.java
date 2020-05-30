package essentials.core.player;

import essentials.internal.CrashReport;
import essentials.internal.exception.PluginException;
import org.h2.tools.Server;

import java.lang.reflect.Method;
import java.sql.*;

import static essentials.Main.config;
import static essentials.Main.root;

public class Database {
    public Method m;
    public Object service;
    public Connection conn;

    public Server server;

    public void create() {
        String data = "CREATE TABLE IF NOT EXISTS players (" +
                "name TEXT NOT NULL," +
                "uuid TEXT NOT NULL," +
                "country TEXT NOT NULL," +
                "country_code TEXT NOT NULL," +
                "language TEXT NOT NULL," +
                "isadmin TINYINT(4) NOT NULL," +
                "placecount INT(11) NOT NULL," +
                "breakcount INT(11) NOT NULL," +
                "killcount INT(11) NOT NULL," +
                "deathcount INT(11) NOT NULL," +
                "joincount INT(11) NOT NULL," +
                "kickcount INT(11) NOT NULL," +
                "level INT(11) NOT NULL," +
                "exp INT(11) NOT NULL," +
                "reqexp INT(11) NOT NULL," +
                "reqtotalexp TEXT NOT NULL," +
                "firstdate TEXT NOT NULL," +
                "lastdate TEXT NOT NULL," +
                "lastplacename TEXT NOT NULL," +
                "lastbreakname TEXT NOT NULL," +
                "lastchat TEXT NOT NULL," +
                "playtime TEXT NOT NULL," +
                "attackclear INT(11) NOT NULL," +
                "pvpwincount INT(11) NOT NULL," +
                "pvplosecount INT(11) NOT NULL," +
                "pvpbreakout INT(11) NOT NULL," +
                "reactorcount INT(11) NOT NULL," +
                "bantimeset TINYTEXT NOT NULL," +
                "bantime TINYTEXT NOT NULL," +
                "banned TINYINT(4) NOT NULL," +
                "translate TINYINT(4) NOT NULL," +
                "crosschat TINYINT(4) NOT NULL," +
                "colornick TINYINT(4) NOT NULL," +
                "connected TINYINT(4) NOT NULL," +
                "connserver TINYTEXT NOT NULL DEFAULT 'none'," +
                "permission TINYTEXT NOT NULL DEFAULT 'default'," +
                "mute TINYTEXT NOT NULL," +
                "alert TINYTEXT NOT NULL," +
                "udid TEXT NOT NULL," +
                "accountid TEXT NOT NULL," +
                "accountpw TEXT NOT NULL" +
                ")";

        try (PreparedStatement pstmt = conn.prepareStatement(data)) {
            pstmt.execute();
        } catch (SQLException e) {
            new CrashReport(e);
        }
    }

    public void connect(boolean isServer) throws SQLException {
        if (isServer) {
            org.h2.Driver.load();
            server = Server.createTcpServer("-tcpPort", "9079", "-tcpAllowOthers", "-tcpDaemon", "-baseDir", "./" + root.child("data").path(), "-ifNotExists");
            server.start();
            conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9079/player", "", "");
        } else {
            conn = DriverManager.getConnection(config.dbUrl());
        }
    }

    public void disconnect() throws SQLException {
        conn.close();
    }

    public void server_stop() {
        server.stop();
    }

    public void dispose() {
        try {
            disconnect();
            if (server != null) server_stop();
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    public void update() {
        // playtime HH:mm:ss -> long 0
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT uuid, playtime FROM players")) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString("playtime").contains(":")) {
                        try (PreparedStatement pstmt2 = conn.prepareStatement("UPDATE players SET playtime=? WHERE uuid=?")) {
                            pstmt2.setLong(1, 0);
                            pstmt2.setString(2, rs.getString("uuid"));
                            int result = pstmt2.executeUpdate();
                            if (result == 0) {
                                new CrashReport(new PluginException("Database update error"));
                                System.exit(1);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            new CrashReport(e);
        }
    }
}
