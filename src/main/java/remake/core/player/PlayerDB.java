package remake.core.player;

import mindustry.entities.type.Player;
import remake.external.Mail;
import remake.internal.CrashReport;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static remake.Main.*;
import static remake.PluginVars.playerData;

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

            // Field[] tree = PlayerData.class.getFields();

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
        Field[] tree = PlayerData.class.getFields();
        Field[] data = playerData.getClass().getFields();
        sql.append("UPDATE players SET ");

        for (Field f : tree){
            sql.append(f.getName()).append("=?,");
        }

        sql.deleteCharAt(sql.length() - 1);
        sql.append(" WHERE uuid=?");

        PreparedStatement pstmt = database.conn.prepareStatement(sql.toString());
        for (int a = 0; a < tree.length - 9; a++) {
            Class<?> field = tree[a].getType();
            Field base = data[a];
            if (String.class.isAssignableFrom(field)) {
                pstmt.setString(a + 1, base.toString());
            } else if (boolean.class.isAssignableFrom(field)) {
                pstmt.setBoolean(a + 1, base.getBoolean(base));
            } else if (int.class.isAssignableFrom(field)) {
                pstmt.setInt(a + 1, base.getInt(base));
            } else if (Long.class.isAssignableFrom(field)) {
                pstmt.setLong(a + 1, base.getLong(base));
            }
        }

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

        Field[] tree = PlayerData.class.getFields();
        sql.append("?,".repeat(Math.max(0, tree.length - 9)));
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");

        try {
            PreparedStatement pstmt = database.conn.prepareStatement(sql.toString());
            PlayerData newdata = playerCore.NewData(player.name, player.uuid, country, country_code, language, connected, connserver, permission, udid, email, accountid, accountpw);
            Field[] data = newdata.getClass().getFields();
            for (int a = 0; a < tree.length - 9; a++) {
                Class<?> field = tree[a].getType();
                Field base = data[a];
                if (String.class.isAssignableFrom(field)) {
                    pstmt.setString(a + 1, base.toString());
                } else if (Boolean.class.isAssignableFrom(field)) {
                    pstmt.setBoolean(a + 1, base.getBoolean(base));
                } else if (Integer.class.isAssignableFrom(field)) {
                    pstmt.setInt(a + 1, base.getInt(base));
                } else if (Long.class.isAssignableFrom(field)) {
                    pstmt.setString(a + 1, base.toString());
                }
            }
            return true;
        } catch (SQLException | IllegalAccessException e) {
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
