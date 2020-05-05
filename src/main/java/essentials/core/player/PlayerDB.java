package essentials.core.player;

import arc.struct.ArrayMap;
import arc.struct.ObjectMap;
import essentials.internal.CrashReport;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import static essentials.Main.*;

public class PlayerDB {
    public PlayerData get(String uuid) {
        for (PlayerData p : vars.playerData()) {
            if (p.uuid().equals(uuid)) return p;
        }
        return new PlayerData();
    }

    public void remove(String uuid) {
        vars.removePlayerData(p -> p.uuid().equals(uuid));
    }

    public PlayerData load(String uuid, String... AccountID) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM players WHERE uuid=?");
        if (AccountID != null && AccountID.length != 0) sql.append(" OR accountid=?");

        try (PreparedStatement pstmt = database.conn.prepareStatement(sql.toString())) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (AccountID != null && AccountID.length != 0) pstmt.setString(2, AccountID[0]);
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
                    vars.addPlayerData(data);
                    return data;
                }
            }
        } catch (SQLException e) {
            new CrashReport(e);
        }
        return new PlayerData();
    }

    public boolean save(PlayerData playerData) {
        StringBuilder sql = new StringBuilder();
        ArrayMap<String, Object> js = playerData.toMap();
        sql.append("UPDATE players SET ");

        int size = js.size + 1;


        js.forEach((s) -> {
            String buf = s.key.toLowerCase() + "=?, ";
            sql.append(buf);
        });

        sql.deleteCharAt(sql.length() - 2);
        sql.append(" WHERE uuid=?");

        try (PreparedStatement pstmt = database.conn.prepareStatement(sql.toString())) {
            js.forEach(new Consumer<ObjectMap.Entry<String, Object>>() {
                int index = 1;

                @Override
                public void accept(ObjectMap.Entry<String, Object> o) {
                    try {
                        if (o.value instanceof String) {
                            pstmt.setString(index, (String) o.value);
                        } else if (o.value instanceof Boolean) {
                            pstmt.setBoolean(index, (Boolean) o.value);
                        } else if (o.value instanceof Integer) {
                            pstmt.setInt(index, (Integer) o.value);
                        } else if (o.value instanceof Long) {
                            pstmt.setLong(index, (Long) o.value);
                        }
                    } catch (SQLException e) {
                        new CrashReport(e);
                    }
                    index++;
                }
            });

            pstmt.setString(size, playerData.uuid());
            pstmt.execute();
            return true;
        } catch (SQLException e) {
            new CrashReport(e);
            return false;
        }
    }

    public void saveAll() {
        for (PlayerData p : vars.playerData()) save(p);
    }

    public boolean register(String name, String uuid, String country, String country_code, String language, boolean connected, String connserver, String permission, Long udid, String accountid, String accountpw) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO players VALUES(");

        PlayerData newdata = playerCore.NewData(name, uuid, country, country_code, language, connected, connserver, permission, udid, accountid, accountpw);
        ArrayMap<String, Object> js = newdata.toMap();

        js.forEach((s) -> sql.append("?,"));
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");

        try (PreparedStatement pstmt = database.conn.prepareStatement(sql.toString())) {
            js.forEach(new Consumer<ObjectMap.Entry<String, Object>>() {
                int index = 1;

                @Override
                public void accept(ObjectMap.Entry<String, Object> o) {
                    try {
                        if (o.value instanceof String) {
                            pstmt.setString(index, (String) o.value);
                        } else if (o.value instanceof Boolean) {
                            pstmt.setBoolean(index, (Boolean) o.value);
                        } else if (o.value instanceof Integer) {
                            pstmt.setInt(index, (Integer) o.value);
                        } else if (o.value instanceof Long) {
                            pstmt.setLong(index, (Long) o.value);
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
