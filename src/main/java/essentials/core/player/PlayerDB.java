package essentials.core.player;

import essentials.internal.CrashReport;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.BiConsumer;

import static essentials.Main.database;
import static essentials.Main.playerCore;
import static essentials.PluginVars.playerData;

public class PlayerDB {
    public PlayerData get(String uuid) {
        for (PlayerData p : playerData) {
            if (p.uuid.equals(uuid)) return p;
        }
        return new PlayerData(true);
    }

    public void remove(String uuid) {
        for (PlayerData p : playerData) {
            if (p.uuid.equals(uuid)) {
                playerData.remove(p);
                break;
            }
        }
    }

    // TODO more simple source
    public PlayerData load(String uuid, String... AccountID) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * FROM players WHERE uuid=?");
            if (AccountID != null && AccountID.length != 0) sql.append(" OR accountid=?");
            PreparedStatement pstmt = database.conn.prepareStatement(sql.toString());
            pstmt.setString(1, uuid);
            if (AccountID != null && AccountID.length != 0) pstmt.setString(2, AccountID[0]);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
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
                playerData.add(data);
                return data;
            }
        } catch (SQLException e) {
            new CrashReport(e);
        }
        return new PlayerData(true);
    }

    public boolean save(PlayerData playerData) throws Exception {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> js = playerData.toMap();
        if (js.get("name") == null) return false; // TODO Find null reason
        sql.append("UPDATE players SET ");

        int size = js.size() + 1;

        js.forEach((name, value) -> {
            sql.append(name).append("=?,");
        });

        sql.deleteCharAt(sql.length() - 1);
        sql.append(" WHERE uuid=?");

        PreparedStatement pstmt = database.conn.prepareStatement(sql.toString());
        js.forEach(new BiConsumer<>() {
            int index = 1;

            @Override
            public void accept(String s, Object o) {
                try {
                    if (o instanceof String) {
                        pstmt.setString(index, (String) o);
                    } else if (o instanceof Boolean) {
                        pstmt.setBoolean(index, (Boolean) o);
                    } else if (o instanceof Integer) {
                        pstmt.setInt(index, (Integer) o);
                    } else if (o instanceof Long) {
                        pstmt.setLong(index, (Long) o);
                    }
                } catch (SQLException e) {
                    new CrashReport(e);
                }
                index++;
            }
        });

        pstmt.setString(size, playerData.uuid);
        pstmt.execute();
        pstmt.close();
        return true;
    }

    public boolean saveAll() {
        try {
            for (PlayerData p : playerData) save(p);
            return true;
        } catch (Exception e) {
            new CrashReport(e);
            return false;
        }
    }

    public boolean register(String name, String uuid, String country, String country_code, String language, boolean connected, String connserver, String permission, Long udid, String accountid, String accountpw) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO players VALUES(");

        PlayerData newdata = playerCore.NewData(name, uuid, country, country_code, language, connected, connserver, permission, udid, accountid, accountpw);
        Map<String, Object> js = newdata.toMap();

        js.forEach((n, value) -> {
            sql.append("?,");
        });
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");

        try {
            PreparedStatement pstmt = database.conn.prepareStatement(sql.toString());

            js.forEach(new BiConsumer<>() {
                int index = 1;

                @Override
                public void accept(String s, Object o) {
                    try {
                        if (o instanceof String) {
                            pstmt.setString(index, (String) o);
                        } else if (o instanceof Boolean) {
                            pstmt.setBoolean(index, (Boolean) o);
                        } else if (o instanceof Integer) {
                            pstmt.setInt(index, (Integer) o);
                        } else if (o instanceof Long) {
                            pstmt.setLong(index, (Long) o);
                        }
                    } catch (SQLException e) {
                        new CrashReport(e);
                    }
                    index++;
                }
            });
            pstmt.execute();
            pstmt.close();
            return true;
        } catch (SQLException e) {
            new CrashReport(e);
            return false;
        }
    }
}
