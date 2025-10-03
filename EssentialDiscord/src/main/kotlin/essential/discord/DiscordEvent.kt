package essential.discord

import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.common.event.CustomEvents
import essential.discord.Main.Companion.bundle
import essential.discord.Main.Companion.conf
import ksp.event.Event

@Event
fun configFileModified(event: CustomEvents.ConfigFileModified) {
    if (event.kind === java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
        if (event.paths == "config_discord.yaml") {
            val config = Config.load("config_discord", DiscordConfig.serializer(), DiscordConfig())
            require(config != null) {
                bundle["event.plugin.load.failed"]
                return
            }
            conf = config

            Log.info(Bundle()["config.reloaded"])
        }
    }
}