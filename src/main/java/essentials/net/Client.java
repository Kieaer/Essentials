package essentials.net;

import essentials.Global;
import io.anuke.arc.collection.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Administration;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static essentials.EssentialConfig.*;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.mindustry.Vars.playerGroup;

public class Client extends Thread{
    public static Socket socket;
    private static BufferedReader br;
    private static BufferedWriter bw;
    public static boolean serverconn;

    public static void update(){
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
            DefaultArtifactVersion current = new DefaultArtifactVersion("5.0");

            if(latest.compareTo(current) > 0){
                Global.log("New version found!");
            } else if(latest.compareTo(current) == 0){
                Global.log("Current version is up to date.");
            } else if(latest.compareTo(current) < 0){
                Global.log("You're using development version!");
            }

        } catch (Exception e){
            printStackTrace(e);
        }
    }

    public void main(String option, Player player, String message){
        if(!serverconn){
            try {
                InetAddress address = InetAddress.getByName(clienthost);
                socket = new Socket(address, clientport);
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                bw = new BufferedWriter(osw);
                bw.write("ping\n");
                bw.flush();

                br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String line = br.readLine();
                if(line != null){
                    Global.logc(line);
                    serverconn = true;
                    new Thread(this).start();
                }
            } catch (UnknownHostException e) {
                Global.loge("Invalid host!");
            } catch (IOException e) {
                Global.loge("I/O Exception");
            }
        } else {
            if(option.equals("bansync")){
                try{
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
                    bw.write(bandata+"\n");
                    bw.flush();
                    Global.logc("Ban list sented!");
                } catch (IOException e) {
                    printStackTrace(e);
                }
            } else if(option.equals("chat")){
                try {
                    String msg = "["+player.name+"]: "+message;
                    bw.write(msg+"\n");
                    bw.flush();
                    Call.sendMessage("[#357EC7][SC] "+msg);
                    Global.logc("Message sent to "+ clienthost+" - "+message+"");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run(){
        while(serverconn){
            try{
                String data = br.readLine();
                if (data == null || data.equals("")) return;

                Global.log(socket.getRemoteSocketAddress().toString());

                if(data.matches("\\[(.*)]:.*")){
                    for (int i = 0; i < playerGroup.size(); i++) {
                        Player player = playerGroup.all().get(i);
                        Pattern p = Pattern.compile("\\[(.*?)]");
                        Matcher m = p.matcher(data);
                        if(!m.group().equals(player.name)){
                            Call.sendMessage("[#C77E36][RC] "+data);
                        }
                    }
                } else if(banshare){
                    try{
                        JSONTokener test = new JSONTokener(data);
                        JSONArray result = new JSONArray(test);
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
                        Global.logc("Ban data received!");
                    }catch (Exception e){
                        printStackTrace(e);
                    }
                } else {
                    Global.logw("Unknown data! - "+data);
                }
            } catch (IOException e) {
                String msg = e.getMessage();
                if (msg.equals("Connection reset")) {
                    Global.logs(clienthost + " Server disconnected");
                    return;
                }
                if (msg.equals("socket closed")) {
                    Global.logs(clienthost + " Server disconnected");
                    return;
                }
                serverconn = false;
                Global.log(msg);
            }
        }
    }
}