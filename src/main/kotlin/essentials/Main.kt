package essentials

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import essentials.Permission.bundle
import mindustry.Vars
import mindustry.mod.Plugin
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonValue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Main : Plugin() {
    val daemon: ExecutorService = Executors.newFixedThreadPool(2)

    companion object {
        val database = DB()
        val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    }

    init {
        Log.info("[Essentials] Loading")
        if (Core.settings.has("debugMode") && Core.settings.getBool("debugMode")) {
            root.child("database.db").delete()
        }

        createFile()
        database.open()
        Config.load()
        Permission.load()
        PluginData.load()

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                database.close()
                daemon.shutdownNow()
                Commands.Discord.shutdownNow()
                Permission.save()
                Permission.sort()
                Config.save()
            }
        })

        Event.register()
    }

    override fun init() {
        Log.info("[Essentials] Starting")
        daemon.submit(FileWatchService)
        daemon.submit(Trigger.Thread())
        if (Config.botToken.isNotEmpty() && Config.channelToken.isNotEmpty()) Commands.Discord.start()

        if (Config.update) {
            try {
                Http.get("https://api.github.com/repos/kieaer/Essentials/releases/latest") {
                    if (it.status == Http.HttpStatus.OK) {
                        val json = JsonValue.readJSON(it.resultAsString).asObject()
                        for (a in 0 until Vars.mods.list().size) {
                            if (Vars.mods.list()[a].meta.name == "Essentials") {
                                PluginData.pluginVersion = Vars.mods.list()[a].meta.version
                            }
                        }
                        val latest = DefaultArtifactVersion(json.getString("tag_name", PluginData.pluginVersion))
                        val current = DefaultArtifactVersion(PluginData.pluginVersion)

                        when {
                            latest > current -> Log.info(bundle["config.update.new", json["assets"].asArray()[0].asObject().get("browser_download_url").asString(), json.get("body").asString()])
                            latest.compareTo(current) == 0 -> Log.info(bundle["config.update.current"])
                            latest < current -> Log.info(bundle["config.update.devel"])
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            for (a in 0 until Vars.mods.list().size) {
                if (Vars.mods.list()[a].meta.name == "Essentials") {
                    PluginData.pluginVersion = Vars.mods.list()[a].meta.version
                    break
                }
            }
        }

        Vars.netServer.admins.addChatFilter { _, _ -> null }

        Vars.netServer.admins.addActionFilter { e ->
            if (e.player == null) return@addActionFilter true
            val data = database.players.find { it.uuid == e.player.uuid() }
            return@addActionFilter data != null
        }
        Log.info("[Essentials] Loaded.")
    }

    override fun registerClientCommands(handler: CommandHandler) {
        Commands(handler, true)
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Commands(handler, false)
    }

    private fun createFile() {
        if (!root.child("log").exists()) {
            val names = arrayOf("block", "chat", "deposit", "error", "griefer", "non-block", "player", "tap", "web", "withdraw")
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

        if (!root.child("messages").exists()) {
            root.child("messages").mkdirs()
            val names = arrayListOf("en", "ko")
            val texts = arrayListOf(
                "To edit this message, open [green]config/mods/Essentials/messages[] folder and edit [green]en.txt[]",
                "이 메세지를 수정할려면 [green]config/mods/Essentials/messages[] 폴더에서 [green]ko.txt[] 파일을 수정하세요."
            )
            for (a in 0 until names.size) {
                if (!root.child("messages/${names[a]}.txt").exists()) {
                    root.child("messages/${names[a]}.txt").writeString(texts[a])
                }
            }
        }
    }
}