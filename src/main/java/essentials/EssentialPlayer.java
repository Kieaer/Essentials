package essentials;

import essentials.thread.GeoThread;
import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EssentialPlayer{
    static String url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("plugins/Essentials/player.sqlite3");
    private static int dbversion = 1;
    static void main(Player player){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
        String nowString = now.format(dateTimeFormatter);
        String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;

        boolean isLocal = player.isLocal;

        Runnable georun = new GeoThread(ip, isLocal);
        Thread geothread = new Thread(georun);
        try {
            geothread.start();
            geothread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        String geo = GeoThread.getgeo();
        String geocode = GeoThread.getgeocode();
        String languages = GeoThread.getlang();

        int timesjoined = Vars.netServer.admins.getInfo(player.uuid).timesJoined;
        int timeskicked = Vars.netServer.admins.getInfo(player.uuid).timesKicked;

        // Remove color nickname
        String changedname = player.name.replaceAll("\\[(.*?)]", "");

        // Set non-color nickname
        player.name = changedname;

        try {
            createNewDatabase(changedname, player.uuid, geo, geocode,
                    0, 0, 0, 0, timesjoined,
                    timeskicked, 1, 0, 500, "0(500) / 500", nowString, nowString, "none",
                    "none", "00:00.00", "none", 0, 0, 0,
                    0, 0, "none", 0, false, languages, false, false, true);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
	public static void createNewDatabase(String name, String uuid, String country, String country_code, int placecount, int breakcount, int killcount, int deathcount, int joincount, int kickcount, int level, int exp, int reqexp, String reqtotalexp, String firstdate, String lastdate, String lastplacename, String lastbreakname, String playtime, String lastchat, int attackclear, int pvpwincount, int pvplosecount, int pvpbreakout, int reactorcount, String bantimeset, int bantime, boolean translate, String language, boolean crosschat, boolean colornick, boolean connected) {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);
            if(conn != null){
                String sql = "CREATE TABLE IF NOT EXISTS players (\n" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                        "name TEXT,\n" +
                        "uuid TEXT,\n" +
                        "country TEXT,\n" +
                        "country_code TEXT,\n" +
                        "language TEXT,\n" +
                        "placecount INTEGER,\n" +
                        "breakcount INTEGER,\n" +
                        "killcount INTEGER,\n" +
                        "deathcount INTEGER,\n" +
                        "joincount INTEGER,\n" +
                        "kickcount INTEGER,\n" +
                        "level INTEGER,\n" +
                        "exp INTEGER,\n" +
                        "reqexp INTEGER,\n" +
                        "reqtotalexp TEXT,\n" +
                        "firstdate TEXT,\n" +
                        "lastdate TEXT,\n" +
                        "lastplacename TEXT,\n" +
                        "lastbreakname TEXT,\n" +
                        "lastchat TEXT,\n" +
                        "playtime TEXT,\n" +
                        "attackclear INTEGER,\n" +
                        "pvpwincount INTEGER,\n" +
                        "pvplosecount INTEGER,\n" +
                        "pvpbreakout INTEGER,\n" +
                        "reactorcount INTEGER,\n" +
                        "bantimeset TEXT,\n" +
                        "bantime INTEGER,\n" +
                        "translate TEXT,\n" +
                        "crosschat TEXT,\n" +
                        "colornick TEXT,\n" +
                        "connected TEXT\n" +
                        ");";
                Statement stmt = conn.createStatement();
                stmt.execute(sql);
                stmt.close();
            }
            if(dbversion < 2){
                /*
                try{
                    String sql = "ALTER TABLE players ADD COLUMN columnname TEXT";
                    assert conn != null;
                    Statement stmt = conn.createStatement();
                    stmt.execute(sql);
                    stmt.close();
                } catch (SQLException e){
                    e.printStackTrace();
                }
                */
            }
            String find = "SELECT * FROM players WHERE uuid = '"+uuid+"'";
            Class.forName("org.sqlite.JDBC");
            assert conn != null;
            Statement stmt  = conn.createStatement();
            ResultSet rs = stmt.executeQuery(find);
            if(!rs.next()){
                String sql = "INSERT INTO 'main'.'players' ('name', 'uuid', 'country', 'country_code', 'language', 'placecount', 'breakcount', 'killcount', 'deathcount', 'joincount', 'kickcount', 'level', 'exp', 'reqexp', 'reqtotalexp', 'firstdate', 'lastdate', 'lastplacename', 'lastbreakname', 'lastchat', 'playtime', 'attackclear', 'pvpwincount', 'pvplosecount', 'pvpbreakout', 'reactorcount', 'bantimeset', 'bantime', 'translate', 'crosschat', 'colornick', 'connected') VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, uuid);
                pstmt.setString(3, country);
                pstmt.setString(4, country_code);
                pstmt.setString(5, language);
                pstmt.setInt(6, placecount);
                pstmt.setInt(7, breakcount);
                pstmt.setInt(8, killcount);
                pstmt.setInt(9, deathcount);
                pstmt.setInt(10, joincount);
                pstmt.setInt(11, kickcount);
                pstmt.setInt(12, level);
                pstmt.setInt(13, exp);
                pstmt.setInt(14, reqexp);
                pstmt.setString(15, reqtotalexp);
                pstmt.setString(16, firstdate);
                pstmt.setString(17, lastdate);
                pstmt.setString(18, lastplacename);
                pstmt.setString(19, lastbreakname);
                pstmt.setString(20, lastchat);
                pstmt.setString(21, playtime);
                pstmt.setInt(22, attackclear);
                pstmt.setInt(23, pvpwincount);
                pstmt.setInt(24, pvplosecount);
                pstmt.setInt(25, pvpbreakout);
                pstmt.setInt(26, reactorcount);
                pstmt.setString(27, bantimeset);
                pstmt.setInt(28, bantime);
                pstmt.setBoolean(29, translate);
                pstmt.setBoolean(30, crosschat);
                pstmt.setBoolean(31, colornick);
                pstmt.setBoolean(32, connected);
                pstmt.executeUpdate();
                pstmt.close();
                conn.close();
                Log.info("[Essentials] Player database created!");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
        }
	}

	public static JSONObject getData(String uuid){
        String sql = "SELECT * FROM players WHERE uuid = '"+uuid+"'";
        JSONObject json = new JSONObject();

        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);
            Statement stmt  = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while(rs.next()){
                json.put("name", rs.getString("name"));
                json.put("uuid", rs.getString("uuid"));
                json.put("country", rs.getString("country"));
                json.put("country_code", rs.getString("country_code"));
                json.put("language", rs.getString("language"));
                json.put("placecount", rs.getInt("placecount"));
                json.put("breakcount", rs.getInt("breakcount"));
                json.put("killcount", rs.getInt("killcount"));
                json.put("deathcount", rs.getInt("deathcount"));
                json.put("joincount", rs.getInt("joincount"));
                json.put("kickcount", rs.getInt("kickcount"));
                json.put("level", rs.getInt("level"));
                json.put("exp", rs.getInt("exp"));
                json.put("reqexp", rs.getInt("reqexp"));
                json.put("reqtotalexp", rs.getString("reqtotalexp"));
                json.put("firstdate", rs.getString("firstdate"));
                json.put("lastdate", rs.getString("lastdate"));
                json.put("lastplacename", rs.getString("lastplacename"));
                json.put("lastbreakname", rs.getString("lastbreakname"));
                json.put("lastchat", rs.getString("lastchat"));
                json.put("playtime", rs.getString("playtime"));
                json.put("attackclear", rs.getInt("attackclear"));
                json.put("pvpwincount", rs.getInt("pvpwincount"));
                json.put("pvplosecount", rs.getInt("pvplosecount"));
                json.put("pvpbreakout", rs.getInt("pvpbreakout"));
                json.put("reactorcount", rs.getInt("reactorcount"));
                json.put("bantimeset", rs.getString("bantimeset"));
                json.put("bantime", rs.getInt("bantime"));
                json.put("translate", rs.getString("translate"));
                json.put("crosschat", rs.getString("crosschat"));
                json.put("colornick", rs.getString("colornick"));
                json.put("connected", rs.getString("connected"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return json;
    }

	static void addtimeban(String name, String uuid, int bantimeset){

	    // Write ban data
        String db = Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").readString();
        JSONTokener parser = new JSONTokener(db);
        JSONObject object = new JSONObject(parser);

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd a hh:mm.ss", Locale.ENGLISH);
        String myTime = now.format(dateTimeFormatter);

        SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd a hh:mm.ss", Locale.ENGLISH);
        Date d1;
        Calendar cal;
        String newTime = null;
        try {
            d1 = format.parse(myTime);
            cal = Calendar.getInstance();
            cal.setTime(d1);
            cal.add(Calendar.HOUR, bantimeset);
            newTime = format.format(cal.getTime());
        } catch (ParseException e1) {
            e1.printStackTrace();
        }

        JSONObject data1 = new JSONObject();
        data1.put("uuid", uuid);
        data1.put("date", newTime);
        data1.put("name", name);

        int i = 0;
        while(i<object.length()){
            i++;
        }

        object.put(String.valueOf(i), data1);

        Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").writeString(String.valueOf(object));

        // Write player data

        String sql = "UPDATE players SET bantime = ?, bantimeset = ?, WHERE uuid = ?";
        try{
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, myTime);
            pstmt.setInt(2, bantimeset);
            pstmt.setString(3, uuid);
            pstmt.executeUpdate();
            pstmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // todo make writedata function
    /*
	public static JSONObject writeData(String uuid, String data){
		String db = Core.settings.getDataDirectory().child("plugins/Essentials/players/"+uuid+".json").readString();
		JSONTokener parser = new JSONTokener(db);
		JSONObject object = new JSONObject(parser);
		JSONObject response = (JSONObject) object.get("data");
		//JSONObject write = object.put(data);
		return response;
	}

    // todo make getall function
	public static JSONObject getAll() throws FileNotFoundException {
		File dir = new File("plugins/Essentials/players");
		for (File file : dir.listFiles()) {
			Scanner s = new Scanner(file);
			JSONTokener parser = new JSONTokener(String.valueOf(s));
			JSONObject object = new JSONObject(parser);
			JSONObject response = (JSONObject) object.get("data");
			s.close();
		}

		return response;
	}
 */
}