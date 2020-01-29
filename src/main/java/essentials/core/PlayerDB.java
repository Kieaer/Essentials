package essentials.core;

import essentials.PluginData;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;
import org.hjson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.Global.*;
import static essentials.Threads.ColorNick;
import static essentials.utils.Config.executorService;
import static essentials.utils.Permission.permission;
import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;

public class PlayerDB{
    public static Connection conn;
    public static ArrayList<Player> pvpteam = new ArrayList<>();
    public static ArrayList<PlayerData> Players = new ArrayList<>(); // Players data

    public void run(){
        openconnect();
        createNewDataFile();
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
                            "`crosschat` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`colornick` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`connected` TINYINT(4) NULL DEFAULT NULL,\n" +
                            "`connserver` TEXT NULL DEFAULT 'none',\n" +
                            "`permission` TEXT NULL DEFAULT 'default',\n" +
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
                    sql = "INSERT INTO 'main'.'players' ('name', 'uuid', 'country', 'country_code', 'language', 'isadmin', 'placecount', 'breakcount', 'killcount', 'deathcount', 'joincount', 'kickcount', 'level', 'exp', 'reqexp', 'reqtotalexp', 'firstdate', 'lastdate', 'lastplacename', 'lastbreakname', 'lastchat', 'playtime', 'attackclear', 'pvpwincount', 'pvplosecount', 'pvpbreakout', 'reactorcount', 'bantimeset', 'bantime', 'banned', 'crosschat', 'colornick', 'connected', 'connserver', 'permission', 'udid', 'accountid', 'accountpw') VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                } else {
                    sql = "INSERT INTO players(name, uuid, country, country_code, language, isadmin, placecount, breakcount, killcount, deathcount, joincount, kickcount, level, exp, reqexp, reqtotalexp, firstdate, lastdate, lastplacename, lastbreakname, lastchat, playtime, attackclear, pvpwincount, pvplosecount, pvpbreakout, reactorcount, bantimeset, bantime, banned, crosschat, colornick, connected, connserver, permission, udid, accountid, accountpw) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                pstmt.setBoolean(31, false); // crosschat
                pstmt.setBoolean(32, false); // colornick
                pstmt.setBoolean(33, connected); // connected
                pstmt.setString(34, hostip); // connected server ip
                pstmt.setString(35, "default"); // set permission
                pstmt.setLong(36, udid); // UDID
                pstmt.setString(37, accountid);
                pstmt.setString(38, accountpw);
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
	public static PlayerData getInfo(String uuid){
        PlayerData data = new PlayerData(true,false);
        try {
            String sql = "SELECT * FROM players WHERE uuid=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1,uuid);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                data = new PlayerData(
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
                        rs.getBoolean("crosschat"),
                        rs.getBoolean("colornick"),
                        rs.getBoolean("connected"),
                        rs.getString("connserver"),
                        rs.getString("permission"),
                        rs.getLong("udid"),
                        rs.getString("accountid"),
                        rs.getString("accountpw")
                );
            } else {
                nlog("debug", uuid+" Data not found!");
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

    // DB 사용 (느림)
    public static JsonObject getRaw(String uuid){
        JsonObject data = new JsonObject();
        try {
            String sql = "SELECT * FROM players WHERE uuid='"+uuid+"'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                data.add("id", rs.getInt("id"));
                data.add("name", rs.getString("name"));
                data.add("uuid", rs.getString("uuid"));
                data.add("country", rs.getString("country"));
                data.add("country_code", rs.getString("country_code"));
                data.add("language", rs.getString("language"));
                data.add("isadmin", rs.getBoolean("isadmin"));
                data.add("placecount", rs.getInt("placecount"));
                data.add("breakcount", rs.getInt("breakcount"));
                data.add("killcount", rs.getInt("killcount"));
                data.add("deathcount", rs.getInt("deathcount"));
                data.add("joincount", rs.getInt("joincount"));
                data.add("kickcount", rs.getInt("kickcount"));
                data.add("level", rs.getInt("level"));
                data.add("exp", rs.getInt("exp"));
                data.add("reqexp", rs.getInt("reqexp"));
                data.add("reqtotalexp", rs.getString("reqtotalexp"));
                data.add("firstdate", rs.getString("firstdate"));
                data.add("lastdate", rs.getString("lastdate"));
                data.add("lastplacename", rs.getString("lastplacename"));
                data.add("lastbreakname", rs.getString("lastbreakname"));
                data.add("lastchat", rs.getString("lastchat"));
                data.add("playtime", rs.getString("playtime"));
                data.add("attackclear", rs.getInt("attackclear"));
                data.add("pvpwincount", rs.getInt("pvpwincount"));
                data.add("pvplosecount", rs.getInt("pvplosecount"));
                data.add("pvpbreakout", rs.getInt("pvpbreakout"));
                data.add("reactorcount", rs.getInt("reactorcount"));
                data.add("bantimeset", rs.getString("bantimeset"));
                data.add("bantime", rs.getString("bantime"));
                data.add("banned", rs.getBoolean("banned"));
                data.add("crosschat", rs.getBoolean("crosschat"));
                data.add("colornick", rs.getBoolean("colornick"));
                data.add("connected", rs.getBoolean("connected"));
                data.add("connserver", rs.getString("connserver"));
                data.add("permission", rs.getString("permission"));
                data.add("udid",rs.getString("udid"));
                data.add("accountid", rs.getString("accountid"));
                data.add("accountpw", rs.getString("accountpw"));
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
            PluginData.banned.add(new PluginData.banned(LocalDateTime.now().plusHours(bantimeset),name,uuid));
            PlayerData player = PlayerData(uuid);
            player.bantime = getTime();
            player.bantimeset = bantimeset;
            PlayerDataSet(uuid,player);
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
        if(checkpw(player,id,pw)) {
            String hashed = BCrypt.hashpw(pw, BCrypt.gensalt(11));
            HashMap<String, String> list = geolocation(player);
            if (!isduplicate(player)) {
                if (createNewDatabase(
                        player.name, // 이름
                        player.uuid, // UUID
                        list.get("country"), // 국가명
                        list.get("country_code"), // 국가 코드
                        list.get("languages"), // 언어
                        player.isAdmin, // 관리자 여부
                        netServer.admins.getInfo(player.uuid).timesJoined, // 총 서버 입장횟수
                        netServer.admins.getInfo(player.uuid).timesKicked, // 총 서버 강퇴횟수
                        getTime(), // 최초 접속일
                        getTime(), // 마지막 접속일
                        true, // 서버 연결여부
                        0L, // Discord UDID
                        id, // 계정 ID
                        hashed, // 계정 비밀번호
                        player) // 플레이어
                ) {
                    nlog("debug", player.name + " Player DB Created!");
                    player.sendMessage(bundle(player, "player-name-changed", player.name));
                    return true;
                } else {
                    nlog("debug", player.name + " Player DB create failed!");
                    return false;
                }
            } else {
                player.sendMessage("[green][Essentials] [orange]This account id is already in use!\n" +
                        "[green][Essentials] [orange]이 계정명은 이미 사용중입니다!");
                log("player", "password-already-accountid", id);
                return false;
            }
        }
        return false;
    }
    // 비 로그인 기능 사용시 계정등록
    public boolean register(Player player) {
        if (!isduplicate(player)) { // 계정 중복 확인
            HashMap<String, String> list = geolocation(player);
            return createNewDatabase(
                    player.name, // 이름
                    player.uuid, // UUID
                    list.get("country"), // 국가명
                    list.get("country_code"), // 국가 코드
                    list.get("languages"), // 언어
                    player.isAdmin, // 관리자 여부
                    netServer.admins.getInfo(player.uuid).timesJoined, // 총 서버 입장횟수
                    netServer.admins.getInfo(player.uuid).timesKicked, // 총 서버 강퇴횟수
                    getTime(), // 최초 접속일
                    getTime(), // 마지막 접속일
                    true, // 서버 연결여부
                    0L, // Discord UDID
                    player.name, // 계정 ID
                    "blank", // 계정 PW
                    player // 플레이어
            );
        } else {
            return false;
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
    public void load(Player target) {
        Thread thread = new Thread(() -> {
            getInfo(target.uuid);
            PlayerData player = PlayerData(target.uuid);

            // 새 기기로 UUID 적용
            player.uuid = target.uuid;
            player.connected = true;
            player.lastdate = getTime();
            player.connserver = hostip;

            // 이 계정이 밴을 당했을 때 강퇴처리
            if (player.banned) {
                netServer.admins.banPlayerID(player.uuid);
                Call.onKick(target.con,"This account can't use.");
                return;
            }

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
            target.kill();

            // 입장 메세지 표시
            String motd = getmotd(target);
            int count = motd.split("\r\n|\r|\n").length;
            if (count > 10) {
                Call.onInfoMessage(target.con, motd);
            } else {
                target.sendMessage(motd);
            }

            // 고정닉 기능이 켜져있을 경우, 플레이어 닉네임 설정
            if (config.isRealname() || config.getPasswordmethod().equals("discord")) target.name = player.name;

            // 서버 입장시 경험치 획득
            player.exp = player.exp+player.joincount;

            // 컬러닉 기능 설정
            if (player.colornick){
                if(config.isRealname()){
                    new Thread(new ColorNick(target)).start();
                } else {
                    player.colornick = false;
                }
            }

            // 플레이어가 관리자 그룹에 있을경우 관리자모드 설정
            if (permission.get(player.permission).asObject().getBoolean("admin", false)) {
                target.isAdmin = true;
                player.isAdmin = true;
            }

            // 플레이어 위치 정보가 없을경우, 위치 정보 가져오기
            if (player.country.equals("invalid")) {
                HashMap<String, String> list = geolocation(target);
                player.country = list.get("country");
                player.country_code = list.get("country_code");
                player.language = list.get("languages");
            }

            // 플레이어 접속 횟수 카운트
            player.joincount = player.joincount++;

            // 데이터 저장
            PlayerDataSet(target.uuid, player);
            PlayerDataSave(player);
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

        public PlayerData(int id, String name, String uuid, String country, String country_code, String language, boolean isAdmin, int placecount, int breakcount, int killcount, int deathcount, int joincount, int kickcount, int level, int exp, int reqexp, String reqtotalexp, String firstdate, String lastdate, String lastplacename, String lastbreakname, String lastchat, String playtime, int attackclear, int pvpwincount, int pvplosecount, int pvpbreakout, int reactorcount, int bantimeset, String bantime, boolean banned, boolean crosschat, boolean colornick, boolean connected, String connserver, String permission, Long udid, String accountid, String accountpw){
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
            this.crosschat = crosschat;
            this.colornick = colornick;
            this.connected = connected;
            this.connserver = connserver;
            this.permission = permission;
            this.udid = udid;
            this.accountid = accountid;
            this.accountpw = accountpw;

            this.error = false;
            this.isLogin = true;
        }
    }

    public static PlayerData PlayerData(String uuid){
        PlayerData data = new PlayerData(true, false);
        for (PlayerData player : Players) {
            if (!player.error && player.uuid.equals(uuid)) {
                data = player;
            }
        }
        return data;
    }

    public static void PlayerDataRemove(PlayerData data){
        Players.remove(data);
    }

    public static boolean PlayerDataSet(String uuid, PlayerData data){
        for(int a=0;a<Players.size();a++){
            PlayerData player = Players.get(a);
            if (!player.error && player.uuid.equals(uuid)) {
                Players.set(a,data);
                return true;
            }
        }
        return false;
    }

    public static void PlayerDataSave(PlayerData data) {
        try {
            String sql = "UPDATE players SET name=?,uuid=?,country=?,country_code=?,language=?,isadmin=?,placecount=?,breakcount=?,killcount=?,deathcount=?,joincount=?,kickcount=?,level=?,exp=?,reqexp=?,reqtotalexp=?,firstdate=?,lastdate=?,lastplacename=?,lastbreakname=?,lastchat=?,playtime=?,attackclear=?,pvpwincount=?,pvplosecount=?,pvpbreakout=?,reactorcount=?,bantimeset=?,bantime=?,banned=?,crosschat=?,colornick=?,connected=?,connserver=?,permission=?,udid=? WHERE uuid=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, data.name);
            pstmt.setString(2, data.uuid);
            pstmt.setString(3, data.country);
            pstmt.setString(4, data.country_code);
            pstmt.setString(5, data.language);
            pstmt.setBoolean(6, data.isAdmin);
            pstmt.setInt(7, data.placecount); // placecount
            pstmt.setInt(8, data.breakcount); // breakcount
            pstmt.setInt(9, data.killcount); // killcount
            pstmt.setInt(10, data.deathcount); // deathcount
            pstmt.setInt(11, data.joincount);
            pstmt.setInt(12, data.kickcount);
            pstmt.setInt(13, data.level); // level
            pstmt.setInt(14, data.exp); // exp
            pstmt.setInt(15, data.reqexp); // reqexp
            pstmt.setString(16, data.reqtotalexp); // reqtotalexp
            pstmt.setString(17, data.firstdate);
            pstmt.setString(18, data.lastdate);
            pstmt.setString(19, data.lastplacename); // lastplacename
            pstmt.setString(20, data.lastbreakname); // lastbreakname
            pstmt.setString(21, data.lastchat); // lastchat
            pstmt.setString(22, data.playtime); // playtime
            pstmt.setInt(23, data.attackclear); // attackclear
            pstmt.setInt(24, data.pvpwincount); // pvpwincount
            pstmt.setInt(25, data.pvplosecount); // pvplosecount
            pstmt.setInt(26, data.pvpbreakout); // pvpbreakcount
            pstmt.setInt(27, data.reactorcount); // reactorcount
            pstmt.setInt(28, data.bantimeset); // bantimeset
            pstmt.setString(29, data.bantime); // bantime
            pstmt.setBoolean(30, data.banned);
            pstmt.setBoolean(31, data.crosschat); // crosschat
            pstmt.setBoolean(32, data.colornick); // colornick
            pstmt.setBoolean(33, data.connected); // connected
            pstmt.setString(34, data.connserver); // connected server ip
            pstmt.setString(35, data.permission); // set permission
            pstmt.setLong(36, data.udid); // UDID
            pstmt.setString(37, data.uuid);
            pstmt.execute();
            pstmt.close();
        } catch (Exception e) {
            printError(e);
        }
    }

    public static void PlayerDataSaveAll(){
        for (PlayerData player : Players) PlayerDataSave(player);
    }
}