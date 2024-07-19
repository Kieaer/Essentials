package essential.discord;

import arc.util.Log;
import essential.core.Bundle;
import mindustry.mod.Plugin;

import java.util.Objects;

public class Main extends Plugin {
    static Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialDiscord]");

        Log.debug(bundle.get("event.plugin.starting"));

        // 플러그인 설정
        conf = essential.core.Main.Companion.createAndReadConfig(
                "config_discord.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_discord.yaml")),
                Config.class
        );

        if (!conf.getUrl().isEmpty() && !conf.getUrl().matches("^https://discord\\\\.gg/[a-zA-Z0-9_-]{6,16}$")) {
            Log.warn(bundle.get("config.invalid.url"));
        }

        Log.info(bundle.get("event.plugin.loaded"));
    }
}
