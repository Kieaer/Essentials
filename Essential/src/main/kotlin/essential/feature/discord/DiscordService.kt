package essential.feature.discord

import arc.util.CommandHandler
import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.discord.generated.registerGeneratedClientCommands
import mindustry.mod.Plugin

class DiscordService : Plugin() {
    companion object {
        internal var bundle: Bundle = Bundle()
        internal lateinit var conf: DiscordConfig
    }

    override fun init() {
        bundle.prefix = "[EssentialDiscord]"

        Log.debug(bundle["event.plugin.starting"])

        // 플러그인 설정
        val config = Config.load("config_discord", DiscordConfig.serializer(), DiscordConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
        }

        conf = config

        if (conf.url.isEmpty() && !conf.url.matches("https://discord\\.gg/[a-zA-Z0-9]{1,16}".toRegex())) {
            Log.warn(bundle["config.invalid.url"])
        }

        Log.debug(bundle["event.plugin.loaded"])
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }
}
