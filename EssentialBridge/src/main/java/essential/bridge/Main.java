package essential.bridge;

import arc.util.Log;
import essential.core.Bundle;
import mindustry.mod.Plugin;

public class Main extends Plugin {
    static final String CONFIG_PATH = "config/config_bridge.yaml";
    static final Bundle bundle = new Bundle();

    @Override
    public void init() {
        bundle.setPrefix("[EssentialChat]");

        Log.debug(bundle.get("event.plugin.starting"));
        Log.info(bundle.get("event.plugin.loaded"));
    }
}
