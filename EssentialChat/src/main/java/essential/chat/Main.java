package essential.chat;

import arc.util.Log;
import essential.core.Bundle;
import essential.core.DB;
import mindustry.mod.Plugin;

import java.util.Objects;

import static essential.core.Main.database;

public class Main extends Plugin {
    static Bundle bundle = new Bundle();
    static Config conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialChat]");

        Log.debug(bundle.get("event.plugin.starting"));

        conf = essential.core.Main.Companion.createAndReadConfig(
                "config_chat.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_chat.yaml")),
                Config.class
        );

        // 이벤트 등록
        new Event().load();

        Log.info(bundle.get("event.plugin.loaded"));
    }

    DB.PlayerData findPlayerByUuid(String uuid) {
        return database.getPlayers().stream().filter(e -> e.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
