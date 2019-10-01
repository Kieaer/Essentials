package essentials.net;

import essentials.Global;
import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Administration;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static essentials.EssentialConfig.*;
import static essentials.EssentialTimer.playtime;
import static io.anuke.mindustry.Vars.*;

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
            String msg = data.replaceAll("\n", "");
            Global.chats("Received message from "+remoteip+": "+msg);
            Call.sendMessage("[#C77E36][RC] "+msg);
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

    private static String query(){
        JSONObject json = new JSONObject();
        JSONObject items = new JSONObject();
        JSONArray array = new JSONArray();
        for(Player p : playerGroup.all()){
            array.put(p.name);
        }

        for(Item item : content.items()) {
            if(item.type == ItemType.material){
                items.put(item.name, state.teams.get(Team.sharded).cores.first().entity.items.get(item));
            }
        }

        json.put("players", playerGroup.size());
        json.put("playerlist", array);
        json.put("version", Version.build);
        json.put("name", Core.settings.getString("servername"));
        json.put("playtime", playtime);
        json.put("difficulty", Difficulty.values());
        json.put("resource",items);
        return json.toString();
    }

    private void httpserver(){
        try{
            String data = query();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd a hh:mm.ss", Locale.ENGLISH);
            String time = now.format(dateTimeFormatter);

            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);
            if(query){
                bw.write("HTTP/1.1 200 OK\r\n");
                bw.write("Date: "+time+"\r\n");
                bw.write("Server: Mindustry/Essentials 5.0\r\n");
                bw.write("Content-Type: application/json; charset=UTF-8\r\n");
                bw.write("Content-Length: "+data.getBytes().length+1+"\r\n");
                bw.write("\r\n");
                bw.write(query());
            } else {
                bw.write("HTTP/1.1 403 Forbidden\r\n");
                bw.write("Date: "+time+"\r\n");
                bw.write("Server: Mindustry/Essentials 5.0\r\n");
                bw.write("\r\n");
                bw.write("<TITLE>403 Forbidden</TITLE>");
                bw.write("<p>This server isn't allowed query!</p>");
            }
            bw.close();
        } catch (Exception e){
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
                    if(banshare) {
                        ban(data, remoteip);
                    }
                } else if (data.equals("GET / HTTP/1.1")) {
                    httpserver();
                } else {
                    chat(data, remoteip);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}