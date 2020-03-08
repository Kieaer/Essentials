package essentials.net;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
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
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import static essentials.Global.*;
import static essentials.Main.config;
import static mindustry.Vars.netServer;

public class Client extends Thread{
    public static Socket socket;
    public static BufferedReader is;
    public static DataOutputStream os;
    public static boolean server_active;

    public static Cipher cipher;
    public static SecretKeySpec spec;

    public enum Request{
        bansync, chat, exit, unbanip, unbanid, datashare
    }

    public Client(){
        try {
            InetAddress address = InetAddress.getByName(config.getClienthost());
            socket.setSoTimeout(30000);
            socket = new Socket(address, config.getClientport());

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
            os.writeBytes(Base64.encode(raw) + "\n");

            // 데이터 전송
            JsonObject json = new JsonObject();
            json.add("type", "ping");

            byte[] encrypted = encrypt(json.toString(), spec, cipher);

            os.writeBytes(Base64.encode(encrypted) + "\n");
            os.flush();

            String receive = new String(decrypt(Base64.decode(is.readLine()), spec, cipher));

            if(JsonValue.readJSON(receive).asObject().get("result") != null){
                server_active = true;
                config.executorService.execute(new Thread(this));
                log(LogType.client, "client-enabled");
            } else {
                throw new Exception("Invalid request!");
            }
        } catch (UnknownHostException e) {
            nlog(LogType.client,"Invalid host!");
        } catch (SocketException e) {
            log(LogType.client, "remote-server-dead");
        } catch (IOException e){
            log(LogType.client, "remote-server-dead");
        } catch (Exception e){
            printError(e);
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
                    for(PlayerInfo info : netServer.admins.getBanned()){
                        ban.add(info.id);
                        for(String ipbans : info.ips){
                            ipban.add(ipbans);
                        }
                    }

                    for(String ipbans : netServer.admins.getBannedIPs()){
                        ipban.add(ipbans);
                    }

                    for(String subbans : netServer.admins.getSubnetBans()){
                        subban.add(subbans);
                    }

                    data.add("type","bansync");
                    data.add("ban",ban);
                    data.add("ipban",ipban);
                    data.add("subban",subban);

                    byte[] encrypted = encrypt(data.toString(), spec, cipher);
                    os.writeBytes(Base64.encode(encrypted));
                    os.flush();

                    log(LogType.client, "client-banlist-sented");
                } catch (Exception e) {
                    printError(e);
                }
                break;
            case chat:
                try {
                    data.add("type","chat");
                    data.add("name",player.name);
                    data.add("message",message);

                    byte[] encrypted = encrypt(data.toString(), spec, cipher);
                    os.writeBytes(Base64.encode(encrypted) + "\n");
                    os.flush();

                    Call.sendMessage("[#357EC7][SC] [orange]" + player.name + "[orange]: [white]" + message);
                    log(LogType.client, "client-sent-message", config.getClienthost(), message);
                } catch (Exception e) {
                    printError(e);
                }
                break;
            case exit:
                try {
                    data.add("type","exit");

                    byte[] encrypted = encrypt(data.toString(), spec, cipher);
                    os.writeBytes(Base64.encode(encrypted) + "\n");
                    os.flush();

                    os.close();
                    is.close();
                    socket.close();
                    server_active = false;
                    this.interrupt();
                    return;
                } catch (Exception e) {
                    printError(e);
                }
                break;
            case unbanip:
                try {
                    data.add("type","unbanip");

                    boolean isip;
                    try{
                        isip = InetAddress.getByName(message).getHostAddress().equals(message);
                    } catch (UnknownHostException ex){
                        isip = false;
                    }

                    if(isip) data.add("ip",message);

                    byte[] encrypted = encrypt(data.toString(), spec, cipher);
                    os.writeBytes(Base64.encode(encrypted) + "\n");
                    os.flush();
                } catch (Exception e) {
                    printError(e);
                }
                break;
            case unbanid:
                try {
                    data.add("type","unbanid");
                    data.add("uuid",message);

                    byte[] encrypted = encrypt(data.toString(), spec, cipher);
                    os.writeBytes(Base64.encode(encrypted) + "\n");
                    os.flush();
                } catch (Exception e) {
                    printError(e);
                }
                break;
            case datashare:
                try {
                    data.add("type","datashare");
                    data.add("data","");
                    byte[] encrypted = encrypt("datashare", spec, cipher);
                    os.writeBytes(Base64.encode(encrypted) + "\n");
                    os.flush();

                    /*String data = is.readLine();
                    if (data.equals())*/
                } catch (Exception e) {
                    printError(e);
                }
                break;
        }
    }

    @Override
    public void run(){
        while(!Thread.currentThread().isInterrupted()){
            try{
                String received = is.readLine();
                if (received == null || received.equals("")) return;

                JsonObject data;
                try{
                    byte[] encrypted = Base64.decode(received);
                    byte[] decrypted = decrypt(encrypted, spec, cipher);
                    data = JsonValue.readJSON(new String(decrypted)).asObject();
                }catch (Exception e){
                    printError(e);
                    log(LogType.client,"server-disconnected", config.getClienthost());

                    server_active = false;
                    try {
                        is.close();
                        os.close();
                        socket.close();
                    } catch (IOException ex) {
                        printError(ex);
                    }
                    return;
                }

                Request type = Request.valueOf(data.get("type").asString());
                switch (type){
                    case bansync:
                        // 적용
                        JsonArray ban = data.get("ban").asArray();
                        JsonArray ipban = data.get("ipban").asArray();
                        JsonArray subban = data.get("subban").asArray();

                        for (JsonValue b : ban){
                            netServer.admins.banPlayerID(b.asString());
                        }

                        for (JsonValue b : ipban){
                            netServer.admins.banPlayerIP(b.asString());
                        }

                        for (JsonValue b : subban){
                            netServer.admins.addSubnetBan(b.asString());
                        }
                        break;
                    case chat:
                        String name = data.get("name").asString();
                        String message = data.get("message").asString();
                        Call.sendMessage("[#C77E36][RC] [orange]"+name+" [orange]:[white] "+message);
                        break;
                    case exit:
                        server_active = false;
                        try {
                            is.close();
                            os.close();
                            socket.close();
                        } catch (IOException ex) {
                            printError(ex);
                        }
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
                }
            } catch (Exception e) {
                log(LogType.client,"server-disconnected", config.getClienthost());

                server_active = false;
                try {
                    is.close();
                    os.close();
                    socket.close();
                } catch (IOException ex) {
                    printError(ex);
                }
                return;
            }
        }
    }
}