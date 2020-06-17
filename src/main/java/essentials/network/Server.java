package essentials.network;

import arc.Core;
import arc.struct.Seq;
import essentials.core.player.PlayerData;
import essentials.internal.Bundle;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.core.GameState;
import mindustry.core.Version;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Playerc;
import mindustry.net.Administration;
import mindustry.type.Item;
import mindustry.type.ItemType;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

import static essentials.Main.*;
import static mindustry.Vars.*;
import static org.hjson.JsonValue.readJSON;

public class Server implements Runnable {
    public Seq<service> list = new Seq<>();
    public ServerSocket serverSocket;

    Bundle bundle = new Bundle();

    public void shutdown() {
        try {
            if (serverSocket != null) {
                Thread.currentThread().interrupt();
                serverSocket.close();
            }
        } catch (IOException e) {
            new CrashReport(e);
        }
    }

    @Override
    public void run() {
        try {
            this.serverSocket = new ServerSocket(config.serverPort());
            Log.info("server.enabled");
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                service service = new service(socket);
                service.start();
                list.add(service);
            }
        } catch (IOException e) {
            if (!e.getMessage().equalsIgnoreCase("socket closed")) new CrashReport(e);
            Thread.currentThread().interrupt();
        }
    }

    enum Request {
        ping, bansync, chat, exit, unbanip, unbanid, datashare, checkban
    }

    public class service extends Thread {
        public BufferedReader in;
        public DataOutputStream os;
        public Socket socket;
        public SecretKey spec;
        public Cipher cipher;
        public String ip;

        public void shutdown(String bundle, Object... parameter) {
            try {
                os.close();
                in.close();
                socket.close();
                list.remove(this);
                if (bundle != null) Log.server(bundle, parameter);
            } catch (Exception ignored) {
            }
        }

        public service(Socket socket) throws IOException {
            this.socket = socket;
            ip = socket.getInetAddress().toString();
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            os = new DataOutputStream(socket.getOutputStream());

            // 키 값 읽기
            String authkey = in.readLine();
            if (authkey == null) throw new IOException("Auth key is null");
            if (Pattern.matches(".*HTTP/.*", authkey)) {
                StringBuilder headers = new StringBuilder();
                headers.append(authkey).append("\n");
                headers.append("Remote IP: ").append(ip).append("\n");

                String headerLine;
                while ((headerLine = in.readLine()).length() != 0) {
                    headers.append(headerLine).append("\n");
                }

                headers.append("========================");
                Log.write(Log.LogType.web, headers.toString());

                StringBuilder payload = new StringBuilder();
                while (in.ready()) {
                    payload.append((char) in.read());
                }

                if (authkey.matches("POST /rank HTTP/.*") && payload.toString().split(";").length != 2) {
                    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                        bw.write("Login failed!\n");
                        bw.flush();
                    } finally {
                        shutdown("client.disconnected.http", ip);
                    }
                } else if (config.query()) {
                    httpserver(authkey, payload.toString());
                } else if (!config.query()) {
                    try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                        bw.write("HTTP/1.1 403 Forbidden\r\n");
                        bw.write("Date: " + tool.getTime() + "\r\n");
                        bw.write("Server: Mindustry/Essentials " + vars.pluginVersion() + "\r\n");
                        bw.write("Content-Encoding: gzip");
                        bw.write("\r\n");
                        bw.write("<TITLE>403 Forbidden</TITLE>");
                        bw.write("<p>This server isn't allowed query!</p>");
                    } finally {
                        shutdown("client.disconnected.http", ip);
                    }
                }
            } else {
                spec = new SecretKeySpec(Base64.getDecoder().decode(authkey), "AES");
            }
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().setName(ip + " Client Thread");
                    ip = socket.getInetAddress().toString().replace("/", "");

                    String value = tool.decrypt(in.readLine(), spec);
                    JsonObject answer = new JsonObject();
                    JsonObject data = readJSON(value).asObject();
                    Request type = Request.valueOf(data.get("type").asString());
                    switch (type) {
                        case ping:
                            String[] msg = {"Hi " + ip + "! Your connection is successful!", "Hello " + ip + "! I'm server!", "Welcome to the server " + ip + "!"};
                            int rnd = new SecureRandom().nextInt(msg.length);
                            answer.add("result", msg[rnd]);
                            os.writeBytes(tool.encrypt(answer.toString(), spec) + "\n");
                            os.flush();
                            Log.server("client.connected", ip);
                            break;
                        case bansync:
                            Log.server("client.request.banlist", ip);

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

                            for (service ser : list) {
                                String remoteip = ser.socket.getInetAddress().toString().replace("/", "");
                                for (JsonValue b : config.banTrust()) {
                                    if (b.asString().equals(remoteip)) {
                                        ser.os.writeBytes(tool.encrypt(answer.toString(), ser.spec) + "\n");
                                        ser.os.flush();
                                        Log.server("server.data-sented", ser.socket.getInetAddress().toString());
                                    }
                                }
                            }
                            break;
                        case chat:
                            String message = data.get("message").asString();
                            for (Playerc p : Groups.player) {
                                p.sendMessage(p.admin() ? "[#C77E36][" + ip + "][RC] " + message : "[#C77E36][RC] " + message);
                            }

                            for (service ser : list) {
                                if (ser.spec != spec) {
                                    ser.os.writeBytes(tool.encrypt(value, ser.spec) + "\n");
                                    ser.os.flush();
                                }
                            }

                            Log.server("server-message-received", ip, message);
                            break;
                        case exit:
                            shutdown("client.disconnected", ip, bundle.get("client.disconnected.reason.exit"));
                            this.interrupt();
                            return;
                        case unbanip:
                            netServer.admins.unbanPlayerIP(data.get("ip").asString());
                            // TODO 성공 메세지 만들기
                            break;
                        case unbanid:
                            netServer.admins.unbanPlayerID(data.get("uuid").asString());
                            // TODO 성공 메세지 만들기
                            break;
                        case datashare:
                            // TODO 데이터 공유 만들기
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
                            os.writeBytes(tool.encrypt(answer.toString(), spec) + "\n");
                            os.flush();
                            break;
                    }
                }
                shutdown(null);
            } catch (IOException e) {
                if (!e.getMessage().equals("Stream closed")) new CrashReport(e);
            } catch (Exception e) {
                Log.server("client.disconnected", ip, bundle.get("client.disconnected.reason.error"));
            }
        }

        private String query() {
            JsonObject result = new JsonObject();
            result.add("players", Groups.player.size()); // 플레이어 인원
            result.add("version", Version.build); // 버전
            result.add("plugin-version", vars.pluginVersion());
            result.add("playtime", tool.secToTime(vars.playtime()));
            result.add("name", Core.settings.getString("servername"));
            result.add("mapname", state.map.name());
            result.add("wave", state.wave);
            result.add("enemy-count", state.enemies);

            boolean online = false;
            for (Playerc p : Groups.player) {
                if (p.admin()) {
                    online = true;
                    break;
                }
            }
            result.add("admin_online", online);

            JsonArray array = new JsonArray();
            for (Playerc p : Groups.player) {
                array.add(p.name()); // player list
            }
            result.add("playerlist", array);

            JsonObject items = new JsonObject();
            for (Item item : content.items()) {
                if (item.type == ItemType.material) {
                    items.add(item.name, state.teams.get(Team.sharded).cores.first().items.get(item)); // resources
                }
            }
            result.add("resource", items);

            JsonObject rank = new JsonObject();
            String[] list = new String[]{"placecount", "breakcount", "killcount", "joincount", "kickcount", "exp", "playtime", "pvpwincount", "reactorcount"};

            for (String s : list) {
                String sql = "SELECT " + s + ",name FROM players ORDER BY `" + s + "`";
                try (PreparedStatement pstmt = database.conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        rank.add(rs.getString("name"), rs.getString(s));
                    }
                } catch (SQLException e) {
                    new CrashReport(e);
                }
            }

            return result.toString();
        }

        private String serverinfo() {
            if (state.is(GameState.State.playing)) {
                int playercount = Groups.player.size();
                StringBuilder playerdata = new StringBuilder();
                for (Playerc p : Groups.player) {
                    playerdata.append(p.name()).append(",");
                }
                if (playerdata.length() != 0) {
                    playerdata.substring(playerdata.length() - 1, playerdata.length());
                }
                int version = Version.build;
                String description = Core.settings.getString("servername");
                String worldtime = tool.secToTime(vars.playtime());
                String serveruptime = tool.secToTime(vars.uptime());
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
            String[] lists = new String[]{"placecount", "breakcount", "killcount", "joincount", "kickcount", "exp", "playtime", "pvpwincount", "reactorcount", "attackclear"};
            JsonObject results = new JsonObject();

            Locale language = tool.getGeo(ip);

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

            Bundle bundle = new Bundle(language);
            String name = bundle.get("server.http.rank.name");
            String country = bundle.get("server.http.rank.country");
            String win = bundle.get("server.http.rank.pvp-win");
            String lose = bundle.get("server.http.rank.pvp-lose");
            String rate = bundle.get("server.http.rank.pvp-rate");

            Statement stmt = null;
            ResultSet rs = null;
            try {
                stmt = database.conn.createStatement();
                for (int a = 0; a < sql.length; a++) {
                    rs = stmt.executeQuery(sql[a]);
                    JsonArray array = new JsonArray();
                    if (lists[a].equals("pvpwincount")) {
                        String header = "<tr><th>" + name + "</th><th>" + country + "</th><th>" + win + "</th><th>" + lose + "</th><th>" + rate + "</th></tr>";
                        array.add(header);
                        while (rs.next()) {
                            int percent;
                            try {
                                percent = rs.getInt("pvpwincount") / rs.getInt("pvplosecount") * 100;
                            } catch (Exception e) {
                                percent = 0;
                            }
                            String data = "<tr><td>" + rs.getString("name") + "</td><td>" + rs.getString("country") + "</td><td>" + rs.getInt("pvpwincount") + "</td><td>" + rs.getInt("pvplosecount") + "</td><td>" + percent + "%</td></tr>\n";
                            array.add(data);
                        }
                    } else {
                        String header = "<tr><th>" + name + "</th><th>" + country + "</th><th>" + lists[a] + "</th></tr>";
                        array.add(header);
                        while (rs.next()) {
                            String data = "<tr><td>" + rs.getString("name") + "</td><td>" + rs.getString("country") + "</td><td>" + rs.getString(lists[a]) + "</td></tr>\n";
                            array.add(data);
                        }
                    }
                    results.add(lists[a], array);
                }
            } catch (SQLException e) {
                new CrashReport(e);
            } finally {
                if (stmt != null) try {
                    stmt.close();
                } catch (SQLException ignored) {
                }
                if (rs != null) try {
                    rs.close();
                } catch (SQLException ignored) {
                }
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
            doc.getElementById("rank-placecount").appendText(bundle.get("server.http.rank.placecount"));
            doc.getElementById("rank-breakcount").appendText(bundle.get("server.http.rank.breakcount"));
            doc.getElementById("rank-killcount").appendText(bundle.get("server.http.rank.killcount"));
            doc.getElementById("rank-joincount").appendText(bundle.get("server.http.rank.joincount"));
            doc.getElementById("rank-kickcount").appendText(bundle.get("server.http.rank.kickcount"));
            doc.getElementById("rank-exp").appendText(bundle.get("server.http.rank.exp"));
            doc.getElementById("rank-playtime").appendText(bundle.get("server.http.rank.playtime"));
            doc.getElementById("rank-pvpwincount").appendText(bundle.get("server.http.rank.pvpcount"));
            doc.getElementById("rank-reactorcount").appendText(bundle.get("server.http.rank.reactorcount"));
            doc.getElementById("rank-attackclear").appendText(bundle.get("server.http.rank.attackclear"));

            return doc.toString();
        }

        private void httpserver(String receive, String payload) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd a HH:mm:ss", Locale.ENGLISH);
            String time = now.format(dateTimeFormatter);

            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                if (receive.matches("GET / HTTP/.*")) {
                    String data = query();
                    bw.write("HTTP/1.1 200 OK\r\n");
                    bw.write("Date: " + time + "\r\n");
                    bw.write("Server: Mindustry/Essentials " + vars.pluginVersion() + "\r\n");
                    bw.write("Content-Type: application/json; charset=utf-8\r\n");
                    bw.write("Content-Length: " + data.getBytes().length + 1 + "\r\n");
                    bw.write("\r\n");
                    bw.write(query());
                } else if (receive.matches("GET /rank HTTP/.*") || receive.matches("GET /rank# HTTP/.*")) {
                    String rank = rankingdata();
                    bw.write("HTTP/1.1 200 OK\r\n");
                    bw.write("Date: " + time + "\r\n");
                    bw.write("Server: Mindustry/Essentials " + vars.pluginVersion() + "\r\n");
                    bw.write("Content-Type: text/html; charset=utf-8\r\n");
                    bw.write("Content-Length: " + rank.getBytes().length + 1 + "\r\n");
                    bw.write("\r\n");
                    bw.write(rank);
                } else if (receive.matches("POST /rank HTTP/.*")) {
                    String[] value = payload.split(";");
                    String id = value[0].replace("id=", "");
                    String pw = value[1].replace("pw=", "");

                    try (PreparedStatement pstm = database.conn.prepareStatement("SELECT * FROM players WHERE accountid = ?")) {
                        pstm.setString(1, id);
                        try (ResultSet rs = pstm.executeQuery()) {
                            if (rs.next()) {
                                String accountpw = rs.getString("accountpw");
                                if (pw.equals(accountpw) || BCrypt.checkpw(pw, accountpw)) {
                                    PlayerData db = playerDB.load(rs.getString("uuid"));

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
                                    Seq<String> array = new Seq<>();
                                    for (String s : ranking) {
                                        try (PreparedStatement pstmt = database.conn.prepareStatement(s);
                                             ResultSet rs1 = pstmt.executeQuery()) {
                                            while (rs1.next()) {
                                                if (rs1.getString("uuid").equals(db.uuid())) {
                                                    array.add(rs1.getString("valrank"));
                                                    break;
                                                }
                                            }
                                        } catch (SQLException e) {
                                            new CrashReport(e);
                                        }
                                    }

                                    datatext = bundle.get("player.info") + "<br>" +
                                            "========================================<br>" +
                                            bundle.get("player.name") + ": " + rs.getString("name") + "<br>" +
                                            bundle.get("player.uuid") + ": " + rs.getString("uuid") + "<br>" +
                                            bundle.get("player.country") + ": " + db.country() + "<br>" +
                                            bundle.get("player.placecount") + ": " + db.placecount() + " - <b>#" + array.get(0) + "</b><br>" +
                                            bundle.get("player.breakcount") + ": " + db.breakcount() + " - <b>#" + array.get(1) + "</b><br>" +
                                            bundle.get("player.killcount") + ": " + db.killcount() + " - <b>#" + array.get(2) + "</b><br>" +
                                            bundle.get("player.deathcount") + ": " + db.deathcount() + " - <b>#" + array.get(3) + "</b><br>" +
                                            bundle.get("player.joincount") + ": " + db.joincount() + " - <b>#" + array.get(4) + "</b><br>" +
                                            bundle.get("player.kickcount") + ": " + db.kickcount() + " - <b>#" + array.get(5) + "</b><br>" +
                                            bundle.get("player.level") + ": " + db.level() + " - <b>#" + array.get(6) + "</b><br>" +
                                            bundle.get("player.reqtotalexp") + ": " + db.reqtotalexp() + "<br>" +
                                            bundle.get("player.firstdate") + ": " + db.firstdate() + "<br>" +
                                            bundle.get("player.lastdate") + ": " + db.lastdate() + "<br>" +
                                            bundle.get("player.playtime") + ": " + tool.secToTime(db.playtime()) + " - <b>#" + array.get(7) + "</b><br>" +
                                            bundle.get("player.attackclear") + ": " + db.attackclear() + " - <b>#" + array.get(8) + "</b><br>" +
                                            bundle.get("player.pvpwincount") + ": " + db.pvpwincount() + " - <b>#" + array.get(9) + "</b><br>" +
                                            bundle.get("player.pvplosecount") + ": " + db.pvplosecount() + " - <b>#" + array.get(10) + "</b><br>" +
                                            bundle.get("player.pvpbreakout") + ": " + db.pvpbreakout() + " - <b>#" + array.get(11) + "</b><br>";
                                    bw.write("HTTP/1.1 200 OK\r\n");
                                    bw.write("Date: " + time + "\r\n");
                                    bw.write("Server: Mindustry/Essentials " + vars.pluginVersion() + "\r\n");
                                    bw.write("Content-Type: text/html; charset=utf-8\r\n");
                                    bw.write("Content-Length: " + datatext.getBytes().length + 1 + "\r\n");
                                    bw.write("\r\n");
                                    bw.write(datatext);
                                } else {
                                    bw.write("Login failed!");
                                }
                            } else {
                                bw.write("Login failed!");
                            }
                        }
                        bw.flush();
                    } catch (SQLException e) {
                        new CrashReport(e);
                    }
                } else {
                    InputStream reader = getClass().getResourceAsStream("/HTML/404.html");
                    BufferedReader br = new BufferedReader(new InputStreamReader(reader, StandardCharsets.UTF_8));

                    String line;
                    StringBuilder result = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        result.append(line).append("\n");
                    }

                    boolean rand = new SecureRandom().nextBoolean();
                    InputStream image;
                    if (rand) {
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
                    Base64.Encoder encoder = Base64.getEncoder();
                    if (rand) {
                        changeString = "data:image/gif;base64," + encoder.encodeToString(fileArray);
                    } else {
                        changeString = "data:image/webp;base64," + encoder.encodeToString(fileArray);
                    }
                    Document doc = Jsoup.parse(result.toString());
                    doc.getElementById("box").append("<img src=" + changeString + " alt=\"\">");

                    bw.write("HTTP/1.1 404 Internal error\r\n");
                    bw.write("Date: " + time + "\r\n");
                    bw.write("Server: Mindustry/Essentials 7.0\r\n");
                    bw.write("\r\n");
                    bw.write(doc.toString());
                    Log.info("Web request :" + receive);
                }
            } catch (IOException e) {
                new CrashReport(e);
            } finally {
                shutdown("client.disconnected.http", ip);
            }

        }
    }
}