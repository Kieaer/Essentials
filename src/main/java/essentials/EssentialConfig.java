package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class EssentialConfig {
    static String clienthost;
    static int clientport;
    static int serverport;
    static boolean realname;
    static boolean detectreactor;
    static boolean serverenable;
    static boolean clientenable;

    static void main() {
        Map<String, Object> obj;
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()) {
            // Make file
            Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").writeString("");

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);

            Map<String, Object> data = new HashMap<>();
            data.put("server-enable", false);
            data.put("server-port", 25000);
            data.put("client-port", 20000);
            data.put("realname", true);
            data.put("client-enable", false);
            data.put("client-host", "localhost");
            data.put("detectreactor", true);

            FileWriter writer = null;
            try {
                writer = new FileWriter(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/config.txt")));
            } catch (IOException e) {
                e.printStackTrace();
            }
            yaml.dump(data, writer);

            Log.info("[Essentials] config file created!");
        }

        if(!Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").exists()){
            JSONObject ban = new JSONObject();
            String json = ban.toString();
            Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").writeString(json);
            Log.info("[Essentials] banned file created!");
        }

        if(!Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").exists()){
            String msg = "To edit this message, modify the [green]motd.txt[] file in the [green]config/plugins/Essentials/[] folder.";
            Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").writeString(msg);
            Log.info("[Essentials] motd file created.");
        }

        if(!Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").exists()){
            JSONArray blacklist = new JSONArray();
            String json = blacklist.toString();
            Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").writeString(json);
            Log.info("[Essentials] blacklist file created!");
        }

        if (Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()){
            Yaml yaml = new Yaml();
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").readString()));
            clienthost = (String) obj.get("client-host");
            clientport = Integer.parseInt(String.valueOf(obj.get("client-port")));
            serverport = Integer.parseInt(String.valueOf(obj.get("server-port")));
            realname = Boolean.parseBoolean(String.valueOf(obj.get("realname")));
            detectreactor = Boolean.parseBoolean(String.valueOf(obj.get("detectreactor")));
            serverenable = Boolean.parseBoolean(String.valueOf(obj.get("server-enable")));
            clientenable = Boolean.parseBoolean(String.valueOf(obj.get("client-enable")));
            Log.info("[Essentials] config file loaded!");
        }
    }
}
