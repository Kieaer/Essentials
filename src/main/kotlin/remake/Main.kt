package remake

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import mindustry.Vars
import mindustry.mod.Plugin
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Main : Plugin() {
    private val daemon: ExecutorService = Executors.newSingleThreadExecutor()
    private val timer = java.util.Timer()

    companion object {
        val database = DB()
        val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    }

    init {
        if (Core.settings.has("debugMode") && Core.settings.getBool("debugMode")){
            root.child("database.db").delete()
        }

        createFile()
        database.open()
        Config.load()
        Permission.load()
        PluginData.load()

        Event.register()
        Trigger.thread.run()

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                Trigger.interrupted = true
                timer.cancel()
                database.close()
                daemon.shutdownNow()
                Permission.save()
                Commands.Discord.shutdownNow()
            }
        })
    }

    override fun init() {
        daemon.submit(FileWatchService)
        timer.scheduleAtFixedRate(Trigger.Seconds(), 1000, 1000)

        Vars.netServer.admins.addChatFilter { _, _ -> null }

        Vars.netServer.admins.addActionFilter { e ->
            if (e.player == null) return@addActionFilter true
            val data = database.players.find { it.uuid == e.player.uuid() }
            if (Config.authType == Config.AuthType.None){
                if (data != null) {
                    !data.mute
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    override fun registerClientCommands(handler: CommandHandler) {
        Commands(handler, true)
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Commands(handler, false)
    }

    private fun createFile() {
        if (!root.child("log").exists()) {
            val names =
                arrayOf("block", "chat", "deposit", "error", "griefer", "non-block", "player", "tap", "web", "withdraw")
            for (a in names) {
                if (!root.child("log/$a.log").exists()) {
                    root.child("log").mkdirs()
                    root.child("log/$a.log").file().createNewFile()
                }
            }
        }

        if (!root.child("motd").exists()) {
            root.child("motd").mkdirs()
            val names = arrayListOf("en", "ko")
            val texts = arrayListOf(
                "To edit this message, open [green]config/mods/Essentials/motd[] folder and edit [green]en.txt[]",
                "이 메세지를 수정할려면 [green]config/mods/Essentials/motd[] 폴더에서 [green]ko.txt[] 파일을 수정하세요."
            )
            for (a in 0 until names.size) {
                if (!root.child("motd/${names[a]}.txt").exists()) {
                    root.child("motd/${names[a]}.txt").writeString(texts[a])
                }
            }
        }
    }
}