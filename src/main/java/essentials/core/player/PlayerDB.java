package essentials.core.player;

import essentials.external.Mail;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.entities.type.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static essentials.Main.*;
import static essentials.PluginVars.playerData;

public class PlayerDB {
    Map<String, String> email = new HashMap<>();

    public PlayerData get(String uuid) {
        for (PlayerData p : playerData) {
            if (p.uuid.equals(uuid)) return p;
        }
        return new PlayerData(true);
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
                        rs.getString("email"),
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

    public boolean register(Player player, String country, String country_code, String language, boolean connected, String connserver, String permission, Long udid, String email, String accountid, String accountpw) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO players VALUES(");

        PlayerData newdata = playerCore.NewData(player.name, player.uuid, country, country_code, language, connected, connserver, permission, udid, email, accountid, accountpw);
        Map<String, Object> js = newdata.toMap();

        js.forEach((name, value) -> {
            sql.append("?,");
        });
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");
        Log.info(sql.toString());

        try {
            PreparedStatement pstmt = database.conn.prepareStatement(sql.toString());

            js.forEach(new BiConsumer<>() {
                int index = 1;

                @Override
                public void accept(String s, Object o) {
                    Log.info("index: " + index + " name:" + s + " data:" + o);
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
            //Log.info(pstmt.toString());
            pstmt.execute();
            pstmt.close();
            return true;
        } catch (SQLException e) {
            new CrashReport(e);
            return false;
        }
    }

    public boolean email(Player player, String id, String pw, String email) {
        StringBuilder key = new StringBuilder();
        for (int a = 0; a <= 6; a++) {
            int n = (int) (Math.random() * 10);
            key.append(n);
        }
        // TODO email 인증키 입력
        Mail mail = new Mail(config.emailserver, config.emailport, config.emailAccountID, config.emailPassword, "sender", "target", email, "subject", "text");
        return mail.send();
    }

    public boolean verify_mail(Player player, String id, String authkey) {
        // TODO email 인증키 확인
        String key = email.get(id);
        return key.equals(authkey);
    }
}
