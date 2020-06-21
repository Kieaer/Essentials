package essentials.feature;

import arc.ApplicationListener;
import arc.Core;
import arc.struct.Array;
import essentials.external.PingHost;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import essentials.internal.Tools;
import mindustry.game.Gamemode;
import org.hjson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.TimerTask;

import static essentials.Main.*;
import static org.hjson.JsonValue.readJSON;

public class EventServer {
    public Array<Process> servers = new Array<>();

    public boolean create(String roomname, String map, String gamemode, int port) {
        try {
            JsonObject json = readJSON(tool.getWebContent("https://api.github.com/repos/anuken/Mindustry/releases/latest")).asObject();
            String url = json.get("assets").asArray().get(0).asObject().get("browser_download_url").asString();
            root.child("temp").child(roomname).mkdirs();
            Tools.URLDownload(new URL(url), root.child("temp/" + roomname + "/server.jar").file());
            EventService service = new EventService(roomname, map, Gamemode.valueOf(gamemode), port);
            service.start();
            Thread.sleep(5000);
            return true;
        } catch (Exception e) {
            new CrashReport(e);
            return false;
        }
    }

    public static class EventService extends Thread {
        public String roomname;
        public String map;
        public Gamemode gamemode;
        public int port;
        public int disablecount;

        EventService(String roomname, String map, Gamemode gamemode, int port) {
            this.gamemode = gamemode;
            this.map = map;
            this.roomname = roomname;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                Process p;
                ProcessBuilder pb = new ProcessBuilder("java", "-jar", Paths.get("").toAbsolutePath().toString() + "/config/mods/Essentials/temp/" + roomname + "/server.jar", "config port " + port + ",host " + map + " " + gamemode);
                pb.directory(new File(Paths.get("").toAbsolutePath().toString() + "/config/mods/Essentials/temp/" + roomname));
                pb.inheritIO().redirectOutput(Core.settings.getDataDirectory().child("test.txt").file());
                p = pb.start();
                sleep(2000);
                eventServer.servers.add(p);
                if (p.isAlive()) Log.info(roomname + " Event serer online!");

                TimerTask t = new TimerTask() {
                    @Override
                    public void run() {
                        new PingHost("127.0.0.1", port, result -> {
                            if (disablecount > 300) {
                                try {
                                    JsonObject settings = readJSON(root.child("data/data.json").reader()).asObject();
                                    for (int a = 0; a < settings.get("servers").asArray().size(); a++) {
                                        if (settings.get("servers").asArray().get(a).asObject().getInt("port", 0) == port) {
                                            settings.get("servers").asArray().remove(a);
                                            root.child("data/data.json").writeString(settings.toString());
                                            break;
                                        }
                                    }

                                    p.destroy();
                                    pluginData.process.remove(p);
                                    this.cancel();
                                } catch (IOException e) {
                                    new CrashReport(e);
                                }
                            } else if (result.players == 0) {
                                disablecount++;
                            }
                        });
                    }
                };
                timer.scheduleAtFixedRate(t, 1000, 1000);

                Core.app.addListener(new ApplicationListener() {
                    @Override
                    public void dispose() {
                        timer.cancel();
                    }
                });
            } catch (Exception e) {
                new CrashReport(e);
            }
        }
    }
}
