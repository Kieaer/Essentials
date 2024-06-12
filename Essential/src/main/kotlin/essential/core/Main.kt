package essential.core

import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import com.charleskorn.kaml.Yaml
import essential.core.Event.findPlayerData
import essential.core.annotation.ClientCommand
import essential.core.annotation.ServerCommand
import inside.commands.CommandManager
import mindustry.Vars
import mindustry.gen.Call
import mindustry.gen.Playerc
import mindustry.mod.Plugin
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonValue
import java.util.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions


class Main : Plugin() {
    companion object {
        const val CONFIG_PATH = "config/config.yaml"
        lateinit var conf: Config

        @JvmField
        val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        @JvmField
        val players : ArrayList<DB.PlayerData> = arrayListOf()
        @JvmField
        val database = DB()

        @JvmStatic
        fun findPlayerByUuid(uuid: String): DB.PlayerData? {
            return players.find { e -> e.uuid == uuid }
        }

        val commandManager = CommandManager()

        val bundle = Bundle()
    }

    init {
        Thread.currentThread().name = "Essential"
        bundle.prefix = "[Essential]"
    }

    override fun init() {
        Log.info(bundle["event.plugin.starting"])

        // 플러그인 설정
        if (!root.child(CONFIG_PATH).exists()) {
            root.child(CONFIG_PATH).write(this::class.java.getResourceAsStream("/config.yaml")!!, false)
        }

        conf = Yaml.default.decodeFromString(Config.serializer(), root.child(CONFIG_PATH).readString())
        bundle.locale = Locale(conf.plugin.lang)

        // DB 설정
        database.load()
        database.connect()
        database.create()

        // 데이터 설정
        PluginData.load()

        // 업데이트 확인
        checkUpdate()

        Log.info(bundle["event.plugin.loaded"])
    }

    override fun registerServerCommands(handler: CommandHandler) {
        Commands::class.functions.forEach { function ->
            function.findAnnotation<ServerCommand>()?.let { annotation ->
                handler.register(annotation.name, annotation.parameter, annotation.description) { args ->
                    function.call(Commands::class, *args)
                }
            }
        }
    }


    override fun registerClientCommands(handler: CommandHandler) {
        Commands::class.functions.forEach { function ->
            function.findAnnotation<ClientCommand>()?.let { annotation ->
                handler.register(annotation.name, annotation.parameter, annotation.description) { args, player : Playerc ->
                    val data = findPlayerData(player.uuid()) ?: DB.PlayerData()
                    if (checkPermission(data, annotation.name)) {
                        function.call(Commands::class, player, data, *args)
                    } else {
                        if (annotation.name == "js") {
                            Call.kick(player.con(), "js no permission")
                        } else {
                            player.sendMessage("no permission 4 u")
                        }
                    }
                }
            }
        }
    }

    private fun checkUpdate() {
        if (conf.plugin.autoUpdate) {
            Http.get("https://api.github.com/repos/kieaer/Essentials/releases/latest").timeout(1000)
                .error { _ -> Log.warn(bundle["event.plugin.update.check.failed"]) }
                .block {
                if (it.status == Http.HttpStatus.OK) {
                    val json = JsonValue.readJSON(it.resultAsString).asObject()
                    PluginData.pluginVersion = JsonValue.readJSON(this::class.java.getResourceAsStream("/plugin.json")!!.reader().readText()).asObject()["version"].asString()
                    val latest = DefaultArtifactVersion(json.getString("tag_name", PluginData.pluginVersion))
                    val current = DefaultArtifactVersion(PluginData.pluginVersion)

                    when {
                        latest > current -> Log.info(bundle["config.update.new", json["assets"].asArray()[0].asObject()["browser_download_url"].asString(), json["body"].asString()])
                        latest.compareTo(current) == 0 -> Log.info(bundle["config.update.current"])
                        latest < current -> Log.info(bundle["config.update.devel"])
                    }
                }
            }
        } else {
            Vars.mods.list().forEach { mod ->
                if (mod.meta.name == "Essentials") {
                    PluginData.pluginVersion = mod.meta.version
                    return@forEach
                }
            }
        }
    }

    fun checkPermission(data: DB.PlayerData, command: String) : Boolean {
        if (!Permission.check(data, command)) {
            Log.err("command.permission.false")
            return false
        } else {
            return true
        }
    }
}