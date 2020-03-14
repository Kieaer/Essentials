package essentials.net;

import arc.Core;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import mindustry.core.GameState;
import mindustry.core.Version;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.net.Administration;
import mindustry.type.Item;
import mindustry.type.ItemType;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import static essentials.Global.*;
import static essentials.Main.config;
import static essentials.Threads.playtime;
import static essentials.Threads.uptime;
import static essentials.core.Log.writeLog;
import static essentials.core.PlayerDB.conn;
import static essentials.core.PlayerDB.getRaw;
import static mindustry.Vars.*;

public class Server implements Runnable {
    public static ServerSocket serverSocket;
    public static ArrayList<Service> list = new ArrayList<>();
    private String remoteip;

    enum Request{
        ping, bansync, chat, exit, unbanip, unbanid, datashare, checkban
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(config.getServerport());
        } catch (IOException e) {
            printError(e);
            return;
        }
        log(LogType.server,"server-enabled");

        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                Service service = new Service(socket);
                service.start();
                list.add(service);
            } catch (SocketException s){
                return;
            } catch (Exception e) {
                printError(e);
            }
        }
    }

    public class Service extends Thread {
        public BufferedReader in;
        public DataOutputStream os;
        public Socket socket;
        public SecretKeySpec spec;
        public Cipher cipher;
        public String ip;

        public Service(Socket socket) {
            try {
                this.socket = socket;
                ip = socket.getInetAddress().toString();
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                // 키 값 읽기
                String authkey = in.readLine();
                if(authkey.matches("GET /.* HTTP/.*")){
                    writeLog(LogType.web, authkey);
                    writeLog(LogType.web, "Remote IP: " + remoteip);
                    String headerLine;
                    while ((headerLine = in.readLine()).length() != 0) {
                        writeLog(LogType.web, headerLine);
                    }
                    writeLog(LogType.web, "========================");
                    StringBuilder payload = new StringBuilder();
                    while (in.ready()) {
                        payload.append((char) in.read());
                    }

                    if(authkey.matches("POST /rank HTTP/.*") && payload.toString().split("\\|\\|\\|").length != 2){
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                        bw.write("Login failed!\n");
                        bw.flush();
                        bw.close();
                        os.close();
                        in.close();
                        socket.close();
                        list.remove(this);
                        log(LogType.server,"client-disconnected-http", remoteip);
                    } else {
                        httpserver(authkey, payload.toString());
                    }
                    return;
                }

                spec = new SecretKeySpec(Base64.decode(authkey), "AES");
                cipher = Cipher.getInstance("AES");

                os = new DataOutputStream(socket.getOutputStream());
            } catch (Exception e) {
                if(e.getMessage().equals("socket closed")){
                    return;
                }
                printError(e);
            }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.currentThread().setName(remoteip+" Client Thread");
                try {
                    remoteip = socket.getInetAddress().toString().replace("/", "");
                    String value = new String(decrypt(Base64.decode(in.readLine()), spec, cipher));

                    try {
                        JsonObject answer = new JsonObject();
                        JsonObject data = JsonValue.readJSON(value).asObject();
                        Request type = Request.valueOf(data.get("type").asString());
                        switch (type) {
                            case ping:
                                String[] msg = {"Hi " + remoteip + "! Your connection is successful!", "Hello " + remoteip + "! I'm server!", "Welcome to the server " + remoteip + "!"};
                                int rnd = new Random().nextInt(msg.length);
                                answer.add("result", msg[rnd]);
                                os.writeBytes(Base64.encode(encrypt(answer.toString(), spec, cipher)) + "\n");
                                os.flush();
                                log(LogType.server, "client-connected", remoteip);
                                break;
                            case bansync:
                                log(LogType.server, "client-request-banlist", remoteip);

                                // 적용
                                JsonArray ban = data.get("ban").asArray();
                                JsonArray ipban = data.get("ipban").asArray();
                                JsonArray subban = data.get("subban").asArray();

                                for (JsonValue b : ban) {
                                    netServer.admins.banPlayerID(b.asString());
                                }

                                for (JsonValue b : ipban) {
                                    netServer.admins.banPlayerIP(b.asString());
                                }

                                for (JsonValue b : subban) {
                                    netServer.admins.addSubnetBan(b.asString());
                                }

                                // 가져오기
                                JsonArray bans = new JsonArray();
                                JsonArray ipbans = new JsonArray();
                                JsonArray subbans = new JsonArray();

                                for (Administration.PlayerInfo b : netServer.admins.getBanned()) {
                                    bans.add(b.id);
                                }

                                for (String b : netServer.admins.getBannedIPs()) {
                                    ipbans.add(b);
                                }

                                for (String b : netServer.admins.getSubnetBans()) {
                                    subbans.add(b);
                                }

                                answer.add("type", "bansync");
                                answer.add("ban", ban);
                                answer.add("ipban", ipban);
                                answer.add("subban", subban);

                                for (Service ser : list) {
                                    String remoteip = ser.socket.getInetAddress().toString().replace("/", "");
                                    for (JsonValue b : config.getBantrust()) {
                                        if (b.asString().equals(remoteip)) {
                                            ser.os.writeBytes(Base64.encode(encrypt(answer.toString(), ser.spec, ser.cipher)) + "\n");
                                            ser.os.flush();
                                            log(LogType.server, "server-data-sented", ser.socket.getInetAddress().toString());
                                        }
                                    }
                                }
                                break;
                            case chat:
                                String message = data.get("message").asString();
                                for (Player p : playerGroup) {
                                    p.sendMessage(p.isAdmin ? "[#C77E36][" + remoteip + "][RC] " + message : "[#C77E36][RC] " + message);
                                }

                                for (Service ser : list) {
                                    if (ser.spec != spec) {
                                        ser.os.writeBytes(Base64.encode(encrypt(value, ser.spec, ser.cipher)) + "\n");
                                        ser.os.flush();
                                    }
                                }

                                log(LogType.server, "server-message-received", remoteip, message);
                                break;
                            case exit:
                                os.close();
                                in.close();
                                socket.close();
                                list.remove(this);
                                log(LogType.server, "client-disconnected", remoteip, nbundle(locale, "client-disconnected-reason-exit"));
                                this.interrupt();
                                return;
                            case unbanip:
                                netServer.admins.unbanPlayerIP(data.get("ip").asString());
                                // TODO make success message
                                break;
                            case unbanid:
                                netServer.admins.unbanPlayerID(data.get("uuid").asString());
                                // TODO make success message
                                break;
                            case datashare:
                                // TODO make datashare
                                break;
                            case checkban:
                                boolean found = false;
                                String target_uuid = data.get("target_uuid").asString();
                                String target_ip = data.get("target_ip").asString();

                                for (Administration.PlayerInfo info : netServer.admins.getBanned()) {
                                    if (info.id.equals(target_uuid)) {
                                        found = true;
                                        break;
                                    }
                                }

                                for (String info : netServer.admins.getBannedIPs()) {
                                    if (info.equals(target_ip)) {
                                        found = true;
                                        break;
                                    }
                                }
                                answer.add("result", found ? "true" : "false");
                                os.writeBytes(Base64.encode(encrypt(answer.toString(), spec, cipher)) + "\n");
                                os.flush();
                                break;
                        }
                    } catch (ParseException e) {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    log(LogType.server,"client-disconnected", remoteip, nbundle(locale, "client-disconnected-reason-error"));
                    try {
                        os.close();
                        in.close();
                        socket.close();
                        list.remove(this);
                    } catch (IOException ex) {
                        printError(ex);
                    }
                    return;
                }
            }
        }

        private String query() {
            JsonObject result = new JsonObject();
            result.add("players", playerGroup.size()); // 플레이어 인원
            result.add("version", Version.build); // 버전
            result.add("plugin-version", plugin_version);
            result.add("playtime", playtime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            result.add("name", Core.settings.getString("servername"));
            result.add("mapname", world.getMap().name());
            result.add("wave", state.wave);
            result.add("enemy-count", state.enemies);

            boolean online = false;
            for(int a=0;a<playerGroup.size();a++){
                Player target = playerGroup.all().get(a);
                if(target.isAdmin){
                    online = true;
                }
            }
            result.add("admin_online", online);

            JsonArray array = new JsonArray();
            for (Player p : playerGroup.all()) {
                array.add(p.name); // player list
            }
            result.add("playerlist",array);

            JsonObject items = new JsonObject();
            for (Item item : content.items()) {
                if (item.type == ItemType.material) {
                    items.add(item.name, state.teams.get(Team.sharded).cores.first().items.get(item)); // resources
                }
            }
            result.add("resource",items);

            JsonObject rank = new JsonObject();
            try{
                String[] list = new String[]{"placecount", "breakcount", "killcount", "joincount", "kickcount", "exp", "playtime", "pvpwincount", "reactorcount"};
                Statement stmt = conn.createStatement();
                for (String s : list) {
                    ResultSet rs = stmt.executeQuery("SELECT " + s + ",name FROM players ORDER BY `" + s + "`");
                    while (rs.next()) {
                        rank.add(rs.getString("name"), rs.getString(s));
                    }
                }
            } catch (SQLException e) {
                printError(e);
            }

            return result.toString();
        }

        private String serverinfo() {
            if (state.is(GameState.State.playing)) {
                int playercount = playerGroup.size();
                StringBuilder playerdata = new StringBuilder();
                for (Player p : playerGroup.all()) {
                    playerdata.append(p.name).append(",");
                }
                if (playerdata.length() != 0) {
                    playerdata.substring(playerdata.length() - 1, playerdata.length());
                }
                int version = Version.build;
                String description = Core.settings.getString("servername");
                String worldtime = playtime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String serveruptime = uptime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                StringBuilder items = new StringBuilder();
                for (Item item : content.items()) {
                    if (item.type == ItemType.material) {
                        items.append(item.name).append(": ").append(state.teams.get(Team.sharded).cores.first().items.get(item)).append("<br>");
                    }
                }
                String coreitem = items.toString();

                return "Player count: " + playercount + "<br>" +
                        "Player list: " + playerdata + "<br>" +
                        "Version: " + version + "<br>" +
                        "Description: " + description + "<br>" +
                        "World playtime: " + worldtime + "<br>" +
                        "Server uptime: " + serveruptime + "<br>" +
                        "Core items<br>" + coreitem;
            } else {
                return "Server isn't hosted!";
            }
        }

        private String rankingdata() throws IOException {
            ArrayList<String> lists = new ArrayList<>(Arrays.asList("placecount","breakcount","killcount","joincount","kickcount","exp","playtime","pvpwincount","reactorcount","attackclear"));
            JsonObject results = new JsonObject();

            Locale language = geolocation(remoteip);

            String[] sql = new String[10];
            sql[0] = "SELECT * FROM players ORDER BY `placecount` DESC LIMIT 10";
            sql[1] = "SELECT * FROM players ORDER BY `breakcount` DESC LIMIT 10";
            sql[2] = "SELECT * FROM players ORDER BY `killcount` DESC LIMIT 10";
            sql[3] = "SELECT * FROM players ORDER BY `joincount` DESC LIMIT 10";
            sql[4] = "SELECT * FROM players ORDER BY `kickcount` DESC LIMIT 10";
            sql[5] = "SELECT * FROM players ORDER BY `exp` DESC LIMIT 10";
            sql[6] = "SELECT * FROM players ORDER BY `playtime` DESC LIMIT 10";
            sql[7] = "SELECT * FROM players ORDER BY `pvpwincount` DESC LIMIT 10";
            sql[8] = "SELECT * FROM players ORDER BY `reactorcount` DESC LIMIT 10";
            sql[9] = "SELECT * FROM players ORDER BY `attackclear` DESC LIMIT 10";

            try {
                Statement stmt = conn.createStatement();
                String name = nbundle(language,"server-http-rank-name");
                String country = nbundle(language,"server-http-rank-country");
                String win = nbundle(language,"server-http-rank-pvp-win");
                String lose = nbundle(language,"server-http-rank-pvp-lose");
                String rate = nbundle(language,"server-http-rank-pvp-rate");

                for(int a=0;a<sql.length;a++){
                    ResultSet rs = stmt.executeQuery(sql[a]);
                    JsonArray array = new JsonArray();
                    if(lists.get(a).equals("pvpwincount")){
                        String header = "<tr><th>"+name+"</th><th>"+country+"</th><th>"+win+"</th><th>"+lose+"</th><th>"+rate+"</th></tr>";
                        array.add(header);
                        while(rs.next()){
                            int percent;
                            try {
                                percent = rs.getInt("pvpwincount") / rs.getInt("pvplosecount") * 100;
                            } catch (Exception e) {
                                percent = 0;
                            }
                            String data = "<tr><td>"+rs.getString("name")+"</td><td>"+rs.getString("country")+"</td><td>"+rs.getInt("pvpwincount")+"</td><td>"+rs.getInt("pvplosecount")+"</td><td>"+percent+"%</td></tr>\n";
                            array.add(data);
                        }
                    } else {
                        String header = "<tr><th>"+name+"</th><th>"+country+"</th><th>"+lists.get(a)+"</th></tr>";
                        array.add(header);
                        while (rs.next()) {
                            String data = "<tr><td>" + rs.getString("name") + "</td><td>" + rs.getString("country") + "</td><td>" + rs.getString(lists.get(a)) + "</td></tr>\n";
                            array.add(data);
                        }
                    }
                    results.add(lists.get(a),array);
                    rs.close();
                }
                stmt.close();
            } catch (Exception e) {
                printError(e);
            }


            InputStream reader = getClass().getResourceAsStream("/HTML/rank.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(reader, StandardCharsets.UTF_8));

            String line;
            StringBuilder result = new StringBuilder();
            while ((line = br.readLine()) != null) {
                result.append(line).append("\n");
            }
            Document doc = Jsoup.parse(result.toString());
            for (String s : lists) {
                for (int b = 0; b < results.get(s).asArray().size(); b++) {
                    doc.getElementById(s).append(results.get(s).asArray().get(b).asString());
                }
            }

            doc.getElementById("info_body").appendText(serverinfo());
            doc.getElementById("rank-placecount").appendText(nbundle(language,"server-http-rank-placecount"));
            doc.getElementById("rank-breakcount").appendText(nbundle(language,"server-http-rank-breakcount"));
            doc.getElementById("rank-killcount").appendText(nbundle(language,"server-http-rank-killcount"));
            doc.getElementById("rank-joincount").appendText(nbundle(language,"server-http-rank-joincount"));
            doc.getElementById("rank-kickcount").appendText(nbundle(language,"server-http-rank-kickcount"));
            doc.getElementById("rank-exp").appendText(nbundle(language,"server-http-rank-exp"));
            doc.getElementById("rank-playtime").appendText(nbundle(language,"server-http-rank-playtime"));
            doc.getElementById("rank-pvpwincount").appendText(nbundle(language,"server-http-rank-pvpcount"));
            doc.getElementById("rank-reactorcount").appendText(nbundle(language,"server-http-rank-reactorcount"));
            doc.getElementById("rank-attackclear").appendText(nbundle(language,"server-http-rank-attackclear"));

            return doc.toString();
        }

        private void httpserver(String receive, String payload){
            try {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd a hh:mm.ss", Locale.ENGLISH);
                String time = now.format(dateTimeFormatter);

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                if (config.isQuery() && state.is(GameState.State.playing)) {
                    if (receive.matches("GET / HTTP/.*")) {
                        String data = query();
                        bw.write("HTTP/1.1 200 OK\r\n");
                        bw.write("Date: " + time + "\r\n");
                        bw.write("Server: Mindustry/Essentials "+ plugin_version +"\r\n");
                        bw.write("Content-Type: application/json; charset=utf-8\r\n");
                        bw.write("Content-Length: " + data.getBytes().length + 1 + "\r\n");
                        bw.write("\r\n");
                        bw.write(query());
                    } else if (receive.matches("GET /rank HTTP/.*") || receive.matches("GET /rank# HTTP/.*")) {
                        String rank = rankingdata();
                        bw.write("HTTP/1.1 200 OK\r\n");
                        bw.write("Date: " + time + "\r\n");
                        bw.write("Server: Mindustry/Essentials "+ plugin_version +"\r\n");
                        bw.write("Content-Type: text/html; charset=utf-8\r\n");
                        bw.write("Content-Length: " + rank.getBytes().length + 1 + "\r\n");
                        bw.write("\r\n");
                        bw.write(rank);
                    } else if (receive.matches("POST /rank HTTP/.*")) {
                        String[] value = payload.split("\\|\\|\\|");
                        String id = value[0].replace("id=", "");
                        String pw = value[1].replace("pw=", "");

                        PreparedStatement pstm = conn.prepareStatement("SELECT * FROM players WHERE accountid = ?");
                        pstm.setString(1, id);
                        ResultSet rs = pstm.executeQuery();
                        if (rs.next()) {
                            if (BCrypt.checkpw(pw, rs.getString("accountpw"))) {
                                JsonObject db = getRaw(rs.getString("uuid"));
                                String language = db.getString("language",null);

                                String[] ranking = new String[12];
                                ranking[0] = "SELECT uuid, placecount, RANK() over (ORDER BY placecount desc) valrank FROM players";
                                ranking[1] = "SELECT uuid, breakcount, RANK() over (ORDER BY breakcount desc) valrank FROM players";
                                ranking[2] = "SELECT uuid, killcount, RANK() over (ORDER BY killcount desc) valrank FROM players";
                                ranking[3] = "SELECT uuid, deathcount, RANK() over (ORDER BY deathcount desc) valrank FROM players";
                                ranking[4] = "SELECT uuid, joincount, RANK() over (ORDER BY joincount desc) valrank FROM players";
                                ranking[5] = "SELECT uuid, kickcount, RANK() over (ORDER BY kickcount desc) valrank FROM players";
                                ranking[6] = "SELECT uuid, level, RANK() over (ORDER BY level desc) valrank FROM players";
                                ranking[7] = "SELECT uuid, playtime, RANK() over (ORDER BY playtime desc) valrank FROM players";
                                ranking[8] = "SELECT uuid, attackclear, RANK() over (ORDER BY attackclear desc) valrank FROM players";
                                ranking[9] = "SELECT uuid, pvpwincount, RANK() over (ORDER BY pvpwincount desc) valrank FROM players";
                                ranking[10] = "SELECT uuid, pvplosecount, RANK() over (ORDER BY pvplosecount desc) valrank FROM players";
                                ranking[11] = "SELECT uuid, pvpbreakout, RANK() over (ORDER BY pvpbreakout desc) valrank FROM players";

                                String datatext;
                                if (!config.isInternalDB()) {
                                    Statement stmt = conn.createStatement();
                                    ArrayList<String> array = new ArrayList<>();
                                    for (String s : ranking) {
                                        ResultSet rs1 = stmt.executeQuery(s);
                                        while (rs1.next()) {
                                            if (rs1.getString("uuid").equals(db.getString("uuid",null))) {
                                                array.add(rs1.getString("valrank"));
                                                break;
                                            }
                                        }
                                        rs1.close();
                                    }
                                    stmt.close();

                                    datatext = nbundle(language, "player-info") + "<br>" +
                                            "========================================<br>" +
                                            nbundle(language, "player-name") + ": " + rs.getString("name") + "<br>" +
                                            nbundle(language, "player-uuid") + ": " + rs.getString("uuid") + "<br>" +
                                            nbundle(language, "player-country") + ": " + db.get("country") + "<br>" +
                                            nbundle(language, "player-placecount") + ": " + db.get("placecount") + " - <b>#" + array.get(0) + "</b><br>" +
                                            nbundle(language, "player-breakcount") + ": " + db.get("breakcount") + " - <b>#" + array.get(1) + "</b><br>" +
                                            nbundle(language, "player-killcount") + ": " + db.get("killcount") + " - <b>#" + array.get(2) + "</b><br>" +
                                            nbundle(language, "player-deathcount") + ": " + db.get("deathcount") + " - <b>#" + array.get(3) + "</b><br>" +
                                            nbundle(language, "player-joincount") + ": " + db.get("joincount") + " - <b>#" + array.get(4) + "</b><br>" +
                                            nbundle(language, "player-kickcount") + ": " + db.get("kickcount") + " - <b>#" + array.get(5) + "</b><br>" +
                                            nbundle(language, "player-level") + ": " + db.get("level") + " - <b>#" + array.get(6) + "</b><br>" +
                                            nbundle(language, "player-reqtotalexp") + ": " + db.get("reqtotalexp") + "<br>" +
                                            nbundle(language, "player-firstdate") + ": " + db.get("firstdate") + "<br>" +
                                            nbundle(language, "player-lastdate") + ": " + db.get("lastdate") + "<br>" +
                                            nbundle(language, "player-playtime") + ": " + db.get("playtime") + " - <b>#" + array.get(7) + "</b><br>" +
                                            nbundle(language, "player-attackclear") + ": " + db.get("attackclear") + " - <b>#" + array.get(8) + "</b><br>" +
                                            nbundle(language, "player-pvpwincount") + ": " + db.get("pvpwincount") + " - <b>#" + array.get(9) + "</b><br>" +
                                            nbundle(language, "player-pvplosecount") + ": " + db.get("pvplosecount") + " - <b>#" + array.get(10) + "</b><br>" +
                                            nbundle(language, "player-pvpbreakout") + ": " + db.get("pvpbreakout") + " - <b>#" + array.get(11) + "</b><br>";
                                } else {
                                    datatext = nbundle(language, "player-info") + "<br>" +
                                            "========================================<br>" +
                                            nbundle(language, "player-name") + ": " + rs.getString("name") + "<br>" +
                                            nbundle(language, "player-uuid") + ": " + rs.getString("uuid") + "<br>" +
                                            nbundle(language, "player-country") + ": " + db.get("country") + "<br>" +
                                            nbundle(language, "player-placecount") + ": " + db.get("placecount") + "<br>" +
                                            nbundle(language, "player-breakcount") + ": " + db.get("breakcount") + "<br>" +
                                            nbundle(language, "player-killcount") + ": " + db.get("killcount") + "<br>" +
                                            nbundle(language, "player-deathcount") + ": " + db.get("deathcount") + "<br>" +
                                            nbundle(language, "player-joincount") + ": " + db.get("joincount") + "<br>" +
                                            nbundle(language, "player-kickcount") + ": " + db.get("kickcount") + "<br>" +
                                            nbundle(language, "player-level") + ": " + db.get("level") + "<br>" +
                                            nbundle(language, "player-reqtotalexp") + ": " + db.get("reqtotalexp") + "<br>" +
                                            nbundle(language, "player-firstdate") + ": " + db.get("firstdate") + "<br>" +
                                            nbundle(language, "player-lastdate") + ": " + db.get("lastdate") + "<br>" +
                                            nbundle(language, "player-playtime") + ": " + db.get("playtime") + "<br>" +
                                            nbundle(language, "player-attackclear") + ": " + db.get("attackclear") + "<br>" +
                                            nbundle(language, "player-pvpwincount") + ": " + db.get("pvpwincount") + "<br>" +
                                            nbundle(language, "player-pvplosecount") + ": " + db.get("pvplosecount") + "<br>" +
                                            nbundle(language, "player-pvpbreakout") + ": " + db.get("pvpbreakout");
                                }
                                bw.write(datatext);
                            } else {
                                bw.write("Login failed!");
                            }
                        } else {
                            bw.write("Login failed!");
                        }
                        rs.close();
                        pstm.close();
                    } else {
                        InputStream reader = getClass().getResourceAsStream("/HTML/404.html");
                        BufferedReader br = new BufferedReader(new InputStreamReader(reader, StandardCharsets.UTF_8));

                        String line;
                        StringBuilder result = new StringBuilder();
                        while ((line = br.readLine()) != null) {
                            result.append(line).append("\n");
                        }

                        int rand = (int) (Math.random() * 2);
                        InputStream image;
                        if (rand == 0) {
                            image = getClass().getResourceAsStream("/HTML/404_Error.gif");
                        } else {
                            image = getClass().getResourceAsStream("/HTML/404.webp");
                        }
                        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();

                        int len;
                        byte[] buf = new byte[1024];
                        while ((len = image.read(buf)) != -1) {
                            byteOutStream.write(buf, 0, len);
                        }

                        byte[] fileArray = byteOutStream.toByteArray();
                        String changeString;
                        if (rand == 0) {
                            changeString = "data:image/gif;base64," + Base64.encode(fileArray);
                        } else {
                            changeString = "data:image/webp;base64," + Base64.encode(fileArray);
                        }
                        Document doc = Jsoup.parse(result.toString());
                        doc.getElementById("box").append("<img src=" + changeString + " alt=\"\">");

                        bw.write("HTTP/1.1 404 Internal error\r\n");
                        bw.write("Date: " + time + "\r\n");
                        bw.write("Server: Mindustry/Essentials 7.0\r\n");
                        bw.write("\r\n");
                        bw.write(doc.toString());
                        nlog(LogType.warn,receive);
                    }
                } else {
                    bw.write("HTTP/1.1 403 Forbidden\r\n");
                    bw.write("Date: " + time + "\r\n");
                    bw.write("Server: Mindustry/Essentials 7.0\r\n");
                    bw.write("Content-Encoding: gzip");
                    bw.write("\r\n");
                    bw.write("<TITLE>403 Forbidden</TITLE>");
                    bw.write("<p>This server isn't allowed query!</p>");
                }
                bw.flush();
                bw.close();
                os.close();
                in.close();
                socket.close();
                list.remove(this);
                log(LogType.server,"client-disconnected-http", remoteip);
            }catch (Exception e){
                printError(e);
                try{
                    os.close();
                    in.close();
                    socket.close();
                    list.remove(this);
                }catch (Exception ignored){}
            }
        }
    }
}