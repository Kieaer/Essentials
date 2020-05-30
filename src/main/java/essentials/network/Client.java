package essentials.network;

import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Administration.PlayerInfo;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static essentials.Main.*;
import static mindustry.Vars.netServer;
import static org.hjson.JsonValue.readJSON;

public class Client implements Runnable {
    public Socket socket;
    public BufferedReader is;
    public DataOutputStream os;

    public SecretKey skey;

    public boolean activated = false;
    private boolean disconnected = false;

    public void shutdown() {
        try {
            Thread.currentThread().interrupt();
            os.close();
            is.close();
            socket.close();
            activated = false;
        } catch (IOException ignored) {
        }
    }

    public void wakeup() {
        try {
            InetAddress address = InetAddress.getByName(config.clientHost());
            Thread.sleep(3000);
            socket = new Socket(address, config.clientPort());
            socket.setSoTimeout(disconnected ? 2000 : 0);

            // 키 생성
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            gen.init(128);
            SecretKey key = gen.generateKey();

            byte[] raw = key.getEncoded();
            skey = new SecretKeySpec(raw, "AES");
            is = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            os = new DataOutputStream(socket.getOutputStream());

            // 키값 보내기
            os.writeBytes(new String(Base64.getEncoder().encode(raw)) + "\n");
            os.flush();

            // 데이터 전송
            JsonObject json = new JsonObject();
            json.add("type", "ping");

            String encrypted = tool.encrypt(json.toString(), skey);
            os.writeBytes(encrypted + "\n");
            os.flush();

            String receive = tool.decrypt(is.readLine(), skey);

            if (readJSON(receive).asObject().get("result") != null) {
                activated = true;
                mainThread.execute(new Thread(this));
                Log.client(readJSON(receive).asObject().get("result").asString());
                Log.client(disconnected ? "client.reconnected" : "client.enabled", socket.getInetAddress().toString().replace("/", ""));
                disconnected = false;
            } else {
                throw new SocketException("Invalid request!");
            }
        } catch (UnknownHostException e) {
            Log.client("Invalid host!");
        } catch (SocketTimeoutException | SocketException e) {
            if (disconnected) {
                try {
                    socket.close();
                    TimeUnit.SECONDS.sleep(5);
                    this.wakeup();
                } catch (InterruptedException | IOException ex) {
                    Thread.currentThread().interrupt();
                }
            } else {
                try {
                    socket.close();
                    Log.client("remote-server-dead");
                } catch (IOException ignored) {
                }
            }
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    public void request(Request request, Player player, String message) {
        JsonObject data = new JsonObject();
        try {
            switch (request) {
                case bansync:
                    JsonArray ban = new JsonArray();
                    JsonArray ipban = new JsonArray();
                    JsonArray subban = new JsonArray();
                    for (PlayerInfo info : netServer.admins.getBanned()) {
                        ban.add(info.id);
                        for (String ipbans : info.ips) {
                            ipban.add(ipbans);
                        }
                    }

                    for (String ipbans : netServer.admins.getBannedIPs()) {
                        ipban.add(ipbans);
                    }

                    for (String subbans : netServer.admins.getSubnetBans()) {
                        subban.add(subbans);
                    }

                    data.add("type", "bansync");
                    data.add("ban", ban);
                    data.add("ipban", ipban);
                    data.add("subban", subban);

                    os.writeBytes(tool.encrypt(data.toString(), skey) + "\n");
                    os.flush();

                    Log.client("client.banlist.sented");
                    break;
                case chat:
                    data.add("type", "chat");
                    data.add("name", player.name);
                    data.add("message", message);

                    os.writeBytes(tool.encrypt(data.toString(), skey) + "\n");
                    os.flush();

                    Call.sendMessage("[#357EC7][SC] [orange]" + player.name + "[orange]: [white]" + message);
                    Log.client("client.message", config.clientHost(), message);
                    break;
                case exit:
                    data.add("type", "exit");

                    os.writeBytes(tool.encrypt(data.toString(), skey) + "\n");
                    os.flush();

                    shutdown();
                    return;
                case unbanip:
                    data.add("type", "unbanip");

                    boolean isip;
                    try {
                        isip = InetAddress.getByName(message).getHostAddress().equals(message);
                    } catch (UnknownHostException ex) {
                        isip = false;
                    }

                    if (isip) data.add("ip", message);

                    os.writeBytes(tool.encrypt(data.toString(), skey) + "\n");
                    os.flush();
                    break;
                case unbanid:
                    data.add("type", "unbanid");
                    data.add("uuid", message);

                    os.writeBytes(tool.encrypt(data.toString(), skey) + "\n");
                    os.flush();
                    break;
                case datashare:
                    data.add("type", "datashare");
                    data.add("data", "");

                    os.writeBytes(tool.encrypt("datashare", skey) + "\n");
                    os.flush();

                /*String data = is.readLine();
                if (data.equals())*/
                    break;
            }
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    @Override
    public void run() {
        disconnected = false;
        try {
            socket.setSoTimeout(0);
        } catch (SocketException e) {
            new CrashReport(e);
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                JsonObject data;
                try {
                    data = readJSON(new String(tool.decrypt(is.readLine(), skey))).asObject();
                } catch (IllegalArgumentException | SocketException e) {
                    disconnected = true;
                    Log.client("server.disconnected", config.clientHost());
                    if (!Thread.currentThread().isInterrupted()) this.wakeup();
                    return;
                } catch (Exception e) {
                    if (!e.getMessage().equals("Socket closed")) new CrashReport(e);
                    Log.client("server.disconnected", config.clientHost());

                    shutdown();
                    return;
                }

                Request type = Request.valueOf(data.get("type").asString());
                switch (type) {
                    case bansync:
                        Log.client("client.banlist.received");

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

                        Log.client("success");
                        break;
                    case chat:
                        String name = data.get("name").asString();
                        String message = data.get("message").asString();
                        Call.sendMessage("[#C77E36][RC] [orange]" + name + " [orange]:[white] " + message);
                        break;
                    case exit:
                        shutdown();
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
                }
            } catch (Exception e) {
                Log.client("server.disconnected", config.clientHost());

                shutdown();
                return;
            }
        }
    }

    public enum Request {
        bansync, chat, exit, unbanip, unbanid, datashare
    }
}