package essentials;

import essentials.special.ColorNick;
import io.anuke.arc.Core;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.EssentialConfig.realname;
import static io.anuke.mindustry.Vars.netServer;

public class EssentialPlayer{
    private static String url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("plugins/Essentials/player.sqlite3");
    private static int dbversion = 1;
    private static boolean queryresult;
    private static Connection conn;
    private static boolean loginresult;
    private static boolean registerresult;

    static void createNewDataFile(){
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);
            String makeplayer = "CREATE TABLE IF NOT EXISTS players (\n" +
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
                    "connected TEXT,\n" +
                    "accountid TEXT,\n" +
                    "accountpw TEXT\n" +
                    ");";
            Statement stmt = conn.createStatement();
            stmt.execute(makeplayer);
            stmt.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

	private static void createNewDatabase(String name, String uuid, String country, String language, String country_code, int joincount, int kickcount, String firstdate, String lastdate, String accountid, String accountpw) {
        try {
            String find = "SELECT * FROM players WHERE uuid = '"+uuid+"'";
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(url);
            Statement stmt  = conn.createStatement();
            ResultSet rs = stmt.executeQuery(find);
            if(!rs.next()){
                String sql = "INSERT INTO 'main'.'players' ('name', 'uuid', 'country', 'country_code', 'language', 'placecount', 'breakcount', 'killcount', 'deathcount', 'joincount', 'kickcount', 'level', 'exp', 'reqexp', 'reqtotalexp', 'firstdate', 'lastdate', 'lastplacename', 'lastbreakname', 'lastchat', 'playtime', 'attackclear', 'pvpwincount', 'pvplosecount', 'pvpbreakout', 'reactorcount', 'bantimeset', 'bantime', 'translate', 'crosschat', 'colornick', 'connected', 'accountid', 'accountpw') VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, uuid);
                pstmt.setString(3, country);
                pstmt.setString(4, country_code);
                pstmt.setString(5, language);
                pstmt.setInt(6, 0);
                pstmt.setInt(7, 0);
                pstmt.setInt(8, 0);
                pstmt.setInt(9, 0);
                pstmt.setInt(10, joincount);
                pstmt.setInt(11, kickcount);
                pstmt.setInt(12, 1);
                pstmt.setInt(13, 0);
                pstmt.setInt(14, 500);
                pstmt.setString(15, "0(500) / 500");
                pstmt.setString(16, firstdate);
                pstmt.setString(17, lastdate);
                pstmt.setString(18, "none");
                pstmt.setString(19, "none");
                pstmt.setString(20, "none");
                pstmt.setString(21, "00:00.00");
                pstmt.setInt(22, 0);
                pstmt.setInt(23, 0);
                pstmt.setInt(24, 0);
                pstmt.setInt(25, 0);
                pstmt.setInt(26, 0);
                pstmt.setString(27, "none");
                pstmt.setInt(28, 0);
                pstmt.setBoolean(29, false);
                pstmt.setBoolean(30, false);
                pstmt.setBoolean(31, false);
                pstmt.setBoolean(32, true);
                pstmt.setString(33, accountid);
                pstmt.setString(34, accountpw);
                pstmt.executeUpdate();
                pstmt.close();
                Global.log(name +" Player database created!");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e){
            e.printStackTrace();
        }
	}

	public static JSONObject getData(String uuid){
        JSONObject json = new JSONObject();

        try {
            String sql = "SELECT * FROM players WHERE uuid='"+uuid+"'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if(rs.next()){
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
                json.put("translate", rs.getBoolean("translate"));
                json.put("crosschat", rs.getBoolean("crosschat"));
                json.put("colornick", rs.getBoolean("colornick"));
                json.put("connected", rs.getBoolean("connected"));
            }
            rs.close();
            stmt.close();
            queryresult = true;
        } catch (Exception e){
            queryresult = false;
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
        writeData("UPDATE players SET bantime = '"+myTime+"', bantimeset = '"+bantimeset+"', WHERE uuid = '"+uuid+"'");
        netServer.admins.banPlayer(uuid);
    }

    private static final String v1sql = "ALTER TABLE players ADD COLUMN string;";
    //private static final String v2sql = "ALTER TABLE players ADD COLUMN string;";

    static void Upgrade() {
        /*
        if(dbversion < 2){
            try {
                Class.forName("org.sqlite.JDBC");
                Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                if(dbversion < 2){
                    stmt.execute(v1sql);
                }
                stmt.close();
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }

         */
    }
    static void openconnect(){
        try{
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void closeconnect(){
        try{
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	static void writeData(String sql){
        Runnable t = () -> {
            Thread.currentThread().setName("DB Thread");
            try {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.executeUpdate();
                pstmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Main.pool.execute(t);
	}

	static boolean register(Player player, String id, String pw, String pw2){
        Thread db = new Thread(() -> {
            Thread.currentThread().setName("DB Register Thread");
            if(!pw.equals(pw2)){
                player.sendMessage("The password isn't the same.");
                registerresult = false;
                return;
            }
            try {
                Class.forName("org.mindrot.jbcrypt.BCrypt");
                String hashed = BCrypt.hashpw(pw, BCrypt.gensalt(11));

                PreparedStatement pstm1 = conn.prepareStatement("SELECT * FROM players WHERE accountid = '"+id+"'");
                ResultSet rs1 = pstm1.executeQuery();
                if(rs1.next()){
                    if(rs1.getString("accountid").equals(id)){
                        player.sendMessage("[green][Essentials] [orange]This ID is already in use!");
                        registerresult = false;
                        return;
                    }
                }

                PreparedStatement pstm2 = conn.prepareStatement("SELECT * FROM players WHERE uuid = '"+player.uuid+"'");
                ResultSet rs2 = pstm2.executeQuery();
                String isuuid = null;
                while(rs2.next()){
                    isuuid = rs2.getString("uuid");
                }
                if(isuuid == null || isuuid.length() == 0){
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                    String nowString = now.format(dateTimeFormatter);
                    String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;

                    boolean isLocal = player.isLocal;

                    // Geolocation
                    String geo;
                    String geocode;
                    String lang;
                    Pattern p = null;
                    try { p = Pattern.compile("(^127\\.)|(^10\\.)|(^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.)|(^192\\.168\\.)");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    assert p != null;
                    Matcher m = p.matcher(ip);

                    if(m.find()){
                        isLocal = true;
                    }
                    if(isLocal) {
                        geo = "Local IP";
                        geocode = "LC";
                        lang = "en";
                    } else {
                        try {
                            String apiURL = "http://ipapi.co/"+ip+"/json";
                            URL url = new URL(apiURL);
                            HttpURLConnection con = (HttpURLConnection) url.openConnection();
                            con.setReadTimeout(5000);
                            con.setRequestMethod("POST");

                            boolean redirect = false;

                            int status = con.getResponseCode();
                            if (status != HttpURLConnection.HTTP_OK) {
                                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) redirect = true;
                            }

                            if (redirect) {
                                String newUrl = con.getHeaderField("Location");
                                String cookies = con.getHeaderField("Set-Cookie");

                                con = (HttpURLConnection) new URL(newUrl).openConnection();
                                con.setRequestProperty("Cookie", cookies);
                            }

                            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                            String inputLine;
                            StringBuilder response = new StringBuilder();
                            while ((inputLine = br.readLine()) != null) {
                                response.append(inputLine);
                            }
                            br.close();
                            JSONTokener parser = new JSONTokener(response.toString());
                            JSONObject result = new JSONObject(parser);

                            if(result.has("reserved")){
                                geo = "Local IP";
                                geocode = "LC";
                                lang = "en";
                            } else {
                                geo = result.getString("country_name");
                                geocode = result.getString("country");
                                lang = result.getString("languages").substring(0, 1);
                            }
                        } catch (IOException e) {
                            geo = "invalid";
                            geocode = "invalid";
                            lang = "en";
                        }
                    }
                    // Geolocation end

                    int timesjoined = Vars.netServer.admins.getInfo(player.uuid).timesJoined;
                    int timeskicked = Vars.netServer.admins.getInfo(player.uuid).timesKicked;

                    // Set non-color nickname
                    player.sendMessage("[green]Your nickname is now [white]"+player.name+".");

                    try {
                        createNewDatabase(player.name, player.uuid, geo, geocode, lang, timesjoined, timeskicked, nowString, nowString, id, hashed);
                        registerresult = true;
                    } catch (Exception e){
                        Call.onInfoMessage(player.con, "Player load failed!\nPlease submit this bug to the plugin developer!\n"+ Arrays.toString(e.getStackTrace()));
                        player.con.kick("You have been kicked due to a plugin error.");
                    }
                } else if (isuuid.length() > 1 || isuuid.equals(player.uuid)){
                    player.sendMessage("[green][Essentials] [orange]This account already exists!");
                    registerresult = false;
                } else {
                    registerresult = false;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        db.start();
        try{db.join();}catch (Exception ignored){}
        return registerresult;
    }

    static boolean login(Player player, String id, String pw) {
        Thread db = new Thread(() -> {
            try{
                PreparedStatement pstm = conn.prepareStatement("SELECT * FROM players WHERE accountid = ?");
                pstm.setString(1, id);
                ResultSet rs = pstm.executeQuery();
                if (rs.next()){
                    if (BCrypt.checkpw(pw, rs.getString("accountpw"))){
                        pstm = conn.prepareStatement("UPDATE players SET uuid = ?, connected = ? WHERE accountid = ? and accountpw = ?");
                        pstm.setString(1, player.uuid);
                        pstm.setBoolean(2, true);
                        pstm.setString(3, id);
                        pstm.setString(4, pw);
                        pstm.executeUpdate();
                        loginresult = true;
                    } else {
                        loginresult = false;
                    }
                } else {
                    loginresult = false;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        });

        db.start();
        try{db.join();}catch (Exception ignored){}
        return loginresult;
    }

    static void load(Player player, String id) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
        String nowString = now.format(dateTimeFormatter);

        // Write player connected
        writeData("UPDATE players SET connected = '1', lastdate = '"+nowString+"', uuid = '"+player.uuid+"' WHERE accountid = '"+id+"'");

        player.setTeam(Vars.defaultTeam);
        Call.onPlayerDeath(player);

        // Show motd
        JSONObject db = getData(player.uuid);

        String motd;
        if(db.getString("language").equals("KR")){
            motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd_ko.txt").readString();
        } else {
            motd = Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").readString();
        }
        int count = motd.split("\r\n|\r|\n").length;
        if(count > 10){
            Call.onInfoMessage(player.con, motd);
        } else {
            player.sendMessage(motd);
        }

        // Check if realname enabled
        if(realname){
            player.name = db.getString("name");
        }

        // Give join exp
        Thread expthread = new Thread(() -> EssentialExp.joinexp(player.uuid));
        expthread.start();

        // Color nickname
        boolean colornick = (boolean) db.get("colornick");
        if(realname && colornick){
            ColorNick.main(player);
        } else if(!realname && colornick){
            Global.logw("Color nickname must be enabled before 'realname' can be enabled.");

            writeData("UPDATE players SET colornick = '0' WHERE uuid = '"+player.uuid+"'");
        }
    }

	/*
    // TODO make getall function
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