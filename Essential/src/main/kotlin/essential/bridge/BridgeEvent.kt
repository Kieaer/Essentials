package essential.bridge

import arc.util.Log
import essential.bridge.Main.Companion.conf
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.common.event.CustomEvents
import ksp.event.Event
import java.nio.file.StandardWatchEventKinds

@Event
internal fun configFileModified(event: CustomEvents.ConfigFileModified) {
    if (event.kind === StandardWatchEventKinds.ENTRY_MODIFY) {
        if (event.paths == "config_bridge.yaml") {
            val config = Config.load("config_bridge", BridgeConfig.serializer(), BridgeConfig())
            require(config != null) {
                Log.err(Bundle()["event.plugin.load.failed"])
                return
            }

            conf = config

            Log.info(Bundle()["config.reloaded"])
        }
    }
}