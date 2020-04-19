package essentials.network;

import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Administration.PlayerInfo;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import javax.crypto.Cipher;
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

public class Client extends Thread {
    public Socket socket;
    public BufferedReader is;
    public DataOutputStream os;

    public Cipher cipher;
    public SecretKeySpec spec;

    public boolean activated = false;
    Base64.Encoder encoder = Base64.getEncoder();
    Base64.Decoder decoder = Base64.getDecoder();
    private boolean disconnected = false;

    public void wakeup() {
        try {
            InetAddress address = InetAddress.getByName(config.clienthost);
            socket = new Socket(address, config.clientport);
            socket.setSoTimeout(disconnected ? 2000 : 0);

            // 키 생성
            KeyGenerator gen = KeyGenerator.getInstance("AES");
            SecretKey key = gen.generateKey();
            gen.init(256);
            byte[] raw = key.getEncoded();

            spec = new SecretKeySpec(raw, "AES");
            cipher = Cipher.getInstance("AES");
            is = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            os = new DataOutputStream(socket.getOutputStream());

            // 키값 보내기
            os.writeBytes(encoder.encodeToString(raw) + "\n");
            os.flush();

            // 데이터 전송
            JsonObject json = new JsonObject();
            json.add("type", "ping");

            byte[] encrypted = tool.encrypt(json.toString(), spec, cipher);

            os.writeBytes(encoder.encodeToString(encrypted) + "\n");
            os.flush();

            String receive = new String(tool.decrypt(decoder.decode(is.readLine()), spec, cipher));

            if (JsonValue.readJSON(receive).asObject().get("result") != null) {
                activated = true;
                mainThread.execute(new Thread(this));
                Log.client(JsonValue.readJSON(receive).asObject().get("result").asString());
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
                    TimeUnit.SECONDS.sleep(5);
                    this.wakeup();
                } catch (InterruptedException ex) {
                    this.interrupt();
                }
            } else {
                Log.client("remote-server-dead");
            }
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    public void request(Request request, Player player, String message) {
        JsonObject data = new JsonObject();
        switch (request) {
            case bansync:
                try {
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

                    byte[] encrypted = tool.encrypt(data.toString(), spec, cipher);
                    os.writeBytes(encoder.encodeToString(encrypted) + "\n");
                    os.flush();

                    Log.client("client.banlist.sented");
                } catch (Exception e) {
                    new CrashReport(e);
                }
                break;
            case chat:
                try {
                    data.add("type", "chat");
                    data.add("name", player.name);
                    data.add("message", message);

                    byte[] encrypted = tool.encrypt(data.toString(), spec, cipher);
                    os.writeBytes(encoder.encodeToString(encrypted) + "\n");
                    os.flush();

                    Call.sendMessage("[#357EC7][SC] [orange]" + player.name + "[orange]: [white]" + message);
                    Log.client("client.message", config.clienthost, message);
                } catch (Exception e) {
                    new CrashReport(e);
                }
                break;
            case exit:
                try {
                    data.add("type", "exit");

                    byte[] encrypted = tool.encrypt(data.toString(), spec, cipher);
                    os.writeBytes(encoder.encodeToString(encrypted) + "\n");
                    os.flush();

                    os.close();
                    is.close();
                    socket.close();
                    activated = false;
                    this.interrupt();
                    return;
                } catch (Exception e) {
                    new CrashReport(e);
                }
                break;
            case unbanip:
                try {
                    data.add("type", "unbanip");

                    boolean isip;
                    try {
                        isip = InetAddress.getByName(message).getHostAddress().equals(message);
                    } catch (UnknownHostException ex) {
                        isip = false;
                    }

                    if (isip) data.add("ip", message);

                    byte[] encrypted = tool.encrypt(data.toString(), spec, cipher);
                    os.writeBytes(encoder.encodeToString(encrypted) + "\n");
                    os.flush();
                } catch (Exception e) {
                    new CrashReport(e);
                }
                break;
            case unbanid:
                try {
                    data.add("type", "unbanid");
                    data.add("uuid", message);

                    byte[] encrypted = tool.encrypt(data.toString(), spec, cipher);
                    os.writeBytes(encoder.encodeToString(encrypted) + "\n");
                    os.flush();
                } catch (Exception e) {
                    new CrashReport(e);
                }
                break;
            case datashare:
                try {
                    data.add("type", "datashare");
                    data.add("data", "");
                    byte[] encrypted = tool.encrypt("datashare", spec, cipher);
                    os.writeBytes(encoder.encodeToString(encrypted) + "\n");
                    os.flush();

                    /*String data = is.readLine();
                    if (data.equals())*/
                } catch (Exception e) {
                    new CrashReport(e);
                }
                break;
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
                    data = JsonValue.readJSON(new String(tool.decrypt(decoder.decode(is.readLine()), spec, cipher))).asObject();
                } catch (IllegalArgumentException | SocketException e) {
                    disconnected = true;
                    Log.client("server.disconnected", config.clienthost);
                    if (!Thread.currentThread().isInterrupted()) this.wakeup();
                    return;
                } catch (Exception e) {
                    if (!e.getMessage().equals("Socket closed")) new CrashReport(e);
                    Log.client("server.disconnected", config.clienthost);

                    activated = false;
                    try {
                        is.close();
                        os.close();
                        socket.close();
                    } catch (IOException ex) {
                        new CrashReport(ex);
                    }
                    return;
                }

                Request type = Request.valueOf(data.get("type").asString());
                switch (type) {
                    case bansync:
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

                        System.out.println("DONE");
                        break;
                    case chat:
                        String name = data.get("name").asString();
                        String message = data.get("message").asString();
                        Call.sendMessage("[#C77E36][RC] [orange]" + name + " [orange]:[white] " + message);
                        break;
                    case exit:
                        activated = false;
                        try {
                            is.close();
                            os.close();
                            socket.close();
                        } catch (IOException ex) {
                            new CrashReport(ex);
                        }
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
                Log.client("server.disconnected", config.clienthost);

                activated = false;
                try {
                    is.close();
                    os.close();
                    socket.close();
                } catch (IOException ex) {
                    new CrashReport(ex);
                }
                return;
            }
        }
    }

    public enum Request {
        bansync, chat, exit, unbanip, unbanid, datashare
    }
}