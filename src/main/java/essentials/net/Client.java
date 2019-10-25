package essentials.net;

import essentials.Global;
import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Administration;
import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import static essentials.EssentialConfig.*;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.netServer;

public class Client implements Runnable{
    private boolean serverconnected = false;
    private Socket socket;
    private BufferedReader in;
    private OutputStream out;

    private void bansent() {
        try{
            Array<Administration.PlayerInfo> bans = Vars.netServer.admins.getBanned();
            Array<String> ipbans = netServer.admins.getBannedIPs();
            Global.banc("Ban list senting...");
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
            out.write((bandata+"\n").getBytes(StandardCharsets.UTF_8));
            Global.banc("Success!");
        }catch (Exception e){
            printStackTrace(e);
        }
    }

    private void banreceive(String data) {
        JSONTokener ar = new JSONTokener(data);
        JSONArray result = new JSONArray(ar);

        for (int i = 0; i < result.length(); i++) {
            String[] array = result.getString(i).split("\\|", -1);
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
    }

    private void chatsent(String chat, Player player) {
        try {
            String msg = "["+player.name+"]: "+chat;
            out.write((msg+"\n").getBytes(StandardCharsets.UTF_8));
            Call.sendMessage("[#357EC7][SC] "+msg);
            Global.chatc("Message sent to "+ clienthost+" - "+chat+"");
        } catch (Exception e) {
            String url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("mods/Essentials/player.sqlite3");
            player.sendMessage("Server is not responding! Cross-chat disabled!");

            String sql = "UPDATE players SET crosschat = ? WHERE uuid = ?";
            player.sendMessage("[green][INFO] [] Crosschat disabled.");

            try {
                Class.forName("org.sqlite.JDBC");
                Connection conn = DriverManager.getConnection(url);

                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, "false");
                pstmt.setString(2, player.uuid);
                pstmt.executeUpdate();
                pstmt.close();
                conn.close();
            } catch (Exception ex) {
                printStackTrace(ex);
            }
            printStackTrace(e);
        }
    }

    public void main(String request, String chat, Player player) {
        try {
            if(!serverconnected){
                if(clientenable){
                    InetAddress address = InetAddress.getByName(clienthost);
                    socket = new Socket(address, clientport);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    out = socket.getOutputStream();
                    out.write("ping\n".getBytes(StandardCharsets.UTF_8));
                    new Thread(this).start();
                }
            } else {
                switch (request) {
                    case "ban":
                        bansent();
                        break;
                    case "chat":
                        chatsent(chat, player);
                        break;
                    case "ping":
                        out.write("ping\n".getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            Global.loge("Unable to connect to the "+ clienthost+":"+ clientport+" server!");
            printStackTrace(e);
        }
    }

    @Override
    public void run(){
        while (true) {
            try {
                String data = in.readLine();
                if (data == null || data.equals("")) return;

                Global.log(socket.getRemoteSocketAddress().toString());

                if(data.matches("\\[(.*)]:.*")){
                    // if chat
                    Call.sendMessage(data);
                } else if(banshare){
                    // if ban list
                    try{
                        JSONTokener test = new JSONTokener(data);
                        new JSONArray(test);
                        banreceive(data);
                    }catch (Exception e){
                        Global.logw("Unknown data! - " + data);
                    }
                } else {
                    // if ping
                    Global.log(data);
                    serverconnected = true;
                }
            } catch (Exception e) {
                printStackTrace(e);
            }
        }
    }
}