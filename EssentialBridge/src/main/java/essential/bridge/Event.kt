package essential.bridge;

import arc.Events;
import arc.util.Log;
import essential.core.Bundle;

import java.nio.file.StandardWatchEventKinds;
import java.util.Objects;

public class Event {
    @essential.core.annotation.Event
    void configFileModified() {
        Events.on(essential.core.CustomEvents.ConfigFileModified.class, e -> {
            if (e.getKind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                if (e.getPaths().equals("config_bridge.yaml")) {
                    Main.conf = essential.core.Main.Companion.createAndReadConfig(
                            "config_bridge.yaml",
                            Objects.requireNonNull(this.getClass().getResourceAsStream("/config_bridge.yaml")),
                            Config.class
                    );
                    Log.info(new Bundle().get("config.reloaded"));
                }
            }
        });
    }
}
