package essentials.net;

import essentials.Global;
import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Administration;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import static essentials.EssentialConfig.clienthost;
import static essentials.EssentialConfig.clientport;
import static io.anuke.mindustry.Vars.netServer;

public class Client{
    private static Socket socket;

    private static void ban(BufferedWriter bw) {
        Array<Administration.PlayerInfo> bans = Vars.netServer.admins.getBanned();
        for (Administration.PlayerInfo info : bans) {
            try {
                String msg = info.id + "|" + info.lastIP + "\n";
                bw.write(msg);
                bw.flush();

                Global.banc(info.id + "/" + info.lastIP + " sented to " + clienthost);

                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                br.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try{
            bw.write("Request");
            bw.flush();

            Global.banc("Ban list requested to "+clienthost);

            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();

            JSONTokener ar = new JSONTokener(message);
            JSONObject jsob = new JSONObject(ar);
            Global.banc("Received data!");

            for (int i = 0; i < jsob.length(); i++) {
                JSONObject value1 = (JSONObject) jsob.get(String.valueOf(i));
                String uuid = (String) value1.get("uuid");
                String ip = (String) value1.get("ip");

                netServer.admins.banPlayerID(uuid);
                if(!ip.equals("<unknown>")){
                    netServer.admins.banPlayerIP(ip);
                }
            }
            Global.banc("Success!");
            socket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void chat(BufferedWriter bw, String chat, Player player) {
        try {
            String msg = chat+"\n";
            bw.write(msg);
            bw.flush();
            Call.sendMessage("[#357EC7][SC] " + chat);
            Global.log("Message sent to " + clienthost + ": " + msg);
        } catch (Exception e) {
            String url = "jdbc:sqlite:" + Core.settings.getDataDirectory().child("plugins/Essentials/player.sqlite3");
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
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    public static void main(String request, String chat, Player player) {
        try{
            InetAddress address = InetAddress.getByName(clienthost);
            socket = new Socket(address, clientport);
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);

            if(request.equals("ban")){
                ban(bw);
            } else if(request.equals("chat")){
                chat(bw, chat, player);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}