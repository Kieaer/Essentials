package essentials.net;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import essentials.Global;
import essentials.utils.Config;
import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.Version;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.Administration;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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
import static essentials.Threads.playtime;
import static essentials.Threads.uptime;
import static essentials.core.Log.writelog;
import static essentials.core.PlayerDB.conn;
import static essentials.core.PlayerDB.getData;
import static io.anuke.mindustry.Vars.*;

public class Server implements Runnable {
    public Config config = new Config();
    public static boolean active = true;
    public static ServerSocket serverSocket;
    public static ArrayList<Service> list = new ArrayList<>();
    private String remoteip;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(config.getServerport());
        } catch (IOException e) {
            printStackTrace(e);
            return;
        }
        Global.server("server-enabled");

        while (active) {
            try {
                Socket socket = serverSocket.accept();
                Service service = new Service(socket);
                service.start();
                list.add(service);
            } catch (Exception e) {
                if(e.getMessage().equals("Socket closed")) return;
                printStackTrace(e);
            }
        }
    }

    public class Service extends Thread {
        public BufferedReader in;
        public DataOutputStream os;
        public Socket socket;
        public SecretKeySpec spec;
        public Cipher cipher;

        public Service(Socket socket) {
            try {
                this.socket = socket;
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                os = new DataOutputStream(socket.getOutputStream());
            } catch (Exception e) {
                if(e.getMessage().equals("socket closed")){
                    return;
                }
                printStackTrace(e);
            }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.currentThread().setName(remoteip+" Client Thread");
                try {
                    remoteip = socket.getInetAddress().toString().replace("/", "");

                    String value = in.readLine();

                    // 수신된 데이터가 Base64 가 아닐경우
                    if(value.length() != 24) {
                        writelog("web", "Remote IP: "+remoteip);
                        String headerLine;
                        while ((headerLine = in.readLine()).length() != 0) {
                            writelog("web", headerLine);
                        }
                        writelog("web", "========================");

                        StringBuilder payload = new StringBuilder();
                        while(in.ready()){
                            payload.append((char) in.read());
                        }
                        httpserver(value, payload.toString());
                        return;
                    }

                    String authkey = in.readLine();
                    byte[] encrypted = Base64.decode(value);
                    byte[] key = Base64.decode(authkey);
                    spec = new SecretKeySpec(key, "AES");
                    cipher = Cipher.getInstance("AES");
                    byte[] decrypted = decrypt(encrypted, spec, cipher);
                    String data = new String(decrypted);

                    if (data.equals("")){
                        os.close();
                        in.close();
                        socket.close();
                        list.remove(this);
                        Global.server("client-disconnected", remoteip);
                        return;
                    }

                    if (data.matches("\\[(.*)]:.*")) {
                        String msg = data.replaceAll("\n", "");
                        Global.server("server-message-received", remoteip, msg);
                        for (int i = 0; i < playerGroup.size(); i++) {
                            Player p = playerGroup.all().get(i);
                            if (p.isAdmin) {
                                p.sendMessage("[#C77E36][" + remoteip + "][RC] " + data);
                            } else {
                                p.sendMessage("[#C77E36][RC] " + data);
                            }
                        }

                        // send message to all clients
                        for (Service ser : list) {
                            ser.os.writeBytes(Base64.encode(encrypt(data,spec,cipher))+"\n");
                            ser.os.flush();
                        }
                    } else if (data.matches("ping")) {
                        String[] msg = {"Hi " + remoteip + "! Your connection is successful!", "Hello " + remoteip + "! I'm server!", "Welcome to the server " + remoteip + "!"};
                        int rnd = new Random().nextInt(msg.length);
                        os.writeBytes(Base64.encode(encrypt(msg[rnd],spec,cipher))+"\n");
                        os.flush();
                        Global.server("client-connected", remoteip);
                    } else if (data.matches("exit")){
                        os.close();
                        in.close();
                        socket.close();
                        list.remove(this);
                        Global.server("client-disconnected", remoteip);
                        this.interrupt();
                        return;
                    } else if (config.isBanshare()) {
                        try {
                            JSONTokener convert = new JSONTokener(data);
                            JSONArray bandata = new JSONArray(convert);
                            if(data.substring(data.length()-5).equals("unban")){
                                Global.server("client-request-unban", remoteip);
                                for (int i = 0; i < bandata.length(); i++) {
                                    String[] array = bandata.getString(i).split("\\|", -1);
                                    if (array[0].length() == 12) {
                                        netServer.admins.unbanPlayerID(array[0]);
                                        if (!array[1].equals("<unknown>") && array[1].length() <= 15) {
                                            netServer.admins.unbanPlayerIP(array[1]);
                                        }
                                    }
                                    if (array[0].equals("<unknown>")) {
                                        netServer.admins.unbanPlayerIP(array[1]);
                                    }
                                    Global.server("unban-done", bandata.getString(i));
                                }

                                // send message to all clients
                                for (Service ser : list) {
                                    String remoteip = ser.socket.getInetAddress().toString().replace("/", "");
                                    for (int a = 0; a < config.getBantrust().length; a++) {
                                        String ip = config.getBantrust()[a];
                                        if (ip.equals(remoteip)) {
                                            ser.os.writeBytes(Base64.encode(encrypt(data,spec,cipher))+"\n");
                                            ser.os.flush();
                                            Global.server("server-data-sented", remoteip);
                                        }
                                    }
                                }
                            } else {
                                Global.server("client-request-banlist", remoteip);
                                for (int i = 0; i < bandata.length(); i++) {
                                    String[] array = bandata.getString(i).split("\\|", -1);
                                    if (array[0].length() == 12) {
                                        netServer.admins.banPlayerID(array[0]);
                                        if (!array[1].equals("<unknown>") && array[1].length() <= 15) {
                                            netServer.admins.banPlayerIP(array[1]);
                                        }
                                    }
                                    if (array[0].equals("<unknown>")) {
                                        netServer.admins.banPlayerIP(array[1]);
                                    }
                                }

                                Array<Administration.PlayerInfo> bans = netServer.admins.getBanned();
                                Array<String> ipbans = netServer.admins.getBannedIPs();
                                JSONArray data1 = new JSONArray();
                                if (bans.size != 0) {
                                    for (Administration.PlayerInfo info : bans) {
                                        data1.put(info.id + "|" + info.lastIP);
                                    }
                                }
                                if (ipbans.size != 0) {
                                    for (String string : ipbans) {
                                        data1.put("<unknown>|" + string);
                                    }
                                }

                                // send message to all clients
                                for (Service ser : list) {
                                    String remoteip = ser.socket.getInetAddress().toString().replace("/", "");
                                    for (int a = 0; a < config.getBantrust().length; a++) {
                                        String ip = config.getBantrust()[a];
                                        if (ip.equals(remoteip)) {
                                            ser.os.writeBytes(Base64.encode(encrypt(data1.toString(),spec,cipher))+"\n");
                                            ser.os.flush();
                                            Global.server("server-data-sented", remoteip);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            printStackTrace(e);
                        }
                    } else if(data.contains("checkban")) {
                        Array<Administration.PlayerInfo> bans = netServer.admins.getBanned();
                        Array<String> ipbans = netServer.admins.getBannedIPs();
                        String[] cda = data.replaceAll("checkban", "").split("/");

                        boolean found = false;
                        for(Administration.PlayerInfo info : bans){
                            if (info.id.contains(cda[0])) {
                                found = true;
                                break;
                            }
                        }
                        if(!found){
                            for(String string : ipbans){
                                Administration.PlayerInfo info = netServer.admins.findByIP(string);
                                if(string.contains(cda[1])){
                                    found = true;
                                    break;
                                }
                            }
                        }

                        if(found){
                            os.writeBytes(Base64.encode(encrypt("true",spec,cipher))+"\n");
                        } else {
                            os.writeBytes(Base64.encode(encrypt("false",spec,cipher))+"\n");
                        }
                        os.flush();
                    } else {
                        Global.nlog("Invalid data - " + data);
                    }
                } catch (Exception e) {
                    Global.server("client-disconnected", remoteip);
                    try {
                        os.close();
                        in.close();
                        socket.close();
                        list.remove(this);
                    } catch (IOException ex) {
                        printStackTrace(ex);
                    }
                    return;
                }
            }
        }

        private String query() {
            JSONObject json = new JSONObject();
            JSONObject items = new JSONObject();
            JSONArray array = new JSONArray();
            for (Player p : playerGroup.all()) {
                array.put(p.name);
            }

            for (Item item : content.items()) {
                if (item.type == ItemType.material) {
                    items.put(item.name, state.teams.get(Team.sharded).cores.first().entity.items.get(item));
                }
            }

            boolean online = false;
            for(int a=0;a<playerGroup.size();a++){
                Player target = playerGroup.all().get(a);
                if(target.isAdmin){
                    online = true;
                }
            }

            JSONObject rank = new JSONObject();
            try{
                JSONObject tmp = new JSONObject();
                String[] list = new String[]{"placecount", "breakcount", "killcount", "joincount", "kickcount", "exp", "playtime", "pvpwincount", "reactorcount"};
                Statement stmt = conn.createStatement();
                for (String s : list) {
                    ResultSet rs = stmt.executeQuery("SELECT " + s + ",name FROM players ORDER BY `" + s + "`");
                    while (rs.next()) {
                        tmp.put(rs.getString("name"), rs.getString(s));
                    }
                    rank.put(s, tmp);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            json.put("players", playerGroup.size());
            json.put("playerlist", array);
            json.put("version", Version.build);
            json.put("name", Core.settings.getString("servername"));
            json.put("playtime", playtime);
            json.put("resource", items);
            json.put("mapname", world.getMap().name());
            json.put("wave", state.wave);
            json.put("admin_online", online);
            json.put("rank", rank);
            return json.toString();
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
                String worldtime = playtime;
                String serveruptime = uptime;
                StringBuilder items = new StringBuilder();
                for (Item item : content.items()) {
                    if (item.type == ItemType.material) {
                        items.append(item.name).append(": ").append(state.teams.get(Team.sharded).cores.first().entity.items.get(item)).append("<br>");
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
            JSONObject results = new JSONObject();
            //ArrayList<String> results = new ArrayList<>();

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
                String name = nbundle("server-http-rank-name");
                String country = nbundle("server-http-rank-country");
                String win = nbundle("server-http-rank-pvp-win");
                String lose = nbundle("server-http-rank-pvp-lose");
                String rate = nbundle("server-http-rank-pvp-rate");

                for(int a=0;a<sql.length;a++){
                    ResultSet rs = stmt.executeQuery(sql[a]);
                    JSONArray array = new JSONArray();
                    if(lists.get(a).equals("pvpwincount")){
                        String header = "<tr><th>"+name+"</th><th>"+country+"</th><th>"+win+"</th><th>"+lose+"</th><th>"+rate+"</th></tr>";
                        array.put(header);
                        while(rs.next()){
                            int percent;
                            try {
                                percent = rs.getInt("pvpwincount") / rs.getInt("pvplosecount") * 100;
                            } catch (Exception e) {
                                percent = 0;
                            }
                            String data = "<tr><td>"+rs.getString("name")+"</td><td>"+rs.getString("country")+"</td><td>"+rs.getInt("pvpwincount")+"</td><td>"+rs.getInt("pvplosecount")+"</td><td>"+percent+"%</td></tr>\n";
                            array.put(data);
                        }
                    } else {
                        String header = "<tr><th>"+name+"</th><th>"+country+"</th><th>"+lists.get(a)+"</th></tr>";
                        array.put(header);
                        while (rs.next()) {
                            String data = "<tr><td>" + rs.getString("name") + "</td><td>" + rs.getString("country") + "</td><td>" + rs.getString(lists.get(a)) + "</td></tr>\n";
                            array.put(data);
                        }
                    }
                    results.put(lists.get(a),array);
                    rs.close();
                }
                stmt.close();
            } catch (Exception e) {
                printStackTrace(e);
            }


            InputStream reader = getClass().getResourceAsStream("/HTML/rank.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(reader, StandardCharsets.UTF_8));

            String line;
            StringBuilder result = new StringBuilder();
            while ((line = br.readLine()) != null) {
                result.append(line).append("\n");
            }
            Document doc = Jsoup.parse(result.toString());
            for(int a=0;a<lists.size();a++) {
                for(int b=0;b<results.getJSONArray(lists.get(a)).length();b++){
                    doc.getElementById(lists.get(a)).append(results.getJSONArray(lists.get(a)).getString(b));
                }
            }

            doc.getElementById("info_body").appendText(serverinfo());
            doc.getElementById("rank-placecount").appendText(nbundle("server-http-rank-placecount"));
            doc.getElementById("rank-breakcount").appendText(nbundle("server-http-rank-breakcount"));
            doc.getElementById("rank-killcount").appendText(nbundle("server-http-rank-killcount"));
            doc.getElementById("rank-joincount").appendText(nbundle("server-http-rank-joincount"));
            doc.getElementById("rank-kickcount").appendText(nbundle("server-http-rank-kickcount"));
            doc.getElementById("rank-exp").appendText(nbundle("server-http-rank-exp"));
            doc.getElementById("rank-playtime").appendText(nbundle("server-http-rank-playtime"));
            doc.getElementById("rank-pvpwincount").appendText(nbundle("server-http-rank-pvpcount"));
            doc.getElementById("rank-reactorcount").appendText(nbundle("server-http-rank-reactorcount"));
            doc.getElementById("rank-attackclear").appendText(nbundle("server-http-rank-attackclear"));

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
                        bw.write("Server: Mindustry/Essentials 6.1.2\r\n");
                        bw.write("Content-Type: application/json; charset=utf-8\r\n");
                        bw.write("Content-Length: " + data.getBytes().length + 1 + "\r\n");
                        bw.write("\r\n");
                        bw.write(query());
                    } else if (receive.matches("GET /rank HTTP/.*") || receive.matches("GET /rank# HTTP/.*")) {
                        String rank = rankingdata();
                        bw.write("HTTP/1.1 200 OK\r\n");
                        bw.write("Date: " + time + "\r\n");
                        bw.write("Server: Mindustry/Essentials 6.1.2\r\n");
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
                                JSONObject db = getData(rs.getString("uuid"));
                                String language = db.getString("language");

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
                                if (!config.isSqlite()) {
                                    Statement stmt = conn.createStatement();
                                    ArrayList<String> array = new ArrayList<>();
                                    for (int a = 0; a < ranking.length; a++) {
                                        ResultSet rs1 = stmt.executeQuery(ranking[a]);
                                        while (rs1.next()) {
                                            if(rs1.getString("uuid").equals(db.getString("uuid"))){
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
                                String result = "Login failed!\n";
                                bw.write(result);
                            }
                        } else {
                            String result = "Login failed!\n";
                            bw.write("HTTP/1.1 200 OK\r\n");
                            bw.write("Date: " + time + "\r\n");
                            bw.write("Server: Mindustry/Essentials 6.1.2\r\n");
                            bw.write("Content-Type: text/html; charset=utf-8\r\n");
                            bw.write("Content-Length: " + result.getBytes().length + 1 + "\r\n");
                            bw.write("\r\n");
                            bw.write(result);
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
                        nlog(receive);
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
                server("client-disconnected-http", remoteip);
            }catch (Exception e){
                printStackTrace(e);
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