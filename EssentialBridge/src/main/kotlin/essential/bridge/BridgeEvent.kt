package essential.bridge

import arc.util.Log
import essential.bridge.Main.Companion.conf
import essential.bundle.Bundle
import essential.config.Config
import essential.event.CustomEvents
import ksp.event.Event

@Event
internal fun configFileModified(event: CustomEvents.ConfigFileModified) {
    if (event.kind === java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
        if (event.paths == "config_bridge.yaml") {
            val config = Config.load("config_bridge.yaml", BridgeConfig.serializer(), true, BridgeConfig())
            require(config != null) {
                Log.err(Bundle()["event.plugin.load.failed"])
                return
            }

            conf = config

            Log.info(Bundle()["config.reloaded"])
        }
    }
}