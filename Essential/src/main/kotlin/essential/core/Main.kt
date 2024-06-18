package essential.core

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import com.charleskorn.kaml.Yaml
import essential.core.Event.findPlayerData
import essential.core.annotation.ClientCommand
import essential.core.annotation.ServerCommand
import mindustry.Vars
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Playerc
import mindustry.mod.Plugin
import mindustry.net.Administration
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.SocketException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions


class Main : Plugin() {
    companion object {
        const val CONFIG_PATH = "config/config.yaml"
        lateinit var conf: Config

        @JvmField
        val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        @JvmField
        val players : MutableList<DB.PlayerData> = mutableListOf()
        @JvmField
        val database = DB()
        @JvmField
        val daemon: ExecutorService = Executors.newFixedThreadPool(2)

        fun currentTime() : String {
            return ZonedDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(Locale.getDefault()))
        }
    }

    val bundle = Bundle()

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

        if (!root.child("data").exists()) {
            root.child("data").mkdirs()
        }

        // 채팅 금지어 추가
        if (!root.child("chat_blacklist.txt").exists()) {
            root.child("chat_blacklist.txt").writeString("않")
        }

        // DB 설정
        database.load()
        database.connect()
        database.create()

        // 데이터 설정
        PluginData.load()

        // 업데이트 확인
        checkUpdate()

        // 권한 기능 설정
        Permission.load()

        Vars.netServer.admins.addActionFilter { e ->
            if (e.player == null) return@addActionFilter true
            val data = database.players.find { it.uuid == e.player.uuid() }
            val isHub = PluginData["hubMode"]
            for (it in PluginData.warpBlocks) {
                if (it != null && e.tile != null && it.mapName == Vars.state.map.name() && it.x.toShort() == e.tile.x && it.y.toShort() == e.tile.y && it.tileName == e.tile.block().name) {
                    return@addActionFilter false
                }
            }

            if (Vars.state.rules.pvp && conf.feature.pvp.autoTeam && e.player.team() == Team.derelict) {
                return@addActionFilter false
            }

            if (data != null) {
                if (e.type == Administration.ActionType.commandUnits) {
                    data.currentControlCount += e.unitIDs.size
                }

                return@addActionFilter when {
                    isHub != null && isHub == Vars.state.map.name() -> {
                        Permission.check(data, "hub.build")
                    }
                    data.strict -> {
                        false
                    }
                    Config.authType == Config.AuthType.Discord && data.discord.isNullOrEmpty() -> {
                        e.player.sendMessage(Bundle(e.player.locale)["event.discord.not.registered"])
                        false
                    }
                    else -> {
                        true
                    }
                }
            }
            return@addActionFilter false
        }

        Core.app.addListener(object: ApplicationListener {
            override fun dispose() {
                if (connectType) {
                    Trigger.clients.forEach {
                        val writer = BufferedWriter(OutputStreamWriter(it.getOutputStream()))
                        try {
                            writer.write("exit")
                            writer.newLine()
                            writer.flush()
                            it.close()
                        } catch (e : SocketException) {
                            it.close()
                            Trigger.clients.remove(it)
                        }
                    }
                    Trigger.Server.shutdown()
                } else {
                    Trigger.Client.send("exit")
                }
                daemon.shutdownNow()
                Permission.sort()
                Config.save()
                webServer.stop()
                if (Config.webServer) webServer.stop()
            }
        })

        if (!Fi(Config.banList).exists()) {
            val data = JsonArray()
            Vars.netServer.admins.banned.forEach {
                val json = JsonObject()
                json.add("id", it.id)

                val ips = JsonArray()
                for (a in it.ips) {
                    ips.add(a)
                }
                json.add("ip", ips)

                val names = JsonArray()
                for (a in it.names) {
                    names.add(a)
                }

                json.add("name", names)
            }

            Fi(Config.banList).writeString(data.toString(Stringify.HJSON))
        } else {
            Vars.netServer.admins.playerInfo.values().forEach(Consumer { info : Administration.PlayerInfo -> info.banned = false })
            for (bans in JsonArray.readHjson(Fi(Config.banList).readString()).asArray()) {
                val data = bans.asObject()
                val id = data["id"].asString()
                val ips = data["ip"].asArray()
                Vars.netServer.admins.playerInfo.values().find { a -> a.id == id }?.banned = true
                for (ip in ips) {
                    Vars.netServer.admins.playerInfo.values().find { a -> a.lastIP == ip.asString() }?.banned = true
                }
            }

            Vars.netServer.admins.save()
        }

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