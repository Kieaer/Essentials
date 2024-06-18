package essential.bridge;

import arc.Core;
import arc.util.CommandHandler;
import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.core.Bundle;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

import java.io.IOException;

import static essential.core.Main.root;

public class Main extends Plugin {
    static final String CONFIG_PATH = "config/config_bridge.yaml";
    static final Bundle bundle = new Bundle();
    static Boolean connectType = false;
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialBridge]");

        Log.debug(bundle.get("event.plugin.starting"));

        // 플러그인 설정
        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(this.getClass().getResourceAsStream("/config_bridge.yaml"), false);
        }

        //todo 서버 포트별로 이름을 설정할 수 있고, 설정에서 서버 그룹별로 해당 설정을 덮어쓰기 할 수 있게 만들기
        // 설정 파일 읽기
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            conf = mapper.readValue(root.child(CONFIG_PATH).file(), Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (conf.isCountAllServers()) {
            Core.settings.put("totalPlayers", 0);
            Core.settings.saveValues();
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
