package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

class EssentialChatClient {
    private static Socket socket;
    private static String host = EssentialConfig.clienthost;
    private static int port = EssentialConfig.clientport;

    static void main(String msg, Player player) {
        Thread t = new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                Thread.currentThread().setName("Chat client thread");
                try {
                    InetAddress address = InetAddress.getByName(host);
                    socket = new Socket(address, port);

                    OutputStream os = socket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                    BufferedWriter bw = new BufferedWriter(osw);

                    bw.write(msg);
                    bw.flush();
                    Call.sendMessage("[#357EC7][SC] "+msg+"\n");
                    Log.info("[SC] Message sent to "+host+": "+msg);
                } catch (Exception ignored) {
                    String url = "jdbc:sqlite:"+ Core.settings.getDataDirectory().child("plugins/Essentials/player.sqlite3");
                    player.sendMessage("Server is not responding! Cross-chat disabled!");

                    String sql = "UPDATE players SET crosschat = ? WHERE uuid = ?";
                    player.sendMessage("[green][INFO] [] Crosschat disabled.");

                    try{
                        Class.forName("org.sqlite.JDBC");
                        Connection conn = DriverManager.getConnection(url);

                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, "false");
                        pstmt.setString(2, player.uuid);
                        pstmt.executeUpdate();
                        pstmt.close();
                        conn.close();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                } finally {
                    try {
                        socket.close();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }
}