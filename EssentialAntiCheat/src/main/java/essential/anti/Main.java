package essential.anti;

import arc.util.Log;
import essential.core.Bundle;
import mindustry.mod.Plugin;

import static essential.core.Main.root;

public class Main extends Plugin {
    static final String CONFIG_PATH = "config/config_anti.yaml";
    static final Bundle bundle = new Bundle();

    @Override
    public void init() {
        Thread.currentThread().setName("EssentialAntiCheat");
        bundle.setPrefix("[EssentialAntiCheat]");

        Log.debug(bundle.get("event.plugin.starting"));

        // 플러그인 설정
        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(this.getClass().getResourceAsStream("/config_anti.yaml"), false);
        }

        Log.info(bundle.get("event.plugin.loaded"));
    }
}
