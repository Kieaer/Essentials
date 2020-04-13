package essentials.feature;

import arc.ApplicationListener;
import arc.Core;
import arc.struct.Array;
import essentials.external.PingHost;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.game.Gamemode;
import org.codehaus.plexus.util.FileUtils;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.TimerTask;

import static essentials.Main.*;

public class EventServer {
    public Array<Process> servers = new Array<>();

    public boolean create(String roomname, String map, String gamemode, int port) {
        try {
            JsonObject json = JsonValue.readJSON(Jsoup.connect("https://api.github.com/repos/kieaer/Essentials/releases/latest").ignoreContentType(true).execute().body()).asObject();
            String url = json.get("assets").asObject().get("0").asObject().get("browser_download_url").asString();
            FileUtils.copyURLToFile(new URL(url), new File(Paths.get("").toAbsolutePath().toString() + "/config/mods/Essentials/temp/" + roomname + "/server.jar"));
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
                pluginData.process.add(p);
                if (p.isAlive()) Log.info(roomname + " Event serer online!");

                TimerTask t = new TimerTask() {
                    @Override
                    public void run() {
                        new PingHost("localhost", port, result -> {
                            if (disablecount > 300) {
                                try {
                                    JsonObject settings = JsonValue.readJSON(root.child("data/data.json").reader()).asObject();
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
