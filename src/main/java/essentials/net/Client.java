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
        Global.banc("Ban list senting...");
        JSONArray bandata = new JSONArray();
        for (Administration.PlayerInfo info : bans) {
            bandata.put(info.id+"|"+info.lastIP);
        }
        try {
            bw.write(bandata +"\n");
            bw.flush();

            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();

            JSONTokener ar = new JSONTokener(message);
            JSONArray result = new JSONArray(ar);
            Global.banc("Received data!");

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
            Global.banc("Success!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void chat(BufferedWriter bw, String chat, Player player) {
        try {
            String msg = chat+"\n";
            bw.write(msg);
            bw.flush();
            Call.sendMessage("[#357EC7][SC] "+chat);
            Global.chatc("Message sent to "+clienthost+" - "+chat+"");
        } catch (Exception e) {
            String url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("plugins/Essentials/player.sqlite3");
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

    private static void ping(BufferedWriter bw){
        try{
            bw.write("ping\n");
            bw.flush();

            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();
            Global.log(message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String request, String chat, Player player) {
        try {
            InetAddress address = InetAddress.getByName(clienthost);
            Global.log("Trying connect to "+address+":"+clientport+"...");
            socket = new Socket(address, clientport);
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);

            switch (request) {
                case "ban":
                    ban(bw);
                    break;
                case "chat":
                    chat(bw, chat, player);
                    break;
                case "ping":
                    ping(bw);
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}