package essentials.core;

import essentials.Global;
import essentials.Threads;
import essentials.utils.Config;
import essentials.utils.Permission;
import io.anuke.arc.Core;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.Global.*;
import static essentials.Threads.ColorNick;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.mindustry.Vars.playerGroup;

public class PlayerDB{
    private static int dbversion = 2;
    public static Connection conn;
    private static ArrayList<Thread> griefthread = new ArrayList<>();
    public Config config = new Config();
    public static ExecutorService ex = Executors.newFixedThreadPool(4, new Global.threadname("EssentialPlayer"));
    public static ArrayList<Player> pvpteam = new ArrayList<>();

    public void createNewDataFile(){
        try {
            String sql = null;
            if(config.isSqlite()){
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection(config.getDBurl());
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
                        "connserver TEXT,\n" +
                        "permission TEXT,\n" +
                        "accountid TEXT,\n" +
                        "accountpw TEXT\n" +
                        ");";
            } else {
                if(!config.getDBid().isEmpty()){
                    Class.forName("org.mariadb.jdbc.Driver");
                    Class.forName("com.mysql.jdbc.Driver");
                    conn = DriverManager.getConnection(config.getDBurl(), config.getDBid(), config.getDBpw());
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
                            "`connserver` TINYTEXT NULL DEFAULT 'none',\n" +
                            "`permission` TINYTEXT NULL DEFAULT 'default',\n" +
                            "`accountid` TEXT NULL DEFAULT NULL,\n" +
                            "`accountpw` TEXT NULL DEFAULT NULL,\n" +
                            "PRIMARY KEY (`id`)\n" +
                            ")\n" +
                            "COLLATE='utf8_general_ci'\n" +
                            "ENGINE=InnoDB\n" +
                            "AUTO_INCREMENT=1\n" +
                            ";";
                } else {
                    Global.loge(nbundle("db-address-notset"));
                }
            }
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.close();
        } catch (Exception ex){
            printStackTrace(ex);
        }
    }

	private boolean createNewDatabase(String name, String uuid, String country, String country_code, String language, Boolean isAdmin, int joincount, int kickcount, String firstdate, String lastdate, String accountid, String accountpw, Player player) {
        boolean result = false;
        try {
            String currentip = new Threads.getip().main();
            String find = "SELECT * FROM players WHERE uuid = '"+uuid+"'";
            Statement stmt  = conn.createStatement();
            ResultSet rs = stmt.executeQuery(find);
            Global.log(String.valueOf(rs.next()));

            if(!rs.next()){
                String sql;
                if(config.isSqlite()){
                    sql = "INSERT INTO 'main'.'players' ('name', 'uuid', 'country', 'country_code', 'language', 'isadmin', 'placecount', 'breakcount', 'killcount', 'deathcount', 'joincount', 'kickcount', 'level', 'exp', 'reqexp', 'reqtotalexp', 'firstdate', 'lastdate', 'lastplacename', 'lastbreakname', 'lastchat', 'playtime', 'attackclear', 'pvpwincount', 'pvplosecount', 'pvpbreakout', 'reactorcount', 'bantimeset', 'bantime', 'translate', 'crosschat', 'colornick', 'connected', 'connserver', 'permission', 'accountid', 'accountpw') VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                } else {
                    sql = "INSERT INTO players(name, uuid, country, country_code, language, isadmin, placecount, breakcount, killcount, deathcount, joincount, kickcount, level, exp, reqexp, reqtotalexp, firstdate, lastdate, lastplacename, lastbreakname, lastchat, playtime, attackclear, pvpwincount, pvplosecount, pvpbreakout, reactorcount, bantimeset, bantime, translate, crosschat, colornick, connected, connserver, permission, accountid, accountpw) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                pstmt.setString(34, currentip); // connected server ip
                pstmt.setString(35, "default"); // set permission
                pstmt.setString(36, accountid);
                pstmt.setString(37, accountpw);
                pstmt.executeUpdate();
                pstmt.close();
                player.sendMessage(nbundle("player-id", player.name));
                Global.logp(nbundle("player-db-created", name));
                result = true;
            } else if(rs.next()){
                player.sendMessage("[green][Essentials] [orange]This account already exists!\n" +
                        "[green][Essentials] [orange]이 계정은 이미 사용중입니다!");
                player.sendMessage("[green][Essentials] ID: "+rs.getString(player.name));
            }
            rs.close();
            stmt.close();
        } catch (Exception e){
            printStackTrace(e);
        }
        return result;
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
                json.put("connserver", rs.getString("connserver"));
                json.put("permission", rs.getString("permission"));
                json.put("accountid", rs.getString("accountid"));
                json.put("accountpw", rs.getString("accountpw"));
            }
            rs.close();
            stmt.close();
            if(json.toString().equals("{}")){
                Config config = new Config();
                if(config.isDebug()) {
                    Global.logpe(uuid+" Player data is empty.");
                    throw new Exception("플레이어 데이터가 없습니다!");
                }
                // todo make invalid player information
            }
        } catch (Exception e){
            if(e.getMessage().contains("Connection is closed")){
                PlayerDB db = new PlayerDB();
                db.openconnect();
            }
            printStackTrace(e);
        }
        return json;
    }

	public static void addtimeban(String name, String uuid, int bantimeset){
	    // Write ban data
        String db = Core.settings.getDataDirectory().child("mods/Essentials/data/banned.json").readString();
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

        Core.settings.getDataDirectory().child("mods/Essentials/data/banned.json").writeString(String.valueOf(object));

        // Write player data
        writeData("UPDATE players SET bantime = '"+myTime+"', bantimeset = '"+bantimeset+"' WHERE uuid = '"+uuid+"'");
    }

    public static void Upgrade() {
        Config config = new Config();
        String[] sql = new String[2];
        String v1sql;
        String v2sql;

        if(config.isSqlite()){
            v1sql = "ALTER TABLE players ADD COLUMN connserver TEXT AFTER connected;";
            v2sql = "ALTER TABLE players ADD COLUMN permission TEXT AFTER connserver;";
        } else {
            v1sql = "ALTER TABLE `players` ADD COLUMN `connserver` TINYTEXT DEFAULT NULL AFTER connected;";
            v2sql = "ALTER TABLE `players` ADD COLUMN `permission` TINYTEXT `default` NULL AFTER connserver;";
        }
        try {
            DatabaseMetaData metadata = conn.getMetaData();
            Statement stmt = conn.createStatement();
            ResultSet resultSet;
            resultSet = metadata.getColumns(null, null, "players", "connserver");
            if (!resultSet.next()) {
                stmt.execute(v1sql);
                Global.logp(nbundle("db-upgrade"));
            }
            resultSet = metadata.getColumns(null, null, "players", "permission");
            if(!resultSet.next()){
                stmt.execute(v2sql);
                Global.logp(nbundle("db-upgrade"));
            }
            stmt.close();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1060) return;
            printStackTrace(e);
        }
    }
    public void openconnect() {
        try {
            Class.forName("org.sqlite.JDBC");
            Class.forName("org.mariadb.jdbc.Driver");
            Class.forName("com.mysql.jdbc.Driver");

            String type = nbundle("db-type");

            if (config.isSqlite()) {
                conn = DriverManager.getConnection(config.getDBurl());
                Global.logp(type+"SQLite");
            } else {
                if (!config.getDBid().isEmpty()) {
                    conn = DriverManager.getConnection(config.getDBurl(), config.getDBid(), config.getDBpw());
                    Global.logp(type+"MariaDB/MySQL");
                } else {
                    conn = DriverManager.getConnection(config.getDBurl());
                    Global.logp(type+"Invalid");
                }
            }
        } catch (ClassNotFoundException e) {
            printStackTrace(e);
            Global.loge("Class not found!");
        } catch (SQLException e){
            printStackTrace(e);
            Global.loge("SQL ERROR!");
        }
    }

    public static void closeconnect(){
        try{
            conn.close();
        } catch (Exception e) {
            printStackTrace(e);
        }
    }

	public static void writeData(String sql){
        Thread t = new Thread(() -> {
            try {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.executeUpdate();
                pstmt.close();
            } catch (Exception e) {
                if(e.getMessage().contains("Connection is closed")){
                    PlayerDB db = new PlayerDB();
                    db.openconnect();
                }
                Global.loge(sql);
                printStackTrace(e);
            }
        });
        ex.submit(t);
	}

	public static boolean checkpw(Player player, String pw){
        boolean result = true;
        // 영문(소문자), 숫자, 7~20자리
        String pwPattern = "^(?=.*\\d)(?=.*[a-z]).{7,20}$";
        Matcher matcher = Pattern.compile(pwPattern).matcher(pw);

        // 같은 문자 4개이상 사용 불가
        pwPattern = "(.)\\1\\1\\1";
        Matcher matcher2 = Pattern.compile(pwPattern).matcher(pw);

        /*if (!pw.equals(pw2)) {
            // 비밀번호가 비밀번호 재확인 문자열과 똑같지 않을경우
            player.sendMessage("[green][Essentials] [sky]The password isn't the same.\n" +
                    "[green][Essentials] [sky]비밀번호가 똑같지 않습니다.");
            result = false;
        } else */
        if (!matcher.matches()) {
            // 정규식에 맞지 않을경우
            player.sendMessage("[green][Essentials] [sky]The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!\n" +
                    "[green][Essentials] [sky]비밀번호는 7~20자 내외로 설정해야 하며, 영문과 숫자를 포함해야 합니다!");
            Global.log(nbundle("password-match-regex", player.name));
            result = false;
        } else if (matcher2.find()) {
            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            player.sendMessage("[green][Essentials] [sky]Passwords should not be similar to nicknames!\n" +
                    "[green][Essentials] [sky]비밀번호는 닉네임과 비슷하면 안됩니다!");
            Global.log(nbundle("password-match-name", player.name));
            result = false;
        } else /*if (pw.contains(id)) {
            // 비밀번호와 ID가 완전히 같은경우
            player.sendMessage("[green][Essentials] [sky]Password shouldn't be the same as your nickname.\n" +
                    "[green][Essentials] [sky]비밀번호는 ID는 똑같이 설정할 수 없습니다!");
            result = false;
        } else*/ if (pw.contains(" ")) {
            // 비밀번호에 공백이 있을경우
            player.sendMessage("[green][Essentials] [sky]Password must not contain spaces!\n" +
                    "[green][Essentials] [sky]비밀번호에는 공백이 있으면 안됩니다!");
            Global.log(nbundle("password-match-blank", player.name));
            result = false;
        } else if (pw.matches("<(.*?)>")) {
            // 비밀번호 형식이 "<비밀번호>" 일경우
            player.sendMessage("[green][Essentials] [green]<[sky]password[green]>[sky] format isn't allowed!\n" +
                    "[green][Essentials] [sky]Use /register password\n" +
                    "[green][Essentials] [green]<[sky]비밀번호[green]>[sky] 형식은 허용되지 않습니다!\n" +
                    "[green][Essentials] [sky]/register password 형식으로 사용하세요.");
            Global.log(nbundle("password-match-invalid", player.name));
            result = false;
        }
        return result;
    }
	// 로그인 기능 사용시 계정 등록
	public boolean register(Player player, String pw) {
        boolean result = true;

        // 비밀번호 보안 확인
        if(!checkpw(player,pw)) result = false;
        // 보안검사 끝

        if(result){
            try {
                Class.forName("org.mindrot.jbcrypt.BCrypt");
                String hashed = BCrypt.hashpw(pw, BCrypt.gensalt(11));

                PreparedStatement pstm1 = conn.prepareStatement("SELECT * FROM players WHERE name = '" + player.name + "'");
                ResultSet rs1 = pstm1.executeQuery();
                if (rs1.next()) {
                    if (rs1.getString("name").equals(player.name)) {
                        player.sendMessage("[green][Essentials] [orange]This username is already in use!\n" +
                                "[green][Essentials] [orange]이 사용자 이름은 이미 사용중입니다!");
                        Global.log(nbundle("password-already-username", player.name));
                        result = false;
                    }
                } else {
                    // email source here
                    PreparedStatement pstm2 = conn.prepareStatement("SELECT * FROM players WHERE uuid = '" + player.uuid + "'");
                    ResultSet rs2 = pstm2.executeQuery();
                    String isuuid = null;
                    while (rs2.next()) {
                        isuuid = rs2.getString("uuid");
                    }
                    if (isuuid == null || isuuid.length() == 0) {
                        LocalDateTime now = LocalDateTime.now();
                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                        String nowString = now.format(dateTimeFormatter);
                        JSONObject list = geolocation(player);

                        String find = "SELECT * FROM players WHERE uuid = '"+player.uuid+"'";
                        Statement stmt  = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(find);
                        if(!rs.next()){
                            result = createNewDatabase(player.name, player.uuid, list.getString("geo"), list.getString("geocode"), list.getString("lang"), player.isAdmin, Vars.netServer.admins.getInfo(player.uuid).timesJoined, Vars.netServer.admins.getInfo(player.uuid).timesKicked, nowString, nowString, player.name, hashed, player);
                        } else if(rs.next()){
                            player.sendMessage("[green][Essentials] [orange]You already have an account!\n" +
                                    "[green][Essentials] [orange]당신은 이미 계정을 가지고 있습니다!");
                            Global.log(nbundle("password-already-account", player.name));
                            result = false;
                        }
                        player.sendMessage(bundle(player, "player-name-changed", player.name));
                    } else if (isuuid.length() > 1 || isuuid.equals(player.uuid)) {
                        player.sendMessage("[green][Essentials] [orange]This account already exists!\n" +
                                "[green][Essentials] [orange]이 계정은 이미 사용중입니다!");
                        Global.log(nbundle("password-already-using", player.name));
                        result = false;
                    } else {
                        result = false;
                    }
                }
            } catch (Exception e) {
                printStackTrace(e);
            }
        }
        return result;
    }

    // 비 로그인 기능 사용시 계정등록
    public boolean register(Player player) {
        if (getData(player.uuid).toString().equals("{}")) {
            JSONObject list = geolocation(player);
            player.sendMessage(bundle(player, "player-name-changed", player.name));

            return createNewDatabase(player.name, player.uuid, list.getString("geo"), list.getString("geocode"), list.getString("lang"), player.isAdmin, Vars.netServer.admins.getInfo(player.uuid).timesJoined, Vars.netServer.admins.getInfo(player.uuid).timesKicked, getnTime(), getnTime(), player.name, "blank", player);
        } else {
            return true;
        }
    }

    public static boolean login(Player player, String id, String pw) {
        boolean result = false;
        try {
            PreparedStatement pstm = conn.prepareStatement("SELECT * FROM players WHERE accountid = ?");
            pstm.setString(1, id);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                if (rs.getBoolean("connected")) {
                    player.con.kick(nbundle(player, "tried-connected-account"));
                    result = false;
                } else if (BCrypt.checkpw(pw, rs.getString("accountpw"))) {
                    if (rs.getBoolean("isadmin")) {
                        player.isAdmin = true;
                    }
                    pstm = conn.prepareStatement("UPDATE players SET uuid = ?, connected = ? WHERE accountid = ? and accountpw = ?");
                    pstm.setString(1, player.uuid);
                    pstm.setBoolean(2, true);
                    pstm.setString(3, id);
                    pstm.setString(4, pw);
                    pstm.executeUpdate();
                    result = true;
                } else {
                    player.sendMessage("[green][EssentialPlayer] [scarlet]Wrong password!/비밀번호가 틀렸습니다!");
                }
            } else {
                player.sendMessage("[green][EssentialPlayer] [scarlet]Account not found!/계정을 찾을 수 없습니다!");
            }
        } catch (Exception e) {
            printStackTrace(e);
        }
        return result;
    }

    public void load(Player player, String id) {
        Thread t = new Thread(() -> {
            JSONObject db = getData(player.uuid);
            // 만약에 새 기기로 기존 계정에 로그인 했을때, 계정에 있던 DB를 가져와서 검사함
            if(db.toString().equals("{}")){
                String uuid = "";
                try{
                    String sql = "SELECT uuid FROM players WHERE accountid='"+id+"'";
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);
                    while(rs.next()){
                        uuid = rs.getString("uuid");
                    }
                    rs.close();
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                db = getData(uuid);
            }

            if(db.getBoolean("connected") && config.isValidconnect()){
                for(int a=0;a<playerGroup.size();a++){
                    String target = playerGroup.all().get(a).uuid;
                    if(target.equals(player.uuid)){
                        player.con.kick(nbundle(player, "tried-connected-account"));
                        return;
                    }
                }
            }

            String currentip = new Threads.getip().main();

            // 플레이어가 연결한 서버 데이터 기록
            if (id == null) {
                writeData("UPDATE players SET connected = '1', lastdate = '" + getnTime() + "', connserver = '" + currentip + "' WHERE uuid = '" + player.uuid + "'");
            } else {
                writeData("UPDATE players SET connected = '1', lastdate = '" + getnTime() + "', connserver = '" + currentip + "', uuid = '" + player.uuid + "' WHERE accountid = '" + id + "'");
            }

            // 플레이어 팀 설정
            if (Vars.state.rules.pvp){
                boolean match = false;
                for(int a=0;a<pvpteam.size();a++){
                    Player target = pvpteam.get(a);
                    Team team = pvpteam.get(a).getTeam();
                    if(player.uuid.equals(target.uuid)){
                        if(Vars.state.teams.get(team).cores.isEmpty()){
                            break;
                        } else {
                            player.setTeam(team);
                            match = true;
                        }
                    }
                }
                if(!match){
                    player.setTeam(netServer.assignTeam(player, playerGroup.all()));
                    pvpteam.add(player);
                }
                Call.onPlayerDeath(player);
            } else {
                player.setTeam(Vars.defaultTeam);
                Call.onPlayerDeath(player);
            }

            // 입장 메세지 표시
            String motd = getmotd(player);
            int count = motd.split("\r\n|\r|\n").length;
            if(count > 10){
                Call.onInfoMessage(player.con, motd);
            } else {
                player.sendMessage(motd);
            }

            // 고정닉 기능이 켜져있을 경우, 플레이어 닉네임 설정
            if(config.isRealname()){
                player.name = db.getString("name");
            }

            // 서버 입장시 경험치 획득
            new Thread(() -> Exp.joinexp(player.uuid));

            // 컬러닉 기능 설정
            boolean colornick = db.getBoolean("colornick");
            if(config.isRealname() && colornick){
                // 컬러닉 스레드 시작
                new Thread(new ColorNick(player)).start();
            } else if(!config.isRealname() && colornick){
                Global.logpw(nbundle("colornick-require"));
                writeData("UPDATE players SET colornick = '0' WHERE uuid = '"+player.uuid+"'");
            }

            // 플레이어별 테러 감지 시작
            if(config.isAntigrief()) {
                new Threads.checkgrief(player);
            }

            // 플레이어가 관리자 그룹에 있을경우 관리자모드 설정
            Permission.setAdmin(player);

            // 플레이어 지역이 invalid 일경우 다시 정보 가져오기
            if(db.getString("country").equals("invalid")) {
                JSONObject list = geolocation(player);
                writeData("UPDATE players SET country_code = '"+list.getString("geocode")+"', country = '"+list.getString("geo")+"', language = '"+list.getString("lang")+"' WHERE uuid = '"+player.uuid+"'");
            }
        });
        t.start();
    }
}