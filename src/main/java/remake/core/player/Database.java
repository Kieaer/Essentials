package remake.core.player;

import remake.PluginVars;
import remake.internal.CrashReport;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;

import static remake.Main.config;
import static remake.Main.root;

public class Database {
    Class<?> cl = null;
    public Object service;
    public Connection conn;

    public void create() throws SQLException {
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
                "bantimeset INT(11) NOT NULL," +
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
                "email TEXT NOT NULL," +
                "accountid TEXT NOT NULL," +
                "accountpw TEXT NOT NULL" +
                ")";

        String ver = "CREATE TABLE IF NOT EXISTS `data` (`dbversion` TINYINT(4) NOT NULL)";

        PreparedStatement ptmt = conn.prepareStatement(data);
        ptmt.execute();

        ptmt = conn.prepareStatement(ver);
        ptmt.execute();
        ptmt.close();

        ptmt = conn.prepareStatement("SELECT * from data");
        ResultSet rs = ptmt.executeQuery();
        if (!rs.next()) {
            ptmt = conn.prepareStatement("INSERT INTO data VALUES(?)");
            ptmt.setInt(1, PluginVars.db_version);
            ptmt.execute();
            ptmt.close();
        }
    }

    public void connect() throws SQLException {
        conn = DriverManager.getConnection(config.DBurl, "", "");
    }

    public void disconnect() throws SQLException {
        conn.close();
    }

    public void server_start() {
        try {
            URLClassLoader cla = new URLClassLoader(new URL[]{root.child("Driver/h2-1.4.200.jar").file().toURI().toURL()}, this.getClass().getClassLoader());
            cl = Class.forName("org.h2.tools.Server", true, cla);
            Object obj = cl.getDeclaredConstructor().newInstance();

            String[] arr = {"-tcp", "-tcpPort", "9090", "-baseDir", "./" + root.child("data").path(), "-tcpAllowOthers"};
            Object[] parameter = new Object[]{arr};

            service = cl.getMethod("createTcpServer").invoke(obj, parameter);
            cl.getMethod("start").invoke(service, (Object[]) null);
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    public void server_stop() throws Exception {
        cl.getMethod("stop").invoke(service, (Object[]) null);
    }

    public void dispose() {
        try {
            disconnect();
            if (cl != null) server_stop();
        } catch (Exception e) {
            new CrashReport(e);
        }
    }
}
