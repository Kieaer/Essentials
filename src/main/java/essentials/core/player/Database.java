package essentials.core.player;

import arc.struct.Array;
import arc.struct.ArrayMap;
import arc.struct.ObjectMap;
import essentials.internal.CrashReport;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

import static essentials.Main.config;
import static essentials.Main.root;

public class Database {
    public Method m;
    public Object service;
    public Connection conn;
    public Class<?> cl = null;

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

    public void connect() throws SQLException {
        conn = DriverManager.getConnection(config.dbUrl());
    }

    public void disconnect() throws SQLException {
        conn.close();
    }

    public void server_start() {
        // TODO H2 library 사용
        try {
            URLClassLoader cla = new URLClassLoader(new URL[]{root.child("Driver/h2-1.4.200.jar").file().toURI().toURL()}, this.getClass().getClassLoader());
            cl = Class.forName("org.h2.tools.Server", true, cla);
            Object obj = cl.getDeclaredConstructor().newInstance();

            m = cl.getMethod("createTcpServer", String[].class);
            service = m.invoke(obj, new Object[]{new String[]{"-tcp", "-tcpPort", "9090", "-baseDir", "./" + root.child("data").path(), "-tcpAllowOthers"}});

            m = cl.getMethod("start");
            m.invoke(service);
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    public void server_stop() throws Exception {
        m = cl.getMethod("stop");
        m.invoke(service);
        cl = null;
    }

    public void dispose() {
        try {
            disconnect();
            if (cl != null) server_stop();
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    public void LegacyUpgrade() {
        Array<PlayerData> buffer = new Array<>();
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM players");
             ResultSet rs = pstmt.executeQuery();
             Statement sm = conn.createStatement()) {
            while (rs.next()) {
                try {
                    try {
                        LocalTime lc = LocalTime.parse(rs.getString("playtime"), DateTimeFormatter.ofPattern("HH:mm.ss"));
                        PreparedStatement update = conn.prepareStatement("UPDATE players SET playtime=? WHERE uuid=?");
                        update.setString(1, lc.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                        update.setString(2, rs.getString("uuid"));
                        update.execute();
                        update.close();
                    } catch (DateTimeParseException ignored) {
                    }

                    PlayerData data = new PlayerData(
                            rs.getString("name"),
                            rs.getString("uuid"),
                            rs.getString("country"),
                            rs.getString("country_code"),
                            rs.getString("language"),
                            rs.getBoolean("isAdmin"),
                            rs.getInt("placecount"),
                            rs.getInt("breakcount"),
                            rs.getInt("killcount"),
                            rs.getInt("deathcount"),
                            rs.getInt("joincount"),
                            rs.getInt("kickcount"),
                            rs.getInt("level"),
                            rs.getInt("exp"),
                            rs.getInt("reqexp"),
                            rs.getString("reqtotalexp"),
                            rs.getString("firstdate"),
                            rs.getString("lastdate"),
                            rs.getString("lastplacename"),
                            rs.getString("lastbreakname"),
                            rs.getString("lastchat"),
                            rs.getString("playtime"),
                            rs.getInt("attackclear"),
                            rs.getInt("pvpwincount"),
                            rs.getInt("pvplosecount"),
                            rs.getInt("pvpbreakout"),
                            rs.getInt("reactorcount"),
                            rs.getString("bantimeset"),
                            rs.getString("bantime"),
                            rs.getBoolean("banned"),
                            rs.getBoolean("translate"),
                            rs.getBoolean("crosschat"),
                            rs.getBoolean("colornick"),
                            rs.getBoolean("connected"),
                            rs.getString("connserver"),
                            rs.getString("permission"),
                            rs.getBoolean("mute"),
                            rs.getBoolean("alert"),
                            rs.getLong("udid"),
                            rs.getString("accountid"),
                            rs.getString("accountpw")
                    );
                    buffer.add(data);
                } catch (Exception e) {
                    LoggerFactory.getLogger(Database.class).info("Database", e);
                    break;
                }
            }

            sm.execute("DROP TABLE players");
            create();

            for (PlayerData p : buffer) {
                StringBuilder sql = new StringBuilder();
                sql.append("INSERT INTO players VALUES(");

                ArrayMap<String, Object> js = p.toMap();

                js.forEach((s) -> sql.append("?,"));
                sql.deleteCharAt(sql.length() - 1);
                sql.append(")");


                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    js.forEach(new Consumer<ObjectMap.Entry<String, Object>>() {
                        int index = 1;

                        @Override
                        public void accept(ObjectMap.Entry<String, Object> o) {
                            try {
                                if (o.value instanceof String) {
                                    ps.setString(index, (String) o.value);
                                } else if (o.value instanceof Boolean) {
                                    ps.setBoolean(index, (Boolean) o.value);
                                } else if (o.value instanceof Integer) {
                                    ps.setInt(index, (Integer) o.value);
                                } else if (o.value instanceof Long) {
                                    ps.setLong(index, (Long) o.value);
                                }
                            } catch (SQLException e) {
                                new CrashReport(e);
                            }
                            index++;
                        }
                    });
                    ps.execute();
                } catch (SQLException e) {
                    new CrashReport(e);
                }
            }
        } catch (Exception e) {
            new CrashReport(e);
        }
    }
}
