package essentials.net;

import arc.struct.Array;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonParser;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Administration;

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
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.Global.*;
import static essentials.core.PlayerDB.writeData;
import static essentials.utils.Config.executorService;
import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;

public class Client extends Thread{
    public static Socket socket;
    public static BufferedReader is;
    public static DataOutputStream os;
    public static boolean serverconn;

    public static Cipher cipher;
    public static SecretKeySpec spec;

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
                    serverconn = true;
                    executorService.execute(new Thread(this));
                    log("client","client-enabled");
                }
            } catch (UnknownHostException e) {
                nlog("client","Invalid host!");
            } catch (IOException e) {
                log("client","remote-server-dead");
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
                        JsonArray bandata = new JsonArray();
                        if (bans.size != 0) {
                            for (Administration.PlayerInfo info : bans) {
                                bandata.add(info.id + "|" + info.lastIP);
                            }
                        }
                        if (ipbans.size != 0) {
                            for (String string : ipbans) {
                                bandata.add("<unknown>|" + string);
                            }
                        }
                        byte[] encrypted = encrypt(bandata.toString(),spec,cipher);
                        os.writeBytes(Base64.encode(encrypted));
                        os.flush();
                        log("client","client-banlist-sented");
                    } catch (Exception e) {
                        printError(e);
                    }
                    break;
                case "chat":
                    try {
                        String msg = "[" + player.name + "]: " + message;
                        byte[] encrypted = encrypt(msg,spec,cipher);
                        os.writeBytes(Base64.encode(encrypted)+"\n");
                        os.flush();
                        Call.sendMessage("[#357EC7][SC] " + msg);
                        log("client","client-sent-message", config.getClienthost(), message);
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
                        printError(e);
                    }
                    break;
                case "unban":
                    try {
                        byte[] encrypted = encrypt("[\""+message + "\"]unban",spec,cipher);
                        os.writeBytes(Base64.encode(encrypted)+"\n");
                        os.flush();
                    }catch (Exception e){
                        printError(e);
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

                String data;
                try{
                    byte[] encrypted = Base64.decode(received);
                    byte[] decrypted = decrypt(encrypted, spec, cipher);
                    data = new String(decrypted);
                }catch (Exception e){
                    printError(e);
                    log("client","server-disconnected", config.getClienthost());

                    serverconn = false;
                    try {
                        is.close();
                        os.close();
                        socket.close();
                    } catch (IOException ex) {
                        printError(ex);
                    }
                    return;
                }

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
                        JsonArray bandata = JsonParser.array().from(data);
                        if(data.substring(data.length()-5).equals("unban")){
                            log("client","server-request-unban");
                            for (int i = 0; i < bandata.size(); i++) {
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
                                log("client","unban-done", bandata.getString(i));
                            }
                        } else {
                            for (int i = 0; i < bandata.size(); i++) {
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
                            log("client","client-banlist-received");
                        }
                    }catch (Exception e){
                        printError(e);
                    }
                } else {
                    nlog("warn","Unknown data! - "+data);
                }
            } catch (Exception e) {
                log("client","server-disconnected", config.getClienthost());

                serverconn = false;
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