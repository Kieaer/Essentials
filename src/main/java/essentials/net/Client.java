package essentials.net;

import essentials.Global;
import essentials.utils.Config;
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

import static essentials.Global.nbundle;
import static essentials.Global.printStackTrace;
import static essentials.utils.Config.executorService;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.mindustry.Vars.playerGroup;

public class Client extends Thread{
    public Config config = new Config();
    public static Socket socket;
    public static BufferedReader br;
    public static BufferedWriter bw;
    public static boolean serverconn;

    public void update(){
        Global.logc(nbundle("client-checking-version"));

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
            DefaultArtifactVersion current = new DefaultArtifactVersion("5.1.3");

            if(latest.compareTo(current) > 0){
                Global.logc(nbundle("version-new"));
            } else if(latest.compareTo(current) == 0){
                Global.logc(nbundle("version-current"));
            } else if(latest.compareTo(current) < 0){
                Global.logc(nbundle("version-devel"));
            }

        } catch (Exception e){
            printStackTrace(e);
        }
    }

    public void main(String option, Player player, String message){
        if(!serverconn){
            try {
                InetAddress address = InetAddress.getByName(config.getClienthost());
                socket = new Socket(address, config.getClientport());
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
                    executorService.execute(new Thread(this));
                    Global.logc(nbundle("client-enabled"));
                }
            } catch (UnknownHostException e) {
                Global.loge("Invalid host!");
            } catch (IOException e) {
                Global.loge("I/O Exception");
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
                        bw.write(bandata + "\n");
                        bw.flush();
                        Global.logc(nbundle("client-banlist-sented"));
                    } catch (IOException e) {
                        printStackTrace(e);
                    }
                    break;
                case "chat":
                    try {
                        String msg = "[" + player.name + "]: " + message;
                        bw.write(msg + "\n");
                        bw.flush();
                        Call.sendMessage("[#357EC7][SC] " + msg);
                        Global.logc(nbundle("client-sent-message", config.getClienthost(), message));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "exit":
                    try {
                        bw.write("exit");
                        bw.flush();

                        bw.close();
                        br.close();
                        socket.close();
                        serverconn = false;
                        this.interrupt();
                        return;
                    } catch (IOException e) {
                        printStackTrace(e);
                    }
                    break;
                case "unban":
                    try {
                        bw.write("[\""+message + "\"]unban\n");
                        bw.flush();
                    }catch (IOException e){
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
                String data = br.readLine();
                if (data == null || data.equals("")) return;

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
                        if(data.substring(data.length()-5).equals("unban")){
                            JSONTokener convert = new JSONTokener(data);
                            JSONArray bandata = new JSONArray(convert);
                            Global.logc(nbundle("server-request-unban"));
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
                                Global.logc(nbundle("unban-done", bandata.getString(i)));
                            }
                        } else {
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
                            Global.logc(nbundle("client-banlist-received"));
                        }
                    }catch (Exception e){
                        printStackTrace(e);
                    }
                } else {
                    Global.logw("Unknown data! - "+data);
                }
            } catch (IOException e) {
                Global.logc(nbundle("server-disconnected", config.getClienthost()));

                serverconn = false;
                try {
                    bw.close();
                    br.close();
                    socket.close();
                } catch (IOException ex) {
                    printStackTrace(ex);
                }
                return;
            }
        }
    }
}