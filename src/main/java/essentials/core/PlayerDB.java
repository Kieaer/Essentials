package essentials.core;

import com.grack.nanojson.JsonObject;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.Global.*;
import static essentials.Threads.ColorNick;
import static essentials.utils.Config.PluginData;
import static essentials.utils.Config.executorService;
import static essentials.utils.Permission.permission;
import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;

public class PlayerDB{
    public static Connection conn;
    private static ArrayList<Thread> griefthread = new ArrayList<>();
    public static ArrayList<Player> pvpteam = new ArrayList<>();
    public static ArrayList<PlayerData> Players = new ArrayList<>(); // Players data

    public void run(){
        openconnect();
        createNewDataFile();
        Upgrade();
    }

    public void createNewDataFile(){
        try {
            String sql = null;
            if(config.isSqlite()){
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
                        "banned TEXT,\n" +
                        "translate TEXT,\n" +
                        "crosschat TEXT,\n" +
                        "colornick TEXT,\n" +
                        "connected TEXT,\n" +
                        "connserver TEXT,\n" +
                        "permission TEXT,\n" +
                        "udid TEXT,\n" +
                        "accountid TEXT,\n" +
                        "accountpw TEXT\n" +
                        ");";
            } else {
                if(!config.getDBid().isEmpty()){
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
                            "`banned` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`translate` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`crosschat` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`colornick` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`connected` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`connserver` TINYTEXT NULL DEFAULT 'none',\n" +
                            "`permission` TINYTEXT NULL DEFAULT 'default',\n" +
                            "`udid` TEXT NULL DEFAULT NULL,\n" +
                            "`accountid` TEXT NULL DEFAULT NULL,\n" +
                            "`accountpw` TEXT NULL DEFAULT NULL,\n" +
                            "PRIMARY KEY (`id`)\n" +
                            ")\n" +
                            "COLLATE='utf8_general_ci'\n" +
                            "ENGINE=InnoDB\n" +
                            "AUTO_INCREMENT=1\n" +
                            ";";
                } else {
                    log("playererror","db-address-notset");
                }
            }
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            stmt.close();
        } catch (Exception ex){
            printError(ex);
        }
    }
	public static boolean createNewDatabase(String name, String uuid, String country, String country_code, String language, Boolean isAdmin, int joincount, int kickcount, String firstdate, String lastdate, boolean connected, Long udid, String accountid, String accountpw, Player player) {
        boolean result = false;
        try {
            if(uuid.equals("InactiveAAA=") || !isduplicate(uuid)){
                String sql;
                if(config.isSqlite()){
                    sql = "INSERT INTO 'main'.'players' ('name', 'uuid', 'country', 'country_code', 'language', 'isadmin', 'placecount', 'breakcount', 'killcount', 'deathcount', 'joincount', 'kickcount', 'level', 'exp', 'reqexp', 'reqtotalexp', 'firstdate', 'lastdate', 'lastplacename', 'lastbreakname', 'lastchat', 'playtime', 'attackclear', 'pvpwincount', 'pvplosecount', 'pvpbreakout', 'reactorcount', 'bantimeset', 'bantime', 'banned', 'translate', 'crosschat', 'colornick', 'connected', 'connserver', 'permission', 'udid', 'accountid', 'accountpw') VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                } else {
                    sql = "INSERT INTO players(name, uuid, country, country_code, language, isadmin, placecount, breakcount, killcount, deathcount, joincount, kickcount, level, exp, reqexp, reqtotalexp, firstdate, lastdate, lastplacename, lastbreakname, lastchat, playtime, attackclear, pvpwincount, pvplosecount, pvpbreakout, reactorcount, bantimeset, bantime, banned, translate, crosschat, colornick, connected, connserver, permission, udid, accountid, accountpw) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                pstmt.setBoolean(30, false);
                pstmt.setBoolean(31, false); // translate
                pstmt.setBoolean(32, false); // crosschat
                pstmt.setBoolean(33, false); // colornick
                pstmt.setBoolean(34, connected); // connected
                pstmt.setString(35, getip()); // connected server ip
                pstmt.setString(36, "default"); // set permission
                pstmt.setLong(37, udid); // UDID
                pstmt.setString(38, accountid);
                pstmt.setString(39, accountpw);
                pstmt.execute();
                pstmt.close();
                if(player != null) player.sendMessage(bundle("player-id", player.name));
                log("player","player-db-created", name);
                result = true;
            } else {
                if(player != null){
                    player.sendMessage("[green][Essentials] [orange]This account already exists!\n" +
                            "[green][Essentials] [orange]이 계정은 이미 사용중입니다!");
                }
            }
        } catch (Exception e){
            printError(e);
        }
        return result;
	}
	// 메모리 사용 (빠름)
	public static void getInfo(String uuid){
        try {
            String sql = "SELECT * FROM players WHERE uuid='"+uuid+"'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if(rs.next()){
                Players.add(new PlayerData(
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
            } else {
                Players.add(new PlayerData(true,false));
            }
            rs.close();
            stmt.close();
        } catch (Exception e){
            if(e.getMessage().contains("Connection is closed")){
                openconnect();
            }
            printError(e);
        }
    }
    // DB 사용 (느림)
    public static JsonObject getRaw(String uuid){
        JsonObject data = new JsonObject();
        try {
            String sql = "SELECT * FROM players WHERE uuid='"+uuid+"'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                data.put("id", rs.getInt("id"));
                data.put("name", rs.getString("name"));
                data.put("uuid", rs.getString("uuid"));
                data.put("country", rs.getString("country"));
                data.put("country_code", rs.getString("country_code"));
                data.put("language", rs.getString("language"));
                data.put("isadmin", rs.getBoolean("isadmin"));
                data.put("placecount", rs.getInt("placecount"));
                data.put("breakcount", rs.getInt("breakcount"));
                data.put("killcount", rs.getInt("killcount"));
                data.put("deathcount", rs.getInt("deathcount"));
                data.put("joincount", rs.getInt("joincount"));
                data.put("kickcount", rs.getInt("kickcount"));
                data.put("level", rs.getInt("level"));
                data.put("exp", rs.getInt("exp"));
                data.put("reqexp", rs.getInt("reqexp"));
                data.put("reqtotalexp", rs.getString("reqtotalexp"));
                data.put("firstdate", rs.getString("firstdate"));
                data.put("lastdate", rs.getString("lastdate"));
                data.put("lastplacename", rs.getString("lastplacename"));
                data.put("lastbreakname", rs.getString("lastbreakname"));
                data.put("lastchat", rs.getString("lastchat"));
                data.put("playtime", rs.getString("playtime"));
                data.put("attackclear", rs.getInt("attackclear"));
                data.put("pvpwincount", rs.getInt("pvpwincount"));
                data.put("pvplosecount", rs.getInt("pvplosecount"));
                data.put("pvpbreakout", rs.getInt("pvpbreakout"));
                data.put("reactorcount", rs.getInt("reactorcount"));
                data.put("bantimeset", rs.getString("bantimeset"));
                data.put("bantime", rs.getString("bantime"));
                data.put("banned", rs.getBoolean("banned"));
                data.put("translate", rs.getBoolean("translate"));
                data.put("crosschat", rs.getBoolean("crosschat"));
                data.put("colornick", rs.getBoolean("colornick"));
                data.put("connected", rs.getBoolean("connected"));
                data.put("connserver", rs.getString("connserver"));
                data.put("permission", rs.getString("permission"));
                data.put("udid",rs.getString("udid"));
                data.put("accountid", rs.getString("accountid"));
                data.put("accountpw", rs.getString("accountpw"));
            }
            rs.close();
            stmt.close();
        } catch (Exception e){
            if(e.getMessage().contains("Connection is closed")){
                openconnect();
            }
            printError(e);
        }
        return data;
    }
	public static void addtimeban(String name, String uuid, int bantimeset) {
        // Write ban data
        try {
            SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd a hh:mm.ss", Locale.ENGLISH);
            Date d1 = format.parse(getTime());
            Calendar cal = Calendar.getInstance();
            cal.setTime(d1);
            cal.add(Calendar.HOUR, bantimeset);
            String newTime = format.format(cal.getTime());

            JsonObject data1 = new JsonObject();
            data1.put("uuid", uuid);
            data1.put("date", newTime);
            data1.put("name", name);

            PluginData.getArray("banned").add(data1);

            // Write player data
            writeData("UPDATE players SET bantime = ?, bantimeset = ? WHERE uuid = ?", getTime(), bantimeset, uuid);
        } catch (Exception e) {
            printError(e);
        }
    }
    public static boolean accountban(boolean ban, String uuid){
        if(ban){
            PlayerData(uuid).banned = true;
            return true;
        } else {
            PlayerData(uuid).banned = false;
            return false;
        }
    }
    public void Upgrade() {
        String v1sql;
        String v1update;
        String v2update;
        String v2sql;
        String v3sql;
        String v3update;
        String v4sql;
        String v4update;

        if(config.isSqlite()){
            v1sql = "ALTER TABLE players ADD COLUMN connserver TEXT AFTER connected;";
            v1update = "UPDATE players SET connected = 0";
            v2sql = "ALTER TABLE players ADD COLUMN permission TEXT AFTER connserver;";
            v2update = "UPDATE players SET permission = default";
            v3sql = "ALTER TABLE players ADD COLUMN banned TEXT AFTER bantime;";
            v4sql = "ALTER TABLE players ADD COLUMN udid TEXT AFTER permission;";
            v4update = "UPDATE players SET udid = none";
        } else {
            v1sql = "ALTER TABLE `players` ADD COLUMN `connserver` TINYTEXT DEFAULT NULL AFTER connected;";
            v1update = "UPDATE players SET connected = 0";
            v2sql = "ALTER TABLE `players` ADD COLUMN `permission` TINYTEXT `default` NULL AFTER connserver;";
            v2update = "UPDATE players SET permission = 'default'";
            v3sql = "ALTER TABLE `players` ADD COLUMN `banned` TINYINT DEFAULT NULL AFTER bantime;";
            v4sql = "ALTER TABLE `players` ADD COLUMN `udid` TEXT DEFAULT NULL AFTER permission;";
            v4update = "UPDATE players SET udid = 'none'";
        }
        v3update = "UPDATE players SET banned = 0";

        try {
            DatabaseMetaData metadata = conn.getMetaData();
            Statement stmt = conn.createStatement();
            ResultSet resultSet;
            resultSet = metadata.getColumns(null, null, "players", "connserver");
            if (!resultSet.next()) {
                stmt.execute(v1sql);
                stmt.execute(v1update);
                log("player","db-upgrade");
            }
            resultSet = metadata.getColumns(null, null, "players", "permission");
            if(!resultSet.next()){
                stmt.execute(v2sql);
                stmt.execute(v2update);
                log("player","db-upgrade");
            }
            resultSet = metadata.getColumns(null, null, "players", "banned");
            if(!resultSet.next()){
                stmt.execute(v3sql);
                stmt.execute(v3update);
                log("player","db-upgrade");
            }
            resultSet = metadata.getColumns(null, null, "players", "udid");
            if(!resultSet.next()){
                stmt.execute(v4sql);
                stmt.execute(v4update);
                log("player","db-upgrade");
            }
            resultSet.close();
            stmt.close();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1060) return;
            printError(e);
        }
    }
    public static void openconnect() {
        try {
            if (config.isSqlite()) {
                conn = DriverManager.getConnection(config.getDBurl());
                log("player","db-type","SQLite");
            } else {
                if (!config.getDBid().isEmpty()) {
                    conn = DriverManager.getConnection(config.getDBurl(), config.getDBid(), config.getDBpw());
                    log("player","db-type","MariaDB/MySQL/PostgreSQL");
                } else {
                    conn = DriverManager.getConnection(config.getDBurl());
                    log("player","db-type","Invalid");
                }
            }
        } catch (SQLException e){
            printError(e);
            nlog("warn","SQL ERROR!");
        }
    }
    public static boolean closeconnect(){
        try {
            conn.close();
            return true;
        } catch (Exception e) {
            printError(e);
            return false;
        }
    }
	public static void writeData(String sql, Object... data){
        Thread t = new Thread(() -> {
            try {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                for (int a=1;a<=data.length;a++) {
                    int b=a-1;
                    if (data[b] instanceof Date) {
                        pstmt.setTimestamp(a, new Timestamp(((Date) data[b]).getTime()));
                    } else if (data[b] instanceof Integer) {
                        pstmt.setInt(a, Integer.parseInt(String.valueOf(data[b])));
                    } else if (data[b] instanceof Long) {
                        pstmt.setLong(a, Long.parseLong(String.valueOf(data[b])));
                    } else if (data[b] instanceof Double) {
                        pstmt.setDouble(a, Double.parseDouble(String.valueOf(data[b])));
                    } else if (data[b] instanceof Float) {
                        pstmt.setFloat(a, Float.parseFloat(String.valueOf(data[b])));
                    } else if (data[b] instanceof Boolean) {
                        pstmt.setBoolean(a, Boolean.parseBoolean(String.valueOf(data[b])));
                    }else {
                        pstmt.setString(a, String.valueOf(data[b]));
                    }
                }
                pstmt.execute();
                pstmt.close();
            } catch (Exception e) {
                if(e.getMessage().contains("Connection is closed")){
                    openconnect();
                } else {
                    printError(e);
                }
            }
        });
        executorService.submit(t);
	}
	public static boolean checkpw(Player player, String id, String pw){
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
            return false;
        } else */
        if (!matcher.matches()) {
            // 정규식에 맞지 않을경우
            player.sendMessage("[green][Essentials] [sky]The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!\n" +
                    "[green][Essentials] [sky]비밀번호는 7~20자 내외로 설정해야 하며, 영문과 숫자를 포함해야 합니다!");
            log("player","password-match-regex", player.name);
            return false;
        } else if (matcher2.find()) {
            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            player.sendMessage("[green][Essentials] [sky]Passwords should not be similar to nicknames!\n" +
                    "[green][Essentials] [sky]비밀번호는 닉네임과 비슷하면 안됩니다!");
            log("player","password-match-name", player.name);
            return false;
        } else if (pw.contains(id)) {
            // 비밀번호와 ID가 완전히 같은경우
            player.sendMessage("[green][Essentials] [sky]Password shouldn't be the same as your nickname.\n" +
                    "[green][Essentials] [sky]비밀번호는 ID와 비슷하게 설정할 수 없습니다!");
            return false;
        } else if (pw.contains(" ")) {
            // 비밀번호에 공백이 있을경우
            player.sendMessage("[green][Essentials] [sky]Password must not contain spaces!\n" +
                    "[green][Essentials] [sky]비밀번호에는 공백이 있으면 안됩니다!");
            log("player","password-match-blank", player.name);
            return false;
        } else if (pw.matches("<(.*?)>")) {
            // 비밀번호 형식이 "<비밀번호>" 일경우
            player.sendMessage("[green][Essentials] [green]<[sky]password[green]>[sky] format isn't allowed!\n" +
                    "[green][Essentials] [sky]Use /register password\n" +
                    "[green][Essentials] [green]<[sky]비밀번호[green]>[sky] 형식은 허용되지 않습니다!\n" +
                    "[green][Essentials] [sky]/register password 형식으로 사용하세요.");
            log("player","password-match-invalid", player.name);
            return false;
        }
        return true;
    }
	// 로그인 기능 사용시 계정 등록
	public boolean register(Player player, String id, String pw) {
        // 비밀번호 보안 확인
        if(checkpw(player, id, pw)) {
            try {
                Class.forName("org.mindrot.jbcrypt.BCrypt");
                String hashed = BCrypt.hashpw(pw, BCrypt.gensalt(11));

                if (isduplicateid(id)) {
                    player.sendMessage("[green][Essentials] [orange]This account id is already in use!\n" +
                            "[green][Essentials] [orange]이 계정명은 이미 사용중입니다!");
                    log("player", "password-already-accountid", id);
                    return false;
                } else {
                    // 한국어, 중국어, 일어, 러시아어, 영어, 숫자만 허용
                    String nickname = player.name;
                    //String nickname = player.name.replaceAll("[^\uac00-\ud7a3\u2E80-\u2eff\u3400-\u4dbf\u4e00-\u9fbf\uf9000\ufaff\u20000-\u2a6df\u3040-\u309f\u30a0-\u30ff\u31f0-\u31ff\u0400-\u052f0-9a-zA-Z\\s]", "");

                    nlog("debug", player.name + " Account not found");
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss", Locale.ENGLISH);
                    String nowString = now.format(dateTimeFormatter);
                    HashMap<String, String> list = geolocation(player);

                    if (!isduplicate(player)) {
                        createNewDatabase(nickname, player.uuid, list.get("country"), list.get("country_code"), list.get("languages"), player.isAdmin, netServer.admins.getInfo(player.uuid).timesJoined, netServer.admins.getInfo(player.uuid).timesKicked, nowString, nowString, true, 0L, "", hashed, player);
                    } else {
                        player.sendMessage("[green][Essentials] [orange]You already have an account!\n" +
                                "[green][Essentials] [orange]당신은 이미 계정을 가지고 있습니다!");
                        log("player", "password-already-account", player.name);
                        return false;
                    }
                    player.sendMessage(bundle(player, "player-name-changed", player.name));
                }
            } catch (Exception e) {
                printError(e);
            }
            return true;
        } else {
            return false;
        }
    }
    // 비 로그인 기능 사용시 계정등록
    public boolean register(Player player) {
        if (isLogin(player)) {
            HashMap<String, String> list = geolocation(player);
            player.sendMessage(bundle(player, "player-name-changed", player.name));
            return createNewDatabase(player.name, player.uuid, list.get("country"), list.get("country_code"), list.get("languages"), player.isAdmin, netServer.admins.getInfo(player.uuid).timesJoined, netServer.admins.getInfo(player.uuid).timesKicked, getTime(), getTime(), true, 0L, "", "blank", player);
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
                    writeData("UPDATE players SET uuid = ?, connected = ? WHERE accountid = ? and accountpw = ?", player.uuid, true, id, pw);
                    result = true;
                } else {
                    player.sendMessage("[green][EssentialPlayer] [scarlet]Wrong password!/비밀번호가 틀렸습니다!");
                }
            } else {
                player.sendMessage("[green][EssentialPlayer] [scarlet]Account not found!/계정을 찾을 수 없습니다!");
            }
            rs.close();
            pstm.close();
        } catch (Exception e) {
            printError(e);
        }
        return result;
    }
    public void load(Player target, String id) {
        Thread thread = new Thread(() -> {
            getInfo(target.uuid);
            PlayerData player = PlayerData(target.uuid);
            nlog("debug", target.name + " Player load start");

            // 만약에 새 기기로 기존 계정에 로그인 했을때, 계정에 있던 DB를 가져와서 검사함
            if (!player.error) {
                nlog("debug", player.name + " Player logged!");
                String uuid = "";
                try {
                    String sql = "SELECT uuid FROM players WHERE accountid = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, id);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        uuid = rs.getString("uuid");
                    }
                    rs.close();
                    stmt.close();
                } catch (SQLException e) {
                    printError(e);
                }
                PlayerDataRemove(target.uuid);
                getInfo(uuid);
                player = PlayerData(uuid);

                // 새 기기로 UUID 적용
                if (!player.isLogin) {
                    try {
                        PreparedStatement stmt = conn.prepareStatement("UPDATE players SET uuid = ? WHERE accountid = ?");
                        stmt.setString(1, player.uuid);
                        stmt.setString(2, id);
                        stmt.execute();

                        PlayerDataRemove(target.uuid);
                        getInfo(uuid);
                        player = PlayerData(uuid);
                    } catch (SQLException e) {
                        printError(e);
                    }
                }
            }

            // 이 계정이 밴을 당했을 때 강퇴처리
            if (player.banned) {
                netServer.admins.banPlayerID(player.uuid);
                target.con.kick("This account can't use.");
                return;
            }

            // 중복 로그인 검사
            if (config.isValidconnect()) {
                nlog("debug", player.name + " Player validate start");
                if (player.connected) {
                    for (int a = 0; a < playerGroup.size(); a++) {
                        String p = playerGroup.all().get(a).uuid;
                        if (p.equals(player.uuid)) {
                            target.con.kick(nbundle(target, "tried-connected-account"));
                            return;
                        }
                    }
                }
            }

            String currentip = getip();
            nlog("debug", player.name + " Player ip collected");

            player.connected = true;
            player.lastdate = getTime();
            player.connserver = currentip;

            if (id != null) {
                player.uuid = target.uuid;
                PlayerDataSet(player.uuid, player);
            }
            nlog("debug", player.name + " Player data write");

            // 플레이어 팀 설정
            if (Vars.state.rules.pvp) {
                boolean match = false;
                for (Player t : pvpteam) {
                    Team team = t.getTeam();
                    if (player.uuid.equals(t.uuid)) {
                        if (Vars.state.teams.get(team).cores.isEmpty()) {
                            break;
                        } else {
                            target.setTeam(team);
                            match = true;
                        }
                    }
                }
                if (!match) {
                    target.setTeam(netServer.assignTeam(target, playerGroup.all()));
                    pvpteam.add(target);
                }
            } else {
                target.setTeam(Team.sharded);
            }

            nlog("debug", player.name + " Player Team set");
            Call.onPlayerDeath(target);
            nlog("debug", player.name + " Player Respawned");

            // 입장 메세지 표시
            String motd = getmotd(target);
            int count = motd.split("\r\n|\r|\n").length;
            if (count > 10) {
                Call.onInfoMessage(target.con, motd);
            } else {
                target.sendMessage(motd);
            }
            nlog("debug", player.name + " Player show motd");

            // 고정닉 기능이 켜져있을 경우, 플레이어 닉네임 설정
            if (config.isRealname() || config.getPasswordmethod().equals("discord")) {
                target.name = player.name;
                nlog("debug", player.name + " Player Set nickname. " + player.name + " to " + target.name + ".");
            }

            // 서버 입장시 경험치 획득
            Exp.joinexp(player.uuid);
            nlog("debug", player.name + " Player increase exp");

            // 컬러닉 기능 설정
            if (config.isRealname() && player.colornick) {
                // 컬러닉 스레드 시작
                new Thread(new ColorNick(target)).start();
            } else if (!config.isRealname() && player.colornick) {
                log("player", "colornick-require");
                player.colornick = false;
                PlayerDataSet(player.uuid, player);
            }
            nlog("debug", player.name + " Player pass colornick");

            // 플레이어별 테러 감지 시작
            if (config.isAntigrief() && !player.isAdmin) {
                //new Threads.checkgrief(target).start();
                nlog("debug", player.name + " Player anti-grief start");
            }

            // 플레이어가 관리자 그룹에 있을경우 관리자모드 설정
            if (permission.getObject(player.permission).has("admin")) {
                if (permission.getObject(player.permission).getBoolean("admin")) {
                    target.isAdmin = true;

                    player.isAdmin = true;
                    PlayerDataSet(player.uuid, player);
                }
            }
            nlog("debug", player.name + " Player permission set");

            // 플레이어 위치 정보가 없을경우, 위치 정보 가져오기
            if (player.country.equals("invalid")) {
                HashMap<String, String> list = geolocation(target);
                player.country = list.get("country");
                player.country_code = list.get("country_code");
                player.language = list.get("languages");
                PlayerDataSet(player.uuid, player);
            }
            nlog("debug", player.name + " Player country data collected");

            // 플레이어 접속 횟수 카운트
            player.joincount = player.joincount++;
            PlayerDataSet(player.uuid, player);
            nlog("debug", player.name + " Player data full loaded!");
        });
        executorService.submit(thread);
    }

    public static class PlayerData {
        public int id;
        public String name;
        public String uuid;
        public String country;
        public String country_code;
        public String language;
        public boolean isAdmin;
        public int placecount;
        public int breakcount;
        public int killcount;
        public int deathcount;
        public int joincount;
        public int kickcount;
        public int level;
        public int exp;
        public int reqexp;
        public String reqtotalexp;
        public String firstdate;
        public String lastdate;
        public String lastplacename;
        public String lastbreakname;
        public String lastchat;
        public String playtime;
        public int attackclear;
        public int pvpwincount;
        public int pvplosecount;
        public int pvpbreakout;
        public int reactorcount;
        public int bantimeset;
        public String bantime;
        public boolean banned;
        public boolean translate;
        public boolean crosschat;
        public boolean colornick;
        public boolean connected;
        public String connserver;
        public String permission;
        public Long udid;
        public String accountid;
        public String accountpw;

        public boolean error;
        public boolean isLogin;

        PlayerData(boolean error, boolean isLogin){
            this.error = error;
            this.isLogin = isLogin;
        }

        public PlayerData(int id, String name, String uuid, String country, String country_code, String language, boolean isAdmin, int placecount, int breakcount, int killcount, int deathcount, int joincount, int kickcount, int level, int exp, int reqexp, String reqtotalexp, String firstdate, String lastdate, String lastplacename, String lastbreakname, String lastchat, String playtime, int attackclear, int pvpwincount, int pvplosecount, int pvpbreakout, int reactorcount, int bantimeset, String bantime, boolean banned, boolean translate, boolean crosschat, boolean colornick, boolean connected, String connserver, String permission, Long udid, String accountid, String accountpw){
            this.id = id;
            this.name = name;
            this.uuid = uuid;
            this.country = country;
            this.country_code = country_code;
            this.language = language;
            this.isAdmin = isAdmin;
            this.placecount = placecount;
            this.breakcount = breakcount;
            this.killcount = killcount;
            this.deathcount = deathcount;
            this.joincount = joincount;
            this.kickcount = kickcount;
            this.level = level;
            this.exp = exp;
            this.reqexp = reqexp;
            this.reqtotalexp = reqtotalexp;
            this.firstdate = firstdate;
            this.lastdate = lastdate;
            this.lastplacename = lastplacename;
            this.lastbreakname = lastbreakname;
            this.lastchat = lastchat;
            this.playtime = playtime;
            this.attackclear = attackclear;
            this.pvpwincount = pvpwincount;
            this.pvplosecount = pvplosecount;
            this.pvpbreakout = pvpbreakout;
            this.reactorcount = reactorcount;
            this.bantimeset = bantimeset;
            this.bantime = bantime;
            this.banned = banned;
            this.translate = translate;
            this.crosschat = crosschat;
            this.colornick = colornick;
            this.connected = connected;
            this.connserver = connserver;
            this.permission = permission;
            this.udid = udid;
            this.accountid = accountid;
            this.accountpw = accountpw;
        }
    }
    public static PlayerData PlayerData(String uuid){
        for (PlayerData player : Players) {
            if (player.error) return new PlayerData(true, false);
            if (player.uuid.equals(uuid)) {
                return player;
            }
        }
        return new PlayerData(true, false);
    }

    public static boolean PlayerDataRemove(String uuid){
        for(int a=0;a<Players.size();a++){
            PlayerData player = Players.get(a);
            if (player.uuid.equals(uuid)) {
                Players.remove(a);
                return true;
            }
        }
        return false;
    }

    public static boolean PlayerDataSet(String uuid, PlayerData data){
        for(int a=0;a<Players.size();a++){
            PlayerData player = Players.get(a);
            if (player.uuid.equals(uuid)) {
                Players.set(a,data);
                return true;
            }
        }
        return false;
    }

    public static boolean PlayerDataSave(String uuid){
        for (PlayerData player : Players) {
            if (player.uuid.equals(uuid)) {
                try {
                    String sql;
                    if (config.isSqlite()) {
                        sql = "INSERT INTO 'main'.'players' ('name', 'uuid', 'country', 'country_code', 'language', 'isadmin', 'placecount', 'breakcount', 'killcount', 'deathcount', 'joincount', 'kickcount', 'level', 'exp', 'reqexp', 'reqtotalexp', 'firstdate', 'lastdate', 'lastplacename', 'lastbreakname', 'lastchat', 'playtime', 'attackclear', 'pvpwincount', 'pvplosecount', 'pvpbreakout', 'reactorcount', 'bantimeset', 'bantime', 'banned', 'translate', 'crosschat', 'colornick', 'connected', 'connserver', 'permission', 'udid') VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    } else {
                        sql = "INSERT INTO players(name, uuid, country, country_code, language, isadmin, placecount, breakcount, killcount, deathcount, joincount, kickcount, level, exp, reqexp, reqtotalexp, firstdate, lastdate, lastplacename, lastbreakname, lastchat, playtime, attackclear, pvpwincount, pvplosecount, pvpbreakout, reactorcount, bantimeset, bantime, banned, translate, crosschat, colornick, connected, connserver, permission, udid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    }
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, player.name);
                    pstmt.setString(2, player.uuid);
                    pstmt.setString(3, player.country);
                    pstmt.setString(4, player.country_code);
                    pstmt.setString(5, player.language);
                    pstmt.setBoolean(6, player.isAdmin);
                    pstmt.setInt(7, player.placecount); // placecount
                    pstmt.setInt(8, player.breakcount); // breakcount
                    pstmt.setInt(9, player.killcount); // killcount
                    pstmt.setInt(10, player.deathcount); // deathcount
                    pstmt.setInt(11, player.joincount);
                    pstmt.setInt(12, player.kickcount);
                    pstmt.setInt(13, player.level); // level
                    pstmt.setInt(14, player.exp); // exp
                    pstmt.setInt(15, player.reqexp); // reqexp
                    pstmt.setString(16, player.reqtotalexp); // reqtotalexp
                    pstmt.setString(17, player.firstdate);
                    pstmt.setString(18, player.lastdate);
                    pstmt.setString(19, player.lastplacename); // lastplacename
                    pstmt.setString(20, player.lastbreakname); // lastbreakname
                    pstmt.setString(21, player.lastchat); // lastchat
                    pstmt.setString(22, player.playtime); // playtime
                    pstmt.setInt(23, player.attackclear); // attackclear
                    pstmt.setInt(24, player.pvpwincount); // pvpwincount
                    pstmt.setInt(25, player.pvplosecount); // pvplosecount
                    pstmt.setInt(26, player.pvpbreakout); // pvpbreakcount
                    pstmt.setInt(27, player.reactorcount); // reactorcount
                    pstmt.setInt(28, player.bantimeset); // bantimeset
                    pstmt.setString(29, player.bantime); // bantime
                    pstmt.setBoolean(30, player.banned);
                    pstmt.setBoolean(31, player.translate); // translate
                    pstmt.setBoolean(32, player.crosschat); // crosschat
                    pstmt.setBoolean(33, player.colornick); // colornick
                    pstmt.setBoolean(34, player.connected); // connected
                    pstmt.setString(35, player.connserver); // connected server ip
                    pstmt.setString(36, player.permission); // set permission
                    pstmt.setLong(37, player.udid); // UDID
                    pstmt.execute();
                    pstmt.close();
                } catch (Exception e) {
                    printError(e);
                }
                return true;
            }
        }
        return false;
    }
    public static void PlayerDataSaveAll(){
        for (PlayerData player : Players) PlayerDataSave(player.uuid);
    }
}