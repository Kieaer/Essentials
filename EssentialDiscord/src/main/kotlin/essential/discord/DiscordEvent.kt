package essential.discord

import arc.util.Log
import essential.bundle.Bundle
import essential.config.Config
import essential.discord.Main.Companion.bundle
import essential.discord.Main.Companion.conf
import essential.event.CustomEvents
import ksp.event.Event

@Event
fun configFileModified(event: CustomEvents.ConfigFileModified) {
    if (event.kind === java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
        if (event.paths == "config_discord.yaml") {
            val config = Config.load("config_discord.yaml", DiscordConfig.serializer(), DiscordConfig())
            require(config != null) {
                Log.err(bundle["event.plugin.load.failed"])
                return
            }
            conf = config

            Log.info(Bundle()["config.reloaded"])
        }
    }
}