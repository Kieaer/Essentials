package essentials.net;

import essentials.Global;
import io.anuke.arc.collection.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Administration;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static essentials.EssentialConfig.serverport;
import static io.anuke.mindustry.Vars.netServer;

public class Server implements Runnable{
    public static ServerSocket serverSocket;
    public static boolean active = true;
    static Socket socket;
    public static Player player;

    private static void ban(String data, String remoteip){
        try{
            if(data.length() < 9){
                Global.bans("Ban list requested from "+remoteip);
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
                Global.bans("Ban list sented to "+remoteip);
            } else {
                // 11 length UUID + split array 1 length + max 12 length/dot IP = 15
                if(data.length() > 28){
                    Global.bansw("Unknown data received from "+remoteip+"!");
                } else {
                    String[] array = data.split("\\|", -1);

                    //Global.bans("[EssentialsBan] "+array[0]+"/"+array[1]+" data received from "+remoteip);
                    if(array[0].length() == 11) {
                        netServer.admins.banPlayerID(array[0]);
                        if (!array[1].equals("<unknown>") && array[1].length() <= 15) {
                            netServer.admins.banPlayerIP(array[1]);
                        }
                    }

                    String returnMessage = "Hello "+remoteip+"! Your data is received!\n";

                    OutputStream os = socket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                    BufferedWriter bw = new BufferedWriter(osw);
                    bw.write(returnMessage);
                    bw.flush();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void chat(String data, String remoteip){
        try{
            Global.chats("Received message from "+remoteip+": "+data);
            Call.sendMessage("[#C77E36][RC] " + data.replaceAll("\n", ""));
            /*
            if(!remoteip.equals(EssentialConfig.clienthost)) {
                Global.chatsw("[EssentialsChat] ALERT! This message isn't received from "+EssentialConfig.clienthost+"!!");
                Global.chatsw("[EssentialsChat] Message is "+data);
            } else {
                Call.sendMessage("[#C77E36][RC] " + data);
            }
            */
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try{
            serverSocket = new ServerSocket(serverport);
            while (active){
                socket = serverSocket.accept();
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String data = br.readLine();
                String remoteip = socket.getRemoteSocketAddress().toString();

                if(data.matches("(.*)\\|(.*)")){
                    ban(data, remoteip);
                } else {
                    chat(data, remoteip);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}