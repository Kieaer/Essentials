package essentials.core;

import essentials.PluginData;
import essentials.special.sendMail;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;
import org.hjson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.Global.*;
import static essentials.Main.*;
import static essentials.Threads.ColorNick;
import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;

public class PlayerDB {
    public static Connection conn;
    public static ArrayList<Player> pvpteam = new ArrayList<>();
    public static ArrayList<PlayerData> Players = new ArrayList<>(); // Players data
    int DBVersion = 3;

    public PlayerDB(){
        openconnect();
        createNewDataFile();
        Upgrade();
    }

    public void createNewDataFile(){
        try {
            String sql = null;
            if(config.isSqlite()){
                sql = "CREATE TABLE IF NOT EXISTS players (\n" +
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
                        "mute TEXT,\n" +
                        "udid TEXT,\n" +
                        "email TEXT,\n" +
                        "accountid TEXT,\n" +
                        "accountpw TEXT\n" +
                        ");";
            } else {
                if(!config.getDBid().isEmpty()){
                    sql = "CREATE TABLE IF NOT EXISTS `players` (\n" +
                            "`name` TEXT NOT NULL,\n" +
                            "`uuid` TEXT NOT NULL,\n" +
                            "`country` TEXT NOT NULL,\n" +
                            "`country_code` TEXT NOT NULL,\n" +
                            "`language` TEXT NOT NULL,\n" +
                            "`isadmin` TINYINT(4) NOT NULL,\n" +
                            "`placecount` INT(11) NOT NULL,\n" +
                            "`breakcount` INT(11) NOT NULL,\n" +
                            "`killcount` INT(11) NOT NULL,\n" +
                            "`deathcount` INT(11) NOT NULL,\n" +
                            "`joincount` INT(11) NOT NULL,\n" +
                            "`kickcount` INT(11) NOT NULL,\n" +
                            "`level` INT(11) NOT NULL,\n" +
                            "`exp` INT(11) NOT NULL,\n" +
                            "`reqexp` INT(11) NOT NULL,\n" +
                            "`reqtotalexp` TEXT NOT NULL,\n" +
                            "`firstdate` TEXT NOT NULL,\n" +
                            "`lastdate` TEXT NOT NULL,\n" +
                            "`lastplacename` TEXT NOT NULL,\n" +
                            "`lastbreakname` TEXT NOT NULL,\n" +
                            "`lastchat` TEXT NOT NULL,\n" +
                            "`playtime` TEXT NOT NULL,\n" +
                            "`attackclear` INT(11) NOT NULL,\n" +
                            "`pvpwincount` INT(11) NOT NULL,\n" +
                            "`pvplosecount` INT(11) NOT NULL,\n" +
                            "`pvpbreakout` INT(11) NOT NULL,\n" +
                            "`reactorcount` INT(11) NOT NULL,\n" +
                            "`bantimeset` INT(11) NOT NULL,\n" +
                            "`bantime` TINYTEXT NOT NULL,\n" +
                            "`banned` TINYINT(4) NOT NULL,\n" +
                            "`translate` TINYINT(4) NOT NULL,\n" +
                            "`crosschat` TINYINT(4) NOT NULL,\n" +
                            "`colornick` TINYINT(4) NOT NULL,\n" +
                            "`connected` TINYINT(4) NOT NULL,\n" +
                            "`connserver` TINYTEXT NOT NULL DEFAULT 'none',\n" +
                            "`permission` TINYTEXT NOT NULL DEFAULT 'default',\n" +
                            "`mute` TINYTEXT NOT NULL,\n" +
                            "`udid` TEXT NOT NULL,\n" +
                            "`email` TEXT NOT NULL,\n" +
                            "`accountid` TEXT NOT NULL,\n" +
                            "`accountpw` TEXT NOT NULL\n" +
                            ")\n" +
                            "COLLATE='utf8_general_ci'\n" +
                            "ENGINE=InnoDB\n" +
                            ";";
                } else {
                    log(LogType.playererr,"db-address-notset");
                }
            }
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            if(config.isSqlite()){
                sql = "CREATE TABLE IF NOT EXISTS data (dbversion INTEGER);";
            } else {
                sql = "CREATE TABLE IF NOT EXISTS `data` (`dbversion` TINYINT(4) NOT NULL)" +
                        "COLLATE='utf8_general_ci'\n" +
                        "ENGINE=InnoDB;";
            }
            stmt.execute(sql);
            stmt.close();
            PreparedStatement ptmt = conn.prepareStatement("SELECT dbversion from data");
            ResultSet rs = ptmt.executeQuery();
            ptmt.close();
            if(!rs.next()){
                ptmt = conn.prepareStatement("INSERT INTO data (dbversion) VALUES (?);");
                ptmt.setInt(1,DBVersion);
                ptmt.execute();
                ptmt.close();
            }
        } catch (Exception ex){
            printError(ex);
        }
    }

	public static boolean createNewDatabase(String name, String uuid, String country, String country_code, String language, Boolean isAdmin, int joincount, int kickcount, String firstdate, String lastdate, boolean connected, Long udid, String email, String accountid, String accountpw, Player player) {
        boolean result = false;
        try {
            if(uuid.equals("InactiveAAA=") || !isduplicate(uuid)){
                /*String sql = "INSERT INTO players (" +
                        "name = ?,uuid = ?,country = ?,country_code = ?,language = ?,isadmin = ?," +
                        "placecount = ?,breakcount = ?,killcount = ?,deathcount = ?,joincount = ?,kickcount = ?,deathcount = ?,joincount = ?,kickcount = ?," +
                        "level = ?,exp = ?,reqexp = ?,reqtotalexp = ?,firstdate = ?,lastdate = ?,lastplacename = ?,lastbreakname = ?,lastchat = ?,playtime = ?," +
                        "attackclear = ?,pvpwincount = ?,pvplosecount = ?,pvpbreakout = ?,reactorcount = ?,bantimeset = ?,bantime = ?,banned = ?,translate = ?," +
                        "crosschat = ?,colornick = ?,connected = ?,connserver = ?,permission = ?,mute = ?,udid = ?,email = ?,accountid = ?,accountpw = ?" +
                        ")";*/
                String sql = "INSERT INTO players VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
                pstmt.setBoolean(30, false); // banned
                pstmt.setBoolean(31, false); // translate
                pstmt.setBoolean(32, false); // crosschat
                pstmt.setBoolean(33, false); // colornick
                pstmt.setBoolean(34, connected); // connected
                pstmt.setString(35, getip()); // connected server ip
                pstmt.setString(36, "default"); // set permission
                pstmt.setBoolean(37, false); // mute
                pstmt.setLong(38, udid); // UDID
                pstmt.setString(39, email); // email
                pstmt.setString(40, accountid);
                pstmt.setString(41, accountpw);
                pstmt.execute();
                pstmt.close();
                //if(player != null) player.sendMessage(bundle(new Locale(country, country_code), "player-id", player.name));
                log(LogType.player,"player-db-created", name);
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
	public static PlayerData getInfo(String type, String value){
        PlayerData data = new PlayerData(true,false);
        try {
            String sql;
            if(type.equals("id")){
                sql = "SELECT * FROM players WHERE accountid=?";
            } else {
                sql = "SELECT * FROM players WHERE uuid=?";
            }
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1,value);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                data = new PlayerData(
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
                        rs.getBoolean("mute"),
                        rs.getLong("udid"),
                        rs.getString("email"),
                        rs.getString("accountid"),
                        rs.getString("accountpw")
                );
            } else {
                nlog(LogType.debug, value+" Data not found!");
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
                data.add("translate", rs.getBoolean("translate"));
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

	public static void addtimeban(String name, String uuid, int bantimeset, String reason) {
        // Write ban data
        try {
            data.banned.add(new PluginData.banned(LocalDateTime.now().plusHours(bantimeset),name,uuid,reason));
            PlayerData target = PlayerData(uuid);
            target.bantime = getTime();
            target.bantimeset = bantimeset;
            PlayerDataSet(target);
        } catch (Exception e) {
            printError(e);
        }
    }

    public static boolean accountban(boolean ban, String uuid, String reason) {
        PlayerData player = getInfo("id", uuid);
        if (!player.error) {
            if (ban) {
                player.banned = true;
                data.banned.add(new PluginData.banned(LocalDateTime.now().plusYears(1000),player.name,uuid,reason));
                PlayerDataSet(player);
                return true;
            } else {
                player.banned = false;
                PlayerDataSet(player);
                for(int a=0;a<data.banned.size();a++){
                    if(data.banned.get(a).uuid.equals(uuid)){
                        data.banned.remove(a);
                        break;
                    }
                }
                return false;
            }
        } else {
            return false;
        }
    }

    public static void openconnect() {
        try {
            if (config.isSqlite()) {
                conn = DriverManager.getConnection(config.getDBurl());
                log(LogType.player,"db-type","SQLite");
            } else {
                if (!config.getDBid().isEmpty()) {
                    conn = DriverManager.getConnection(config.getDBurl(), config.getDBid(), config.getDBpw());
                    log(LogType.player,"db-type","MariaDB/MySQL/PostgreSQL");
                } else {
                    conn = DriverManager.getConnection(config.getDBurl());
                    log(LogType.player,"db-type","Invalid");
                }
            }
        } catch (SQLException e){
            printError(e);
            nlog(LogType.warn,"SQL ERROR!");
        }
    }

    public static void closeconnect(){
        try {
            conn.close();
        } catch (Exception e) {
            printError(e);
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
        config.executorService.submit(t);
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
            log(LogType.player,"password-match-regex", player.name);
            return false;
        } else if (matcher2.find()) {
            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            player.sendMessage("[green][Essentials] [sky]Passwords should not be similar to nicknames!\n" +
                    "[green][Essentials] [sky]비밀번호는 닉네임과 비슷하면 안됩니다!");
            log(LogType.player,"password-match-name", player.name);
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
            log(LogType.player,"password-match-blank", player.name);
            return false;
        } else if (pw.matches("<(.*?)>")) {
            // 비밀번호 형식이 "<비밀번호>" 일경우
            player.sendMessage("[green][Essentials] [green]<[sky]password[green]>[sky] format isn't allowed!\n" +
                    "[green][Essentials] [sky]Use /register password\n" +
                    "[green][Essentials] [green]<[sky]비밀번호[green]>[sky] 형식은 허용되지 않습니다!\n" +
                    "[green][Essentials] [sky]/register password 형식으로 사용하세요.");
            log(LogType.player,"password-match-invalid", player.name);
            return false;
        }
        return true;
    }

	// 로그인 기능 사용시 계정 등록
	public boolean register(Player player, String id, String pw, String type, String... parameter) {
        // 비밀번호 보안 확인
        if(checkpw(player,id,pw)) {
            String hashed = BCrypt.hashpw(pw, BCrypt.gensalt(11));
            Locale locale = geolocation(player);
            if (!isduplicate(player)) {
                switch (type) {
                    case "password":
                        if (createNewDatabase(
                                player.name, // 이름
                                player.uuid, // UUID
                                locale.getDisplayCountry(Locale.US), // 국가명
                                locale.toString(), // 국가 코드
                                locale.getLanguage(), // 언어
                                player.isAdmin, // 관리자 여부
                                netServer.admins.getInfo(player.uuid).timesJoined, // 총 서버 입장횟수
                                netServer.admins.getInfo(player.uuid).timesKicked, // 총 서버 강퇴횟수
                                getTime(), // 최초 접속일
                                getTime(), // 마지막 접속일
                                true, // 서버 연결여부
                                Long.MIN_VALUE, // Discord UDID
                                "none", // Email
                                id, // 계정 ID
                                hashed, // 계정 비밀번호
                                player) // 플레이어
                        ) {
                            nlog(LogType.debug, player.name + " Player DB Created!");
                            player.sendMessage(bundle(locale, "player-name-changed", player.name));
                            return true;
                        } else {
                            nlog(LogType.debug, player.name + " Player DB create failed!");
                            return false;
                        }
                    case "email":
                        char[] passwordTable =  { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
                                'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                                'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                                'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
                                'w', 'x', 'y', 'z', '!', '@', '#', '$', '%', '^', '&', '*',
                                '(', ')', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' };
                        Random random = new Random(System.currentTimeMillis());
                        int tablelength = passwordTable.length;
                        StringBuilder buf = new StringBuilder();

                        for(int i = 0; i < 7; i++) {
                            buf.append(passwordTable[random.nextInt(tablelength)]);
                        }

                        String text = "인증 번호는 "+buf.toString()+" 입니다.\n서버 안에서 /email "+buf.toString()+" 명령어를 입력하여 계정 등록을 하세요.";
                        sendMail mail = new sendMail(config.getEmailServer(),config.getEmailPort(),config.getEmailAccountID(),config.getEmailPassword(),config.getEmailUsername(),player.name,parameter[0],"서버 계정등록 확인", text);
                        mail.main();

                        player.sendMessage("Mail sented! Please check your mail!");
                        player.sendMessage("Enter the /email command to enter your email verification number.");
                        data.emailauth.add(new PluginData.maildata(player.uuid, buf.toString(), id, pw, parameter[0]));
                        break;
                    case "emailauth":
                        if (createNewDatabase(
                                player.name, // 이름
                                player.uuid, // UUID
                                locale.getDisplayCountry(Locale.US), // 국가명
                                locale.toString(), // 국가 코드
                                locale.getLanguage(), // 언어
                                player.isAdmin, // 관리자 여부
                                netServer.admins.getInfo(player.uuid).timesJoined, // 총 서버 입장횟수
                                netServer.admins.getInfo(player.uuid).timesKicked, // 총 서버 강퇴횟수
                                getTime(), // 최초 접속일
                                getTime(), // 마지막 접속일
                                true, // 서버 연결여부
                                Long.MIN_VALUE, // Discord UDID
                                parameter[0], // Email
                                id, // 계정 ID
                                hashed, // 계정 비밀번호
                                player) // 플레이어
                        ) {
                            nlog(LogType.debug, player.name + " Player DB Created!");
                            player.sendMessage(bundle(locale, "player-name-changed", player.name));
                            return true;
                        } else {
                            nlog(LogType.debug, player.name + " Player DB create failed!");
                            return false;
                        }
                }
            } else {
                player.sendMessage("[green][Essentials] [orange]This account id is already in use!\n" +
                        "[green][Essentials] [orange]이 계정명은 이미 사용중입니다!");
                log(LogType.player, "password-already-accountid", id);
                return false;
            }
        }
        return false;
    }
    // 비 로그인 기능 사용시 계정등록
    public boolean register(Player player) {
        if (!isduplicate(player)) { // 계정 중복 확인
            Locale locale = geolocation(player);
            return createNewDatabase(
                    player.name, // 이름
                    player.uuid, // UUID
                    locale.getDisplayCountry(Locale.US), // 국가명
                    locale.toString(), // 국가 코드
                    locale.getLanguage(), // 언어
                    player.isAdmin, // 관리자 여부
                    netServer.admins.getInfo(player.uuid).timesJoined, // 총 서버 입장횟수
                    netServer.admins.getInfo(player.uuid).timesKicked, // 총 서버 강퇴횟수
                    getTime(), // 최초 접속일
                    getTime(), // 마지막 접속일
                    true, // 서버 연결여부
                    0L, // Discord UDID
                    "none", // Email
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
                    player.con.kick(nbundle(PlayerData(player.uuid).locale, "tried-connected-account"));
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
    public void load(Player player, String... parameter) {
        Thread thread = new Thread(() -> {
            PlayerData target;
            if(config.getPasswordmethod().equals("discord")){
                target = getInfo("id", parameter[0]);
                Players.add(getInfo("id", parameter[0]));
            } else {
                target = getInfo("uuid", player.uuid);
                Players.add(target);
            }

            // 새 기기로 UUID 적용
            target.uuid = player.uuid;
            target.connected = true;
            target.lastdate = getTime();
            target.connserver = hostip;

            // 이 계정이 밴을 당했을 때 강퇴처리
            if (target.banned) {
                netServer.admins.banPlayerID(target.uuid);
                Call.onKick(player.con,"This account can't use.");
                return;
            }

            // 플레이어 팀 설정

            player.kill();
            if (Vars.state.rules.pvp) {
                boolean match = false;
                for (Player t : pvpteam) {
                    Team team = t.getTeam();
                    if (target.uuid.equals(t.uuid)) {
                        if (Vars.state.teams.get(team).cores.isEmpty()) {
                            break;
                        } else {
                            player.setTeam(team);
                            match = true;
                        }
                    }
                }
                if (!match) {
                    player.setTeam(netServer.assignTeam(player, playerGroup.all()));
                    pvpteam.add(player);
                }
            } else {
                player.setTeam(Team.sharded);
            }

            // 입장 메세지 표시
            String motd = getmotd(player);
            int count = motd.split("\r\n|\r|\n").length;
            if (count > 10) {
                Call.onInfoMessage(player.con, motd);
            } else {
                player.sendMessage(motd);
            }

            // 고정닉 기능이 켜져있을 경우, 플레이어 닉네임 설정
            if (config.isRealname() || config.getPasswordmethod().equals("discord")) player.name = target.name;

            // 서버 입장시 경험치 획득
            target.exp = target.exp+target.joincount;

            // 컬러닉 기능 설정
            if (target.colornick){
                if(config.isRealname()){
                    // TODO edit name tag from mindustry sources
                    new Thread(new ColorNick(player)).start();
                } else {
                    target.colornick = false;
                }
            }

            // 플레이어가 관리자 그룹에 있을경우 관리자모드 설정
            if (perm.permission.get(target.permission).asObject().getBoolean("admin", false)) {
                target.isAdmin = true;
                player.isAdmin = true;
            }

            // 플레이어 위치 정보가 없을경우, 위치 정보 가져오기
            if (target.country.equals("invalid")) {
                Locale locale = geolocation(player);
                target.country = locale.getDisplayCountry(Locale.US);
                target.country_code = locale.toString();
                target.language = locale.getLanguage();
            }

            // 플레이어 접속 횟수 카운트
            target.joincount = target.joincount++;

            // 데이터 저장
            if(config.getPasswordmethod().equals("discord")) {
                for (int a = 0; a < Players.size(); a++) {
                    PlayerData p = Players.get(a);
                    if (!p.error && p.accountid.equals(parameter[0])) {
                        Players.set(a, target);
                        return;
                    }
                }
                PlayerDataSaveUUID(target, target.accountid);
            } else {
                PlayerDataSet(target);
                PlayerDataSave(target);
            }
        });
        config.executorService.submit(thread);
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
        public boolean mute;
        public Long udid;
        public String email;
        public String accountid;
        public String accountpw;

        public boolean error;
        public boolean isLogin;

        public LocalTime afk = LocalTime.of(0,0,0);
        public int afk_tilex = 0;
        public int afk_tiley = 0;

        public int grief_build_count = 0; // 블럭 설치 계산
        public int grief_destory_count = 0; // 블럭 파괴 계산
        public ArrayList<short[]> grief_tilelist = new ArrayList<>(); // 건설한 블록 개수

        public Locale locale = config.getLanguage();

        PlayerData(boolean error, boolean isLogin){
            this.error = error;
            this.isLogin = isLogin;
        }

        public PlayerData(String name, String uuid, String country, String country_code, String language, boolean isAdmin, int placecount, int breakcount, int killcount, int deathcount, int joincount, int kickcount, int level, int exp, int reqexp, String reqtotalexp, String firstdate, String lastdate, String lastplacename, String lastbreakname, String lastchat, String playtime, int attackclear, int pvpwincount, int pvplosecount, int pvpbreakout, int reactorcount, int bantimeset, String bantime, boolean banned, boolean translate, boolean crosschat, boolean colornick, boolean connected, String connserver, String permission, boolean mute, Long udid, String email, String accountid, String accountpw){
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
            this.mute = mute;
            this.udid = udid;
            this.email = email;
            this.accountid = accountid;
            this.accountpw = accountpw;

            this.error = false;
            this.isLogin = true;

            this.locale = TextToLocale(country_code);
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

    public static void PlayerDataSet(PlayerData data){
        for(int a=0;a<Players.size();a++){
            PlayerData player = Players.get(a);
            if (!player.error && player.uuid.equals(data.uuid)) {
                Players.set(a,data);
                return;
            }
        }
    }

    public static void PlayerDataSave(PlayerData data) {
        try {
            String sql = "UPDATE players SET name=?,uuid=?,country=?,country_code=?,language=?,isadmin=?,placecount=?,breakcount=?,killcount=?,deathcount=?,joincount=?,kickcount=?,level=?,exp=?,reqexp=?,reqtotalexp=?,firstdate=?,lastdate=?,lastplacename=?,lastbreakname=?,lastchat=?,playtime=?,attackclear=?,pvpwincount=?,pvplosecount=?,pvpbreakout=?,reactorcount=?,bantimeset=?,bantime=?,banned=?,translate=?,crosschat=?,colornick=?,connected=?,connserver=?,permission=?,mute=?,udid=? WHERE uuid=?";
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
            pstmt.setBoolean(31, data.translate); // translate
            pstmt.setBoolean(32, data.crosschat); // crosschat
            pstmt.setBoolean(33, data.colornick); // colornick
            pstmt.setBoolean(34, data.connected); // connected
            pstmt.setString(35, data.connserver); // connected server ip
            pstmt.setString(36, data.permission); // set permission
            pstmt.setBoolean(37,data.mute); // mute
            pstmt.setLong(38, data.udid); // UDID
            pstmt.setString(39, data.uuid);
            pstmt.execute();
            pstmt.close();
        } catch (Exception e) {
            printError(e);
        }
    }

    public static void PlayerDataSaveUUID(PlayerData data, String accountid) {
        try {
            String sql = "UPDATE players SET name=?,uuid=?,country=?,country_code=?,language=?,isadmin=?,placecount=?,breakcount=?,killcount=?,deathcount=?,joincount=?,kickcount=?,level=?,exp=?,reqexp=?,reqtotalexp=?,firstdate=?,lastdate=?,lastplacename=?,lastbreakname=?,lastchat=?,playtime=?,attackclear=?,pvpwincount=?,pvplosecount=?,pvpbreakout=?,reactorcount=?,bantimeset=?,bantime=?,banned=?,translate=?,crosschat=?,colornick=?,connected=?,connserver=?,permission=?,mute=?,udid=? WHERE accountid=?";
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
            pstmt.setBoolean(31, data.translate); // translate
            pstmt.setBoolean(32, data.crosschat); // crosschat
            pstmt.setBoolean(33, data.colornick); // colornick
            pstmt.setBoolean(34, data.connected); // connected
            pstmt.setString(35, data.connserver); // connected server ip
            pstmt.setString(36, data.permission); // set permission
            pstmt.setBoolean(37,data.mute); // mute
            pstmt.setLong(38, data.udid); // UDID
            pstmt.setString(39, accountid);
            pstmt.execute();
            pstmt.close();
        } catch (Exception e) {
            printError(e);
        }
    }

    public static void PlayerDataSaveAll(){
        // java.util.ConcurrentModificationException
        Iterator<PlayerData> iter = Players.iterator();
        while (iter.hasNext()) {
            PlayerData player = iter.next();
            /*System.out.println("name: "+player.name);
            System.out.println("uuid: "+player.uuid);
            System.out.println("country: "+player.country);
            System.out.println("country_code: "+player.country_code);
            System.out.println("language: "+player.language);
            System.out.println("isAdmin: "+player.isAdmin);
            System.out.println("placecount: "+player.placecount);
            System.out.println("breakcount: "+player.breakcount);
            System.out.println("killcount: "+player.killcount);
            System.out.println("deathcount: "+player.deathcount);
            System.out.println("joincount: "+player.joincount);
            System.out.println("kickcount: "+player.kickcount);
            System.out.println("level: "+player.level);
            System.out.println("exp: "+player.exp);
            System.out.println("reqexp: "+player.reqexp);
            System.out.println("reqtotalexp: "+player.reqtotalexp);
            System.out.println("firstdate: "+player.firstdate);
            System.out.println("lastdate: "+player.lastdate);
            System.out.println("lastplacename: "+player.lastplacename);
            System.out.println("lastbreakname: "+player.lastbreakname);
            System.out.println("lastchat: "+player.lastchat);
            System.out.println("playtime: "+player.playtime);
            System.out.println("attackclear: "+player.attackclear);
            System.out.println("pvpwincount: "+player.pvpwincount);
            System.out.println("pvplosecount: "+player.pvplosecount);
            System.out.println("pvpbreakout: "+player.pvpbreakout);
            System.out.println("reactorcount: "+player.reactorcount);
            System.out.println("bantimeset: "+player.bantimeset);
            System.out.println("bantime: "+player.bantime);
            System.out.println("banned: "+player.banned);
            System.out.println("crosschat: "+player.crosschat);
            System.out.println("colornick: "+player.colornick);
            System.out.println("connected: "+player.connected);
            System.out.println("connserver: "+player.connserver);
            System.out.println("permission: "+player.permission);
            System.out.println("udid: "+player.udid);
            System.out.println("uuid: "+player.uuid);*/
            PlayerDataSave(player);
        }
    }

    public void Upgrade(){
        ArrayList<PlayerData> buffer = new ArrayList<>();
        try{
            PreparedStatement pstm = conn.prepareStatement("SELECT dbversion from data");
            ResultSet rs = pstm.executeQuery();
            rs.next();
            int current_version = rs.getInt("dbversion");
            if(current_version < DBVersion) {
                conn.prepareStatement("ALTER table players ADD column IF NOT EXISTS mute TEXT AFTER permission").execute();
                conn.prepareStatement("ALTER table players ADD column IF NOT EXISTS email TEXT AFTER udid").execute();
                pstm = conn.prepareStatement("SELECT * FROM players");
                rs = pstm.executeQuery();
                boolean mute;
                String email;
                while (rs.next()) {
                    mute = current_version >= 2 && rs.getBoolean("mute");
                    email = current_version > 3 ? rs.getString("email") : "none";
                    buffer.add(new PlayerData(
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
                                    mute,
                                    rs.getLong("udid"),
                                    email,
                                    rs.getString("accountid"),
                                    rs.getString("accountpw")
                            )
                    );
                }
                rs.close();
                conn.prepareStatement("DROP TABLE players").execute();
                createNewDataFile();
                String sql;
                if (config.isSqlite()) {
                    sql = "INSERT INTO players ('name', 'uuid', 'country', 'country_code', 'language', 'isadmin', 'placecount', 'breakcount', 'killcount', 'deathcount', 'joincount', 'kickcount', 'level', 'exp', 'reqexp', 'reqtotalexp', 'firstdate', 'lastdate', 'lastplacename', 'lastbreakname', 'lastchat', 'playtime', 'attackclear', 'pvpwincount', 'pvplosecount', 'pvpbreakout', 'reactorcount', 'bantimeset', 'bantime', 'banned', 'translate', 'crosschat', 'colornick', 'connected', 'connserver', 'permission', 'mute', 'email', 'udid', 'accountid', 'accountpw') VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                } else {
                    sql = "INSERT INTO players (name, uuid, country, country_code, language, isadmin, placecount, breakcount, killcount, deathcount, joincount, kickcount, level, exp, reqexp, reqtotalexp, firstdate, lastdate, lastplacename, lastbreakname, lastchat, playtime, attackclear, pvpwincount, pvplosecount, pvpbreakout, reactorcount, bantimeset, bantime, banned, translate, crosschat, colornick, connected, connserver, permission, mute, udid, email, accountid, accountpw) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                }
                for (PlayerData data : buffer) {
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
                    pstmt.setString(39, data.bantime); // bantime
                    pstmt.setBoolean(30, data.banned);
                    pstmt.setBoolean(31, data.translate); // translate
                    pstmt.setBoolean(32, data.crosschat); // crosschat
                    pstmt.setBoolean(33, data.colornick); // colornick
                    pstmt.setBoolean(34, data.connected); // connected
                    pstmt.setString(35, data.connserver); // connected server ip
                    pstmt.setString(36, data.permission); // set permission
                    pstmt.setBoolean(37, data.mute); // mute
                    pstmt.setLong(38, data.udid); // UDID
                    pstmt.setString(39, data.email);
                    pstmt.setString(40, data.accountid);
                    pstmt.setString(41, data.accountpw);
                    pstmt.execute();
                    pstmt.close();
                }
                current_version++;
                log(LogType.player,"db-upgrade");
            }
            if(current_version < DBVersion) {
                if (!config.isSqlite()) {
                    conn.prepareStatement("ALTER TABLE players CHANGE COLUMN uuid uuid TINYTEXT NULL DEFAULT NULL AFTER name;").execute();
                }
                conn.prepareStatement("UPDATE data SET dbversion="+DBVersion).execute();
                if(current_version <= 3) {
                    PreparedStatement reset = conn.prepareStatement("UPDATE players SET uuid = ?");
                    reset.setString(1, "none");
                    reset.execute();
                }
                log(LogType.player,"db-upgrade");
            }
        }catch (SQLException e){
            printError(e);
        }
    }
}