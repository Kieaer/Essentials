package essential.web;

import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.core.Bundle;
import mindustry.mod.Plugin;

import java.io.IOException;

import static essential.core.Main.root;

public class Main extends Plugin {
    static String CONFIG_PATH = "config/config_web.yaml";
    static Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialWeb]");

        Log.debug(bundle.get("event.plugin.starting"));

        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(this.getClass().getResourceAsStream("/config_web.yaml"), false);
        }

        // 설정 파일 읽기
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            conf = mapper.readValue(root.child("config/config_protect.yaml").file(), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.info(bundle.get("event.plugin.loaded"));
    }
}
