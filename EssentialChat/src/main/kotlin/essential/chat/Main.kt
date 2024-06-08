package essential.chat

import arc.Events
import arc.util.CommandHandler
import arc.util.Log
import essential.core.PluginData
import mindustry.game.EventType.ServerLoadEvent
import mindustry.mod.Plugin

class Main : Plugin() {
    override fun init() {
        Log.info("EssentialChat enabled.")
        Events.on(ServerLoadEvent::class.java) {
            Log.info("[chat] ${PluginData.pluginVersion}")
        }
    }

    override fun registerServerCommands(handler: CommandHandler?) {
        super.registerServerCommands(handler)
    }

    override fun registerClientCommands(handler: CommandHandler?) {
        super.registerClientCommands(handler)
    }
}