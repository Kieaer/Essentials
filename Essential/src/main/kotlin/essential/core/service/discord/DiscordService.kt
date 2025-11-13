package essential.core.service.discord

import arc.util.CommandHandler
import arc.util.Log
import essential.common.bundle.Bundle
import essential.core.service.discord.generated.registerGeneratedClientCommands
import mindustry.mod.Plugin

class DiscordService : Plugin() {
    companion object {
        var bundle: Bundle = Bundle()
        lateinit var conf: DiscordConfig
    }

    override fun init() {
        bundle.prefix = "[EssentialDiscord]"

        val url = conf.url
        if (url.isEmpty() && !url.matches("https://discord\\.gg/[a-zA-Z0-9]{1,16}".toRegex())) {
            Log.warn(bundle["config.invalid.url"])
        }
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }
}
