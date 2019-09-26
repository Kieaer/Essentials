package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Administration;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static essentials.EssentialConfig.banclienthost;
import static essentials.EssentialConfig.banclientport;
import static io.anuke.mindustry.Vars.netServer;

public class EssentialBanClient implements Runnable{
    static Socket socket;
    static boolean active;

    @Override
    public void run() {
        Array<Administration.PlayerInfo> bans = Vars.netServer.admins.getBanned();

        String db = Core.settings.getDataDirectory().child("plugins/Essentials/data.json").readString();
        JSONTokener parser = new JSONTokener(db);
        JSONObject object = new JSONObject(parser);
        if(object.getBoolean("banall")) {
            for (Administration.PlayerInfo info : bans) {
                try {
                    InetAddress address = InetAddress.getByName(banclienthost);
                    socket = new Socket(address, banclientport);
                    OutputStream os = socket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                    BufferedWriter bw = new BufferedWriter(osw);

                    String msg = info.id+"|"+info.lastIP+"\n";
                    bw.write(msg);
                    bw.flush();

                    Log.info("[EssentialsBan] "+info.id+"/"+info.lastIP+" sented to "+banclienthost);

                    InputStream is = socket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    br.readLine();
                    //Log.info("[EssentialsBanClient] "+message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                InetAddress address = InetAddress.getByName(banclienthost);
                socket = new Socket(address, banclientport);
                OutputStream os = socket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
                BufferedWriter bw = new BufferedWriter(osw);

                String msg = "Request\n";
                bw.write(msg);
                bw.flush();

                Log.info("[EssentialsBan] Ban list requested to "+banclienthost);

                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String message = br.readLine();

                JSONTokener ar = new JSONTokener(message);
                JSONObject jsob = new JSONObject(ar);
                Log.info("[EssentialsBanClient] Received data!");

                for (int i = 0; i < jsob.length(); i++) {
                    JSONObject value1 = (JSONObject) jsob.get(String.valueOf(i));
                    String uuid = (String) value1.get("uuid");
                    String ip = (String) value1.get("ip");

                    netServer.admins.banPlayerID(uuid);
                    if(!ip.equals("<unknown>")){
                        netServer.admins.banPlayerIP(ip);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.info("[EssentialsBanClient] Success!");
            object.put("banall", "false");
            Core.settings.getDataDirectory().child("plugins/Essentials/data.json").writeString(String.valueOf(object));
        }
    }
}