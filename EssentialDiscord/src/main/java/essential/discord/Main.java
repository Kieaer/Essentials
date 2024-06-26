package essential.discord;

import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.core.Bundle;
import mindustry.mod.Plugin;

import java.io.IOException;

import static essential.core.Main.root;

public class Main extends Plugin {
    static String CONFIG_PATH = "config/config_discord.yaml";
    static Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialDiscord]");

        Log.debug(bundle.get("event.plugin.starting"));

        // 플러그인 설정
        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(this.getClass().getResourceAsStream("/config_chat.yaml"), false);
        }

        // 설정 파일 읽기
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            conf = mapper.readValue(root.child(CONFIG_PATH).file(), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!conf.getUrl().isEmpty() && !conf.getUrl().matches("^https://discord\\\\.gg/[a-zA-Z0-9_-]{6,16}$")) {
            Log.warn(bundle.get("config.invalid.url"));
        }

        Log.info(bundle.get("event.plugin.loaded"));
    }
}
