package essential.bridge

import arc.Events

class Event {
    @essential.core.annotation.Event
    fun configFileModified() {
        Events.on(ConfigFileModified::class.java, { e ->
            if (e.getKind() === java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
                if (e.getPaths().equals("config_bridge.yaml")) {
                    essential.bridge.Main.Companion.conf = essential.core.Main.Companion.createAndReadConfig(
                        "config_bridge.yaml",
                        java.util.Objects.requireNonNull<T?>(this.javaClass.getResourceAsStream("/config_bridge.yaml")),
                        essential.bridge.BridgeConfig::class.java
                    )
                    Log.info(Bundle().get("config.reloaded"))
                }
            }
        })
    }
}
