package essentials.special;

import arc.Core;
import essentials.core.PlayerDB.PlayerData;

import java.sql.*;
import java.util.ArrayList;

import static essentials.Global.config;
import static essentials.Global.nbundle;
import static essentials.core.PlayerDB.conn;
import static essentials.utils.Config.PluginData;

public class DBConvert {
    private ArrayList<PlayerData> data = new ArrayList<>();

    public DBConvert(){
        if(!config.isSqlite() && PluginData.getBoolean("sqlite")) SQLite2DB();
    }

    public void SQLite2DB() {
        try {
            String start = nbundle("db-transfer-start");
            String progress = nbundle("db-transfer-progress");
            String copy = nbundle("db-transfer-copy");
            String end = nbundle("db-transfer-end");

            // SQLite 데이터 가져오기
            Connection con = DriverManager.getConnection("jdbc:sqlite:" + Core.settings.getDataDirectory().child("mods/Essentials/data/player.sqlite3"));
            PreparedStatement prepared = con.prepareStatement("SELECT * FROM players");
            ResultSet rs = prepared.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            int size = rsmd.getColumnCount();
            int current = 0;
            System.out.print(start);

            while (rs.next()) {
                data.add(new PlayerData(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("uuid"),
                        rs.getString("country"),
                        rs.getString("country_code"),
                        rs.getString("language"),
                        rs.getBoolean("isadmin"),
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
                        rs.getLong("udid"),
                        rs.getString("accountid"),
                        rs.getString("accountpw")
                ));
                System.out.print("\r"+progress+" "+current+"/"+size);
            }
            rs.close();
            prepared.close();
            con.close();
            System.out.print("\n");

            // DB 붙여넣기
            String sql = "INSERT INTO players(id, name, uuid, country, country_code, language, isadmin, placecount, breakcount, killcount, deathcount, joincount, kickcount, level, exp, reqexp, reqtotalexp, firstdate, lastdate, lastplacename, lastbreakname, lastchat, playtime, attackclear, pvpwincount, pvplosecount, pvpbreakout, reactorcount, bantimeset, bantime, banned, translate, crosschat, colornick, connected, connserver, permission, udid, accountid, accountpw) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            prepared = conn.prepareStatement(sql);
            for (PlayerData data : data) {
                prepared.setInt(0, data.id);
                prepared.setString(1, data.name);
                prepared.setString(2, data.uuid);
                prepared.setString(3, data.country);
                prepared.setString(4, data.country_code);
                prepared.setString(5, data.language);
                prepared.setBoolean(6, data.isAdmin);
                prepared.setInt(7, data.placecount);
                prepared.setInt(8, data.breakcount);
                prepared.setInt(9, data.killcount); // killcount
                prepared.setInt(10, data.deathcount); // deathcount
                prepared.setInt(11, data.joincount);
                prepared.setInt(12, data.kickcount);
                prepared.setInt(13, data.level); // level
                prepared.setInt(14, data.exp); // exp
                prepared.setInt(15, data.reqexp); // reqexp
                prepared.setString(16, data.reqtotalexp); // reqtotalexp
                prepared.setString(17, data.firstdate);
                prepared.setString(18, data.lastdate);
                prepared.setString(19, data.lastplacename); // lastplacename
                prepared.setString(20, data.lastbreakname); // lastbreakname
                prepared.setString(21, data.lastchat); // lastchat
                prepared.setString(22, data.playtime); // playtime
                prepared.setInt(23, data.attackclear); // attackclear
                prepared.setInt(24, data.pvpwincount); // pvpwincount
                prepared.setInt(25, data.pvplosecount); // pvplosecount
                prepared.setInt(26, data.pvpbreakout); // pvpbreakcount
                prepared.setInt(27, data.reactorcount); // reactorcount
                prepared.setInt(28, data.bantimeset); // bantimeset
                prepared.setString(29, data.bantime); // bantime
                prepared.setBoolean(30, data.banned);
                prepared.setBoolean(31, data.translate); // translate
                prepared.setBoolean(32, data.crosschat); // crosschat
                prepared.setBoolean(33, data.colornick); // colornick
                prepared.setBoolean(34, data.connected); // connected
                prepared.setString(35, data.connserver); // connected server ip
                prepared.setString(36, data.permission); // set permission
                prepared.setLong(37, data.udid); // UDID
                prepared.setString(38, data.accountid);
                prepared.setString(39, data.accountpw);
                prepared.execute();
                System.out.print("\r"+copy+" "+current+"/"+size);
            }
            prepared.close();
            PluginData.put("sqlite", false);
            System.out.print("\n"+end+"\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}