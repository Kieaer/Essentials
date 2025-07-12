package essential.chat

import arc.util.CommandHandler
import arc.util.Log
import essential.bundle.Bundle
import essential.chat.generated.registerGeneratedClientCommands
import essential.chat.generated.registerGeneratedEventHandlers
import essential.config.Config
import essential.rootPath
import mindustry.mod.Plugin

class Main : Plugin() {
    companion object {
        internal var bundle: Bundle = Bundle()
        internal lateinit var conf: ChatConfig
    }

    override fun init() {
        bundle.prefix = "[EssentialChat]"

        Log.debug(bundle["event.plugin.starting"])

        val config = Config.load("config_chat", ChatConfig.serializer(), ChatConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
        }
        conf = config


        // 이벤트 등록
        registerGeneratedEventHandlers()

        // 채팅 금지어 파일 추가
        if (!rootPath.child("chat_blacklist.txt").exists()) {
            rootPath.child("chat_blacklist.txt").writeString("않")
        }

        Log.debug(bundle["event.plugin.loaded"])
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }
}
