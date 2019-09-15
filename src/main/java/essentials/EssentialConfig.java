package essentials;

import io.anuke.arc.Core;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class EssentialConfig {
    static String host;
    static int port;
    static boolean realname;
    static boolean detectreactor;

    static void main() throws IOException {
        Map<String, Object> obj;
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);

            Map<String, Object> data = new HashMap<>();
            data.put("port", 25000);
            data.put("realname", true);
            data.put("host", "localhost");
            data.put("detectreactor", true);

            FileWriter writer = new FileWriter(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/config.txt")));
            yaml.dump(data, writer);
        }

        if (Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()){
            Yaml yaml = new Yaml();
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").readString()));
            host = (String) obj.get("host");
            port = Integer.parseInt(String.valueOf(obj.get("port")));
            realname = Boolean.parseBoolean(String.valueOf(obj.get("port")));
            realname = Boolean.parseBoolean(String.valueOf(obj.get("detectreactor")));
        }
    }
}
