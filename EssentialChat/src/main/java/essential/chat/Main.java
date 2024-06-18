package essential.chat;

import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.core.Bundle;
import essential.core.DB;
import mindustry.mod.Plugin;

import java.io.IOException;
import java.util.Optional;

import static essential.core.Main.players;
import static essential.core.Main.root;

public class Main extends Plugin {
    static final String CONFIG_PATH = "config/config_chat.yaml";
    static final Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialChat]");

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

        Log.info(bundle.get("event.plugin.loaded"));
    }

    DB.PlayerData findPlayerByUuid(String uuid) {
        Optional<DB.PlayerData> data =  players.stream().filter(e -> e.getUuid().equals(uuid)).findFirst();
        return data.orElse(null);
    }
}
