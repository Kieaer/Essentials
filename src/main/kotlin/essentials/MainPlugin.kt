package essentials

import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import essentials.command.CommandManager
import essentials.database.DatabaseManager
import essentials.event.EventManager
import essentials.thread.ThreadManager
import mindustry.mod.Plugin

class MainPlugin : Plugin() {
    private val rootPath: Fi = Core.settings.dataDirectory.child("mods/Essentials/")

    val databaseManager = DatabaseManager()
    val commandManager = CommandManager()
    val eventManager = EventManager()
    val threadManager = ThreadManager()

    override fun init() {
        if (!rootPath.exists()) rootPath.mkdirs()

        // Database 시작
        databaseManager.initialize()

        // 이벤트 시작
        eventManager.initialize()

        // Thread 시작
        threadManager.initialize()

        // 명령어 시작
        commandManager.initialize()
    }

    override fun registerServerCommands(handler: CommandHandler) {
        commandManager.serverCommandList.forEach {
            handler.register(it.command, it.parameter, it.description, it.runner)
        }
    }

    override fun registerClientCommands(handler: CommandHandler) {
        commandManager.clientCommandList.forEach {
            handler.register(it.command, it.parameter, it.description, it.runner)
        }
    }
}