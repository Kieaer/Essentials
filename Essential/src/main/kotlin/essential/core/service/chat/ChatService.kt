package essential.core.service.chat

import arc.util.CommandHandler
import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.common.rootPath
import essential.core.service.chat.generated.registerGeneratedClientCommands
import essential.core.service.chat.generated.registerGeneratedEventHandlers
import kotlinx.coroutines.runBlocking
import mindustry.mod.Plugin

class ChatService : Plugin() {
    companion object {
        var bundle: Bundle = Bundle()
        var conf: ChatConfig = reloadConf()

        fun reloadConf() : ChatConfig {
            return runBlocking {
                val config = Config.load("config_chat", ChatConfig.serializer(), ChatConfig())
                require(config != null) {
                    Log.err(bundle["event.plugin.load.failed"])
                }
                config
            }
        }
    }

    override fun init() {
        bundle.prefix = "[EssentialChat]"

        Log.debug(bundle["event.plugin.starting"])


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
