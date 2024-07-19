package essential.web;

import arc.util.Log;
import essential.core.Bundle;
import mindustry.mod.Plugin;

import java.util.Objects;

public class Main extends Plugin {
    static Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialWeb]");

        Log.debug(bundle.get("event.plugin.starting"));

        conf = essential.core.Main.Companion.createAndReadConfig(
                "config_web.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_web.yaml")),
                Config.class
        );

        Log.info(bundle.get("event.plugin.loaded"));
    }
}
