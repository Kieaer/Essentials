package essentials;

import io.anuke.arc.Core;
import org.json.JSONObject;

class EssentialConfig {
    static JSONObject main() {
        JSONObject configs = null;
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()) {
            String value = "## Notice: All settings must be written after `: ` and don't change line\n" +
                    "## Cross-server server config\n" +
                    "host: localhost\n" +
                    "port: 10000\n";
            Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").writeString(value);
        }

        if (Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()){
            String config = Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").readString();
            String[] lines = config.split("\\n");
            configs = new JSONObject();
            configs.put("host", lines[2].replace("host: ", ""));
            configs.put("port", lines[3].replace("port: ", ""));
        }
        return configs;
    }
}
