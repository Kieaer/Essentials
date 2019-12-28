package essentials.net;

import arc.struct.Array;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import essentials.Global;
import essentials.utils.Config;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Administration;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.Global.*;
import static essentials.core.PlayerDB.writeData;
import static essentials.utils.Config.executorService;
import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;

public class Client extends Thread{
    public Config config = new Config();
    public static Socket socket;
    public static BufferedReader is;
    public static DataOutputStream os;
    public static boolean serverconn;

    public static Cipher cipher;
    public static SecretKeySpec spec;

    public void update(){
        Global.client("client-checking-version");

        Thread t = new Thread(() -> {
            HttpURLConnection con;
            try {
                String apiURL = "https://api.github.com/repos/kieaer/Essentials/releases/latest";
                URL url = new URL(apiURL);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("Content-length", "0");
                con.setUseCaches(false);
                con.setAllowUserInteraction(false);
                con.setConnectTimeout(3000);
                con.setReadTimeout(3000);
                con.connect();
                int status = con.getResponseCode();
                StringBuilder response = new StringBuilder();

                switch (status) {
                    case 200:
                    case 201:
                        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line).append("\n");
                        }
                        br.close();
                        con.disconnect();
                }

                JSONTokener parser = new JSONTokener(response.toString());
                JSONObject object = new JSONObject(parser);

                DefaultArtifactVersion latest = new DefaultArtifactVersion(object.getString("tag_name"));
                DefaultArtifactVersion current = new DefaultArtifactVersion(version);

                if (latest.compareTo(current) > 0) {
                    Global.client("version-new");

                } else if (latest.compareTo(current) == 0) {
                    Global.client("version-current");
                } else if (latest.compareTo(current) < 0) {
                    Global.client("version-devel");
                }

            } catch (Exception e) {
                printStackTrace(e);
            }
        });
        t.start();
    }

    public void main(String option, Player player, String message){
        if(!serverconn){
            try {
                InetAddress address = InetAddress.getByName(config.getClienthost());
                socket = new Socket(address, config.getClientport());
                KeyGenerator gen = KeyGenerator.getInstance("AES");
                SecretKey key = gen.generateKey();
                gen.init(256);
                byte[] raw = key.getEncoded();
                spec = new SecretKeySpec(raw,"AES");
                cipher = Cipher.getInstance("AES");
                is = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                os = new DataOutputStream(socket.getOutputStream());
                byte[] encrypted = encrypt("ping",spec,cipher);

                os.writeBytes(Base64.encode(encrypted)+"\n");
                os.writeBytes(Base64.encode(raw)+"\n");
                os.flush();

                String data = is.readLine();
                if(data != null){
                    Global.nclient(data);
                    serverconn = true;
                    executorService.execute(new Thread(this));
                    Global.client("client-enabled");
                }
            } catch (UnknownHostException e) {
                Global.client("Invalid host!");
            } catch (IOException e) {
                Global.client("remote-server-dead");
                if(player != null) {
                    writeData("UPDATE players SET crosschat = ? WHERE uuid = ?",0, player.uuid);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        } else {
            switch (option) {
                case "bansync":
                    try {
                        Array<Administration.PlayerInfo> bans = Vars.netServer.admins.getBanned();
                        Array<String> ipbans = netServer.admins.getBannedIPs();
                        JSONArray bandata = new JSONArray();
                        if (bans.size != 0) {
                            for (Administration.PlayerInfo info : bans) {
                                bandata.put(info.id + "|" + info.lastIP);
                            }
                        }
                        if (ipbans.size != 0) {
                            for (String string : ipbans) {
                                bandata.put("<unknown>|" + string);
                            }
                        }
                        byte[] encrypted = encrypt(bandata.toString(),spec,cipher);
                        os.writeBytes(Base64.encode(encrypted));
                        os.flush();
                        Global.client("client-banlist-sented");
                    } catch (Exception e) {
                        printStackTrace(e);
                    }
                    break;
                case "chat":
                    try {
                        String msg = "[" + player.name + "]: " + message;
                        byte[] encrypted = encrypt(msg,spec,cipher);
                        os.writeBytes(Base64.encode(encrypted)+"\n");
                        os.flush();
                        Call.sendMessage("[#357EC7][SC] " + msg);
                        Global.client("client-sent-message", config.getClienthost(), message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "exit":
                    try {
                        byte[] encrypted = encrypt("exit",spec,cipher);
                        os.writeBytes(Base64.encode(encrypted)+"\n");
                        os.flush();

                        os.close();
                        is.close();
                        socket.close();
                        serverconn = false;
                        this.interrupt();
                        return;
                    } catch (Exception e) {
                        printStackTrace(e);
                    }
                    break;
                case "unban":
                    try {
                        byte[] encrypted = encrypt("[\""+message + "\"]unban",spec,cipher);
                        os.writeBytes(Base64.encode(encrypted)+"\n");
                        os.flush();
                    }catch (Exception e){
                        printStackTrace(e);
                    }
                    break;
            }
        }
    }

    @Override
    public void run(){
        while(!Thread.currentThread().isInterrupted()){
            try{
                String received = is.readLine();
                if (received == null || received.equals("")) return;

                byte[] encrypted = Base64.decode(received);
                byte[] decrypted = decrypt(encrypted,spec,cipher);
                String data = new String(decrypted);

                if(data.matches("\\[(.*)]:.*")){
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player player = playerGroup.all().get(i);
                        Matcher m = Pattern.compile("\\[(.*?)]").matcher(player.name);
                        if(m.find()){
                            if(m.group(1).equals(player.name)){
                                Call.sendMessage("[#C77E36][RC] "+data);
                            }
                        }
                    }
                } else if(config.isBanshare()){
                    try{
                        JSONTokener convert = new JSONTokener(data);
                        JSONArray bandata = new JSONArray(convert);
                        if(data.substring(data.length()-5).equals("unban")){
                            Global.client("server-request-unban");
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
                                Global.client("unban-done", bandata.getString(i));
                            }
                        } else {
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
                            Global.client("client-banlist-received");
                        }
                    }catch (Exception e){
                        printStackTrace(e);
                    }
                } else {
                    Global.nlog("Unknown data! - "+data);
                }
            } catch (Exception e) {
                Global.client("server-disconnected", config.getClienthost());

                serverconn = false;
                try {
                    is.close();
                    os.close();
                    socket.close();
                } catch (IOException ex) {
                    printStackTrace(ex);
                }
                return;
            }
        }
    }
}