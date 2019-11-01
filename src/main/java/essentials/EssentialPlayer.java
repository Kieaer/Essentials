package essentials;

import essentials.special.ColorNick;
import io.anuke.arc.Core;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
import org.json.JSONArray;
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
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.EssentialConfig.*;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.mindustry.Vars.playerGroup;

public class EssentialPlayer{
    private static int dbversion = 1;
    private static boolean queryresult;
    public static Connection conn;
    private static boolean loginresult;
    private static boolean registerresult;
    private static ArrayList<Thread> griefthread = new ArrayList<>();

    static void createNewDataFile(){
        try {
            String sql = null;
            if(sqlite){
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection(url);
                sql = "CREATE TABLE IF NOT EXISTS players (\n" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                        "name TEXT,\n" +
                        "uuid TEXT,\n" +
                        "country TEXT,\n" +
                        "country_code TEXT,\n" +
                        "language TEXT,\n" +
                        "isadmin TEXT,\n" +
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
                        "bantimeset INTEGER,\n" +
                        "bantime TEXT,\n" +
                        "translate TEXT,\n" +
                        "crosschat TEXT,\n" +
                        "colornick TEXT,\n" +
                        "connected TEXT,\n" +
                        "accountid TEXT,\n" +
                        "accountpw TEXT\n" +
                        ");";
            } else {
                if(!dbid.isEmpty()){
                    Class.forName("org.mariadb.jdbc.Driver");
                    Class.forName("com.mysql.jdbc.Driver");
                    conn = DriverManager.getConnection(url, dbid, dbpw);
                    sql = "CREATE TABLE IF NOT EXISTS `players` (\n" +
                            "`id` INT(11) NOT NULL AUTO_INCREMENT,\n" +
                            "`name` TEXT NULL DEFAULT NULL,\n" +
                            "`uuid` VARCHAR(12) NULL DEFAULT NULL,\n" +
                            "`country` TEXT NULL DEFAULT NULL,\n" +
                            "`country_code` TEXT NULL DEFAULT NULL,\n" +
                            "`language` TEXT NULL DEFAULT NULL,\n" +
                            "`isadmin` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`placecount` INT(11) NULL DEFAULT NULL,\n" +
                            "`breakcount` INT(11) NULL DEFAULT NULL,\n" +
                            "`killcount` INT(11) NULL DEFAULT NULL,\n" +
                            "`deathcount` INT(11) NULL DEFAULT NULL,\n" +
                            "`joincount` INT(11) NULL DEFAULT NULL,\n" +
                            "`kickcount` INT(11) NULL DEFAULT NULL,\n" +
                            "`level` INT(11) NULL DEFAULT NULL,\n" +
                            "`exp` INT(11) NULL DEFAULT NULL,\n" +
                            "`reqexp` INT(11) NULL DEFAULT NULL,\n" +
                            "`reqtotalexp` TEXT NULL DEFAULT NULL,\n" +
                            "`firstdate` TEXT NULL DEFAULT NULL,\n" +
                            "`lastdate` TEXT NULL DEFAULT NULL,\n" +
                            "`lastplacename` TEXT NULL DEFAULT NULL,\n" +
                            "`lastbreakname` TEXT NULL DEFAULT NULL,\n" +
                            "`lastchat` TEXT NULL DEFAULT NULL,\n" +
                            "`playtime` TEXT NULL DEFAULT NULL,\n" +
                            "`attackclear` INT(11) NULL DEFAULT NULL,\n" +
                            "`pvpwincount` INT(11) NULL DEFAULT NULL,\n" +
                            "`pvplosecount` INT(11) NULL DEFAULT NULL,\n" +
                            "`pvpbreakout` INT(11) NULL DEFAULT NULL,\n" +
                            "`reactorcount` INT(11) NULL DEFAULT NULL,\n" +
                            "`bantimeset` INT(11) NULL DEFAULT NULL,\n" +
                            "`bantime` TINYTEXT NULL DEFAULT NULL,\n" +
                            "`translate` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`crosschat` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`colornick` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`connected` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`accountid` TEXT NULL DEFAULT NULL,\n" +
                            "`accountpw` TEXT NULL DEFAULT NULL,\n" +
                            "PRIMARY KEY (`id`)\n" +
                            ")\n" +
                            "COLLATE='utf8_general_ci'\n" +
                            "ENGINE=InnoDB\n" +
                            "AUTO_INCREMENT=1\n" +
                            ";";
                } else {
                    Global.loge("Database address isn't set!");
                }
            }

            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.close();
        } catch (Exception ex){
            printStackTrace(ex);
        }
    }

	private static void createNewDatabase(String name, String uuid, String country, String country_code, String language, Boolean isAdmin, int joincount, int kickcount, String firstdate, String lastdate, String accountid, String accountpw) {
        try {
            String find = "SELECT * FROM players WHERE uuid = '"+uuid+"'";
            Statement stmt  = conn.createStatement();
            ResultSet rs = stmt.executeQuery(find);
            if(!rs.next()){
                String sql;
                if(sqlite){
                    sql = "INSERT INTO 'main'.'players' ('name', 'uuid', 'country', 'country_code', 'language', 'isadmin', 'placecount', 'breakcount', 'killcount', 'deathcount', 'joincount', 'kickcount', 'level', 'exp', 'reqexp', 'reqtotalexp', 'firstdate', 'lastdate', 'lastplacename', 'lastbreakname', 'lastchat', 'playtime', 'attackclear', 'pvpwincount', 'pvplosecount', 'pvpbreakout', 'reactorcount', 'bantimeset', 'bantime', 'translate', 'crosschat', 'colornick', 'connected', 'accountid', 'accountpw') VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                } else {
                    sql = "INSERT INTO players(name, uuid, country, country_code, language, isadmin, placecount, breakcount, killcount, deathcount, joincount, kickcount, level, exp, reqexp, reqtotalexp, firstdate, lastdate, lastplacename, lastbreakname, lastchat, playtime, attackclear, pvpwincount, pvplosecount, pvpbreakout, reactorcount, bantimeset, bantime, translate, crosschat, colornick, connected, accountid, accountpw) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                }
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, name);
                pstmt.setString(2, uuid);
                pstmt.setString(3, country);
                pstmt.setString(4, country_code);
                pstmt.setString(5, language);
                pstmt.setBoolean(6, isAdmin);
                pstmt.setInt(7, 0); // placecount
                pstmt.setInt(8, 0); // breakcount
                pstmt.setInt(9, 0); // killcount
                pstmt.setInt(10, 0); // deathcount
                pstmt.setInt(11, joincount);
                pstmt.setInt(12, kickcount);
                pstmt.setInt(13, 1); // level
                pstmt.setInt(14, 0); // exp
                pstmt.setInt(15, 500); // reqexp
                pstmt.setString(16, "0(500) / 500"); // reqtotalexp
                pstmt.setString(17, firstdate);
                pstmt.setString(18, lastdate);
                pstmt.setString(19, "none"); // lastplacename
                pstmt.setString(20, "none"); // lastbreakname
                pstmt.setString(21, "none"); // lastchat
                pstmt.setString(22, "00:00.00"); // playtime
                pstmt.setInt(23, 0); // attackclear
                pstmt.setInt(24, 0); // pvpwincount
                pstmt.setInt(25, 0); // pvplosecount
                pstmt.setInt(26, 0); // pvpbreakcount
                pstmt.setInt(27, 0); // reactorcount
                pstmt.setInt(28, 0); // bantimeset
                pstmt.setString(29, "none"); // bantime
                pstmt.setBoolean(30, false); // translate
                pstmt.setBoolean(31, false); // crosschat
                pstmt.setBoolean(32, false); // colornick
                pstmt.setBoolean(33, true); // connected
                pstmt.setString(34, accountid);
                pstmt.setString(35, accountpw);
                pstmt.executeUpdate();
                pstmt.close();
                Global.log(name +" Player database created!");
            }
            rs.close();
            stmt.close();
        } catch (Exception e){
            printStackTrace(e);
        }
	}

	public static JSONObject getData(String uuid){
        JSONObject json = new JSONObject();
        try {
            String sql = "SELECT * FROM players WHERE uuid='"+uuid+"'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                json.put("id", rs.getInt("id"));
                json.put("name", rs.getString("name"));
                json.put("uuid", rs.getString("uuid"));
                json.put("country", rs.getString("country"));
                json.put("country_code", rs.getString("country_code"));
                json.put("language", rs.getString("language"));
                json.put("isadmin", rs.getBoolean("isadmin"));
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
                json.put("bantime", rs.getString("bantime"));
                json.put("translate", rs.getBoolean("translate"));
                json.put("crosschat", rs.getBoolean("crosschat"));
                json.put("colornick", rs.getBoolean("colornick"));
                json.put("connected", rs.getBoolean("connected"));
                json.put("accountid", rs.getString("accountid"));
                json.put("accountpw", rs.getString("accountpw"));
            }
            rs.close();
            stmt.close();
            queryresult = true;
        } catch (Exception e){
            printStackTrace(e);
            queryresult = false;
        }
        return json;
    }

	public static void addtimeban(String name, String uuid, int bantimeset){
	    // Write ban data
        String db = Core.settings.getDataDirectory().child("mods/Essentials/banned.json").readString();
        JSONTokener parser = new JSONTokener(db);
        JSONArray object = new JSONArray(parser);

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
            printStackTrace(e1);
        }

        JSONObject data1 = new JSONObject();
        data1.put("uuid", uuid);
        data1.put("date", newTime);
        data1.put("name", name);

        object.put(data1);

        Core.settings.getDataDirectory().child("mods/Essentials/banned.json").writeString(String.valueOf(object));

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
                printStackTrace(e);
            }
        }

         */
    }
    static void openconnect(){
        try{
            if(sqlite){
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection(url);
                Global.log("Database type: SQLite");
            } else {
                Class.forName("org.mariadb.jdbc.Driver");
                Class.forName("com.mysql.jdbc.Driver");
                if(!dbid.isEmpty()){
                    conn = DriverManager.getConnection(url, dbid, dbpw);
                    Global.log("Database type: MariaDB/MySQL");
                } else {
                    conn = DriverManager.getConnection(url);
                    Global.log("Database type: Invalid");
                }
            }
        } catch (Exception e) {
            printStackTrace(e);
        }
    }

    static void closeconnect(){
        try{
            conn.close();
        } catch (Exception e) {
            printStackTrace(e);
        }
    }

	static void writeData(String sql){
        Thread t = new Thread(() -> {
            Thread.currentThread().setName("DB Thread");
            try {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.executeUpdate();
                pstmt.close();
            } catch (Exception e) {
                Global.loge(sql);
                printStackTrace(e);
            }
        });
        t.start();
	}

	static boolean register(Player player, String id, String pw, String pw2){
        Thread db = new Thread(() -> {
            Thread.currentThread().setName("DB Register Thread");
            // Check password security
            // 영문(대/소문자), 숫자, 특수문자 조합, 7~20자리
            String pwPattern = "^(?=.*\\d)(?=.*[~`!@#$%\\^&*()-])(?=.*[a-z])(?=.*[A-Z]).{7,20}$";
            Matcher matcher = Pattern.compile(pwPattern).matcher(pw);

            // 같은 문자 4개이상 사용 불가
            pwPattern = "(.)\\1\\1\\1";
            Matcher matcher2 = Pattern.compile(pwPattern).matcher(pw);

            // 비밀번호가 비밀번호 재확인 문자열과 똑같지 않을경우
            if(!pw.equals(pw2)){
                player.sendMessage("[green][Essentials] [sky]The password isn't the same.\n" +
                        "[green][Essentials] [sky]비밀번호가 똑같지 않습니다.");
                registerresult = false;
                return;
            }

            // 정규식에 맞지 않을경우
            if(!matcher.matches()){
                player.sendMessage("[green][Essentials] [sky]The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!\n" +
                        "[green][Essentials] [sky]비밀번호는 7~20자 내외로 설정해야 하며, 영문과 숫자, 특수문자를 포함해야 합니다!");
                registerresult = false;
                return;
            }

            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            if(matcher2.find()){
                player.sendMessage("[green][Essentials] [sky]The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!\n" +
                        "[green][Essentials] [sky]비밀번호는 7~20자 내외로 설정해야 하며, 영문과 숫자, 특수문자를 포함해야 합니다!");
                registerresult = false;
                return;
            }

            // 비밀번호와 ID가 완전히 같은경우
            if(pw.contains(id)){
                player.sendMessage("[green][Essentials] [sky]Passwords can't be set similar to ID!\n" +
                        "[green][Essentials] [sky]비밀번호는 ID는 비슷하게 설정할 수 없습니다!");
                registerresult = false;
                return;
            }

            // 비밀번호에 공백이 있을경우
            if(pw.contains(" ")){
                player.sendMessage("[green][Essentials] [sky]Password must not contain spaces!\n" +
                        "[green][Essentials] [sky]비밀번호에는 공백이 있으면 안됩니다!");
                registerresult = false;
                return;
            }

            // 비밀번호 형식이 "<비밀번호>" 일경우
            if(pw.matches("<(.*?)>")){
                player.sendMessage("[green][Essentials] [green]<[sky]password[green]>[sky] format isn't allowed!\n" +
                        "[green][Essentials] [sky]Use /register accountname password password\n" +
                        "[green][Essentials] [green]<[sky]비밀번호[green]>[sky] 형식은 허용되지 않습니다!\n" +
                        "[green][Essentials] [sky]/register accountname password password 형식으로 사용하세요.");
                player.sendMessage("");
                registerresult = false;
                return;
            }
            // Check password security end

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
                        printStackTrace(e);
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
                                lang = result.getString("languages").substring(0, 2);
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

                    player.sendMessage("[green]Your nickname is now [white]"+player.name+".");

                    try {
                        createNewDatabase(player.name, player.uuid, geo, geocode, lang, player.isAdmin, timesjoined, timeskicked, nowString, nowString, id, hashed);
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
                printStackTrace(e);
            }
        });
        db.start();
        try{db.join();}catch (Exception e){printStackTrace(e);}
        return registerresult;
    }

    static boolean register(Player player){
        Thread db = new Thread(() -> {
            Thread.currentThread().setName("DB Register Thread");
            try {
                if(!getData(player.uuid).toString().equals("{}")){
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
                    try {
                        p = Pattern.compile("(^127\\.)|(^10\\.)|(^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.)|(^192\\.168\\.)");
                    } catch (Exception e) {
                        printStackTrace(e);
                    }
                    assert p != null;
                    Matcher m = p.matcher(ip);

                    if (m.find()) {
                        isLocal = true;
                    }
                    if (isLocal) {
                        geo = "Local IP";
                        geocode = "LC";
                        lang = "en";
                    } else {
                        try {
                            String apiURL = "http://ipapi.co/" + ip + "/json";
                            URL url = new URL(apiURL);
                            HttpURLConnection con = (HttpURLConnection) url.openConnection();
                            con.setReadTimeout(5000);
                            con.setRequestMethod("POST");

                            boolean redirect = false;

                            int status = con.getResponseCode();
                            if (status != HttpURLConnection.HTTP_OK) {
                                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER)
                                    redirect = true;
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

                            if (result.has("reserved")) {
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

                    player.sendMessage("[green]Your nickname is now [white]" + player.name + ".");

                    try {
                        createNewDatabase(player.name, player.uuid, geo, geocode, lang, player.isAdmin, timesjoined, timeskicked, nowString, nowString, "blank", "blank");
                        registerresult = true;
                    } catch (Exception e) {
                        registerresult = false;
                        Call.onInfoMessage(player.con, "Player load failed!\nPlease submit this bug to the plugin developer!\n" + Arrays.toString(e.getStackTrace()));
                        player.con.kick("You have been kicked due to a plugin error.");
                    }
                }
            } catch (Exception e) {
                printStackTrace(e);
            }
        });
        db.start();
        try{db.join();}catch (Exception e){printStackTrace(e);}
        return registerresult;
    }

    static boolean login(Player player, String id, String pw) {
        Thread db = new Thread(() -> {
            try{
                PreparedStatement pstm = conn.prepareStatement("SELECT * FROM players WHERE accountid = ?");
                pstm.setString(1, id);
                ResultSet rs = pstm.executeQuery();
                if (rs.next()){
                    if(rs.getBoolean("connected")){
                        player.con.kick("You have tried to access an account that is already in use!");
                        loginresult = false;
                    } else if (BCrypt.checkpw(pw, rs.getString("accountpw"))){
                        if(rs.getBoolean("isadmin")){
                            player.isAdmin = true;
                        }
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
                printStackTrace(e);
            }
        });

        db.start();
        try{
            db.join();
        }catch (Exception e){
            printStackTrace(e);
        }
        return loginresult;
    }

    static void load(Player player, String id) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
        String nowString = now.format(dateTimeFormatter);

        // Write player connected
        if(id == null){
            writeData("UPDATE players SET connected = '1', lastdate = '"+nowString+"' WHERE uuid = '"+player.uuid+"'");
        } else {
            writeData("UPDATE players SET connected = '1', lastdate = '"+nowString+"', uuid = '"+player.uuid+"' WHERE accountid = '"+id+"'");
        }

        if (Vars.state.rules.pvp){
            player.setTeam(netServer.assignTeam(player, playerGroup.all()));
            Call.onPlayerDeath(player);
        } else {
            player.setTeam(Vars.defaultTeam);
            Call.onPlayerDeath(player);
        }

        JSONObject db = getData(player.uuid);
        if(!db.getBoolean("connected")){
            Global.loge("ERROR!");
        }

        // Show motd
        String motd;
        if(db.getString("language").equals("KR")){
            motd = Core.settings.getDataDirectory().child("mods/Essentials/motd_ko.txt").readString();
        } else {
            motd = Core.settings.getDataDirectory().child("mods/Essentials/motd.txt").readString();
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
            ColorNick color = new ColorNick();
            color.main(player);
        } else if(!realname && colornick){
            Global.logw("Color nickname must be enabled before 'realname' can be enabled.");
            writeData("UPDATE players SET colornick = '0' WHERE uuid = '"+player.uuid+"'");
        }

        //Thread checkgrief = new Thread(() -> new EssentialTimer.checkgrief(player));
        //checkgrief.start();

        if(db.getString("country").equals("invalid")) {
            // Geolocation
            String geo;
            String geocode;
            String lang;
            String ip = Vars.netServer.admins.getInfo(player.uuid).lastIP;

            try {
                String apiURL = "http://ipapi.co/" + ip + "/json";
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(5000);
                con.setRequestMethod("POST");

                boolean redirect = false;

                int status = con.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER)
                        redirect = true;
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

                if (result.has("reserved")) {
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
            writeData("UPDATE players SET country_code = '"+geocode+"', country = '"+geo+"', language = '"+lang+"' WHERE uuid = '"+player.uuid+"'");
        }
    }
}