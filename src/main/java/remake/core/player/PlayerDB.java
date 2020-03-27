package remake.core.player;

import remake.internal.CrashReport;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static remake.Main.database;
import static remake.Vars.playerData;

public class PlayerDB {
    public PlayerData get(String uuid){
        for(PlayerData p : playerData){
            if (p.uuid.equals(uuid)) return p;
        }
        return null;
    }

    public PlayerData load(String uuid) throws Exception{
        String sql = "SELECT * FROM players WHERE uuid=?";
        PreparedStatement pstmt = database.conn.prepareStatement(sql);
        pstmt.setString(1, uuid);
        ResultSet rs = pstmt.executeQuery();

        Field[] tree = PlayerData.class.getFields();

        if(rs.next()){
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
                    rs.getInt("bantimeset"),
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
        for(int a = 0;a<tree.length;a++){
            Class<?> field = tree[a].getType();
            Field base = data[a];
            if(String.class.isAssignableFrom(field)){
                pstmt.setString(a + 1, data[a].toString());
            } else if(boolean.class.isAssignableFrom(field)){
                pstmt.setBoolean(a + 1, data[a].getBoolean(data[a]));
            } else if(int.class.isAssignableFrom(field)){
                pstmt.setInt(a + 1, data[a].getInt(data[a]));
            } else if(Long.class.isAssignableFrom(field)){
                pstmt.setLong(a + 1, data[a].getLong(data[a].getLong(data[a])));
            }
        }

        pstmt.execute();
        pstmt.close();
        return true;
    }

    public boolean saveAll(){
        try {
            for(PlayerData p : playerData) save(p);
            return true;
        } catch (Exception e) {
            new CrashReport(e);
            return false;
        }
    }
}
