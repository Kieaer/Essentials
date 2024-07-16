package essential.achievements;

import arc.util.CommandHandler;
import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.core.Bundle;
import mindustry.mod.Plugin;

import java.io.IOException;

import static essential.core.Main.root;

public class Main extends Plugin {
    static String CONFIG_PATH = "config/config_achievements.yaml";
    static Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialAchievements]");

        Log.debug(bundle.get("event.plugin.starting"));

        // 플러그인 설정
        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(this.getClass().getResourceAsStream("/config_achievements.yaml"), false);
        }

        // 설정 파일 읽기
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try {
            conf = mapper.readValue(root.child(CONFIG_PATH).file(), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.info(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {

    }
}
