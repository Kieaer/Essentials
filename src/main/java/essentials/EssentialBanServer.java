package essentials;

import io.anuke.arc.collection.Array;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Administration;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static essentials.EssentialConfig.banserverport;
import static io.anuke.mindustry.Vars.netServer;

public class EssentialBanServer implements Runnable {
    private static Socket socket;
    static boolean active = true;
    public static ServerSocket serverSocket;

    @Override
    public void run() {
        try{
            serverSocket = new ServerSocket(banserverport);
            while(active){
                socket = serverSocket.accept();
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String data = br.readLine();
                String remoteip = socket.getRemoteSocketAddress().toString();

                if(data.length() < 9){
                    Log.info("[EssentialsBanServer] Ban list requested from "+remoteip);
                    JSONObject json = new JSONObject();
                    Array<Administration.PlayerInfo> bans = Vars.netServer.admins.getBanned();
                    int i=0;
                    for (Administration.PlayerInfo info : bans) {
                        JSONObject data1 = new JSONObject();
                        data1.put("uuid", info.id);
                        data1.put("ip", info.lastIP);

                        while(i<json.length()){
                            i++;
                        }
                        json.put(String.valueOf(i), data1);
                    }

                    OutputStream os = socket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                    BufferedWriter bw = new BufferedWriter(osw);
                    bw.write(json +"\n");
                    bw.flush();
                    Log.info("[EssentialsBanServer] Ban list sented to "+remoteip);
                } else {
                    Log.info(data);

                    // 11 length UUID + split array 1 length + max 12 length/dot IP = 15
                    if(data.length() > 28 || data.equals("null")){
                        Log.warn("[EssentialsBan] Unknown data received from "+remoteip+"!");
                    } else {
                        String[] array = data.split("\\|", -1);

                        //Log.info("[EssentialsBan] "+array[0]+"/"+array[1]+" data received from "+remoteip);
                        netServer.admins.banPlayerID(array[0]);
                        if(!array[1].equals("<unknown>")){
                            netServer.admins.banPlayerIP(array[1]);
                        }

                        String returnMessage = "Hello "+remoteip+"! Your data is received!\n";

                        OutputStream os = socket.getOutputStream();
                        OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                        BufferedWriter bw = new BufferedWriter(osw);
                        bw.write(returnMessage);
                        bw.flush();
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}