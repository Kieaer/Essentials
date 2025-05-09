package essential.core

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import essential.bundle
import essential.bundle.Bundle
import essential.config.Config
import essential.core.Event.actionFilter
import essential.core.Event.findPlayerData
import essential.ksp.ClientCommand
import essential.ksp.ServerCommand
import essential.core.generated.registerGeneratedServerCommands
import essential.core.generated.registerGeneratedClientCommands
import essential.database.data.PlayerData
import essential.database.data.PluginData
import essential.database.data.getPluginData
import essential.database.databaseInit
import essential.permission.Permission
import essential.rootPath
import essential.service.fileWatchService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mindustry.Vars
import mindustry.game.EventType.WorldLoadEvent
import mindustry.game.Team
import mindustry.gen.Playerc
import mindustry.mod.Plugin
import mindustry.net.Administration
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonValue
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation


class Main : Plugin() {
    companion object {
        const val CONFIG_PATH = "config/config.yaml"
        lateinit var conf: CoreConfig
        lateinit var pluginData: PluginData

        val scope = CoroutineScope(Dispatchers.IO)
    }

    private val clientCommandCache = mutableMapOf<KFunction<*>, (Commands, Playerc, PlayerData, Array<String>) -> Unit>()
    private val serverCommandCache = mutableMapOf<KFunction<*>, (Commands, Array<String>) -> Unit>()

    override fun init() {
        // 플러그인 언어 설정 및 태그 추가
        bundle.prefix = "[Essential]"
        bundle.locale = Locale.of(conf.plugin.lang)

        // 업데이트 확인
        checkUpdate()

        Log.debug(bundle["event.plugin.starting"])

        // 플러그인 설정 불러오기
        val config = Config.load("config", CoreConfig.serializer(), true, CoreConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
        }

        conf = config

        // 기록 및 데이터 폴더 생성
        rootPath.child("log").mkdirs()
        rootPath.child("data").mkdirs()

        // DB 설정
        // todo 이전 버전 db 업그레이드
        databaseInit(
            conf.plugin.database.url,
            conf.plugin.database.username,
            conf.plugin.database.password
        )

        // 플러그인 데이터 설정
        runBlocking {
            val data = getPluginData()
            require(data != null) {

            }
            pluginData = data
        }

        // 권한 기능 설정
        Permission.load()

        // 설정 파일 감시기능
        scope.launch {
            fileWatchService()
        }

        // 이벤트 등록
        for (functions in Event::class.declaredFunctions) {
            val annotation = functions.findAnnotation<essential.core.annotation.Event>()
            if (annotation != null) {
                functions.call(Event)
            }
        }

        // 스레드 등록
        val trigger = Trigger()
        trigger.register()
        daemon.submit(Trigger.Thread())
        daemon.submit(Trigger.UpdateThread())

        Vars.netServer.admins.addActionFilter(object : Administration.ActionFilter {
            var isNotTargetMap = false

            init {
                Events.on(WorldLoadEvent::class.java) {
                    isNotTargetMap = !isNotTargetMap && pluginData.warpBlocks.none { f -> f.mapName == Vars.state.map.name() }
                }
            }

            override fun allow(e: Administration.PlayerAction): Boolean {
                if (e.player == null) return true
                val data = database.players.find { it.uuid == e.player.uuid() }
                val isHub = pluginData["hubMode"]

                if (!isNotTargetMap) {
                    pluginData.warpBlocks.forEach {
                        if (it.mapName == pluginData.currentMap && e.tile != null && it.x.toShort() == e.tile.x && it.y.toShort() == e.tile.y && it.tileName == e.tile.block().name) {
                            return false
                        }
                    }
                }

                if (Vars.state.rules.pvp && conf.feature.pvp.autoTeam && e.player.team() == Team.derelict) {
                    return false
                }

                if (data != null) {
                    if (e.type == Administration.ActionType.commandUnits) {
                        data.currentControlCount += e.unitIDs.size
                    }

                    return when {
                        isHub != null && isHub == Vars.state.map.name() -> {
                            Permission.check(data, "hub.build")
                        }

                        data.strict -> {
                            false
                        }

                        else -> {
                            true
                        }
                    }
                }
                return false
            }
        }.also { listener -> actionFilter = listener })

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                daemon.shutdownNow()
            }
        })

        Log.info(bundle["event.plugin.loaded"])
    }

    override fun registerServerCommands(handler: CommandHandler) {
        // Call the generated function to register server commands
        registerGeneratedServerCommands(handler)

        // Legacy code for backward compatibility
        val commands = Commands()

        for (function in commands::class.declaredFunctions) {
            val annotation = function.findAnnotation<ServerCommand>()
            if (annotation != null) {
                val lambda = serverCommandCache.getOrPut(function) {
                    { instance, args ->
                        function.call(instance, args)
                    }
                }

                handler.register(annotation.name, annotation.parameter, annotation.description) { args ->
                    if (args.isNotEmpty()) {
                        lambda(commands, args)
                        function.call(commands, arrayOf(*args))
                    } else {
                        try {
                            function.call(commands, arrayOf<String>())
                        } catch (e: Exception) {
                            Log.err("arg size - ${args.size}")
                            Log.err("command - ${annotation.name}")
                        }
                    }
                }
            }
        }
    }


    override fun registerClientCommands(handler: CommandHandler) {
        // Call the generated function to register client commands
        registerGeneratedClientCommands(handler)

        // Legacy code for backward compatibility
        val commands = Commands()

        for (function in commands::class.declaredFunctions) {
            val annotation = function.findAnnotation<ClientCommand>()
            if (annotation != null) {
                val lambda = clientCommandCache.getOrPut(function) {
                    { instance, player, data, args ->
                        function.call(instance, player, data, args)
                    }
                }

                handler.register<Playerc>(annotation.name, annotation.parameter, annotation.description) { args, player ->
                    val data = findPlayerData(player.uuid()) ?: DB.PlayerData()
                    if (Permission.check(data, annotation.name)) {
                        lambda(commands, player, data, args)
                    } else {
                        if (annotation.name == "js") {
                            player.kick(Bundle(player.locale())["command.js.no.permission"])
                        } else {
                            data.send("command.permission.false")
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
                        pluginData.pluginVersion = JsonValue.readJSON(
                            this::class.java.getResourceAsStream("/plugin.json")!!.reader().readText()
                        ).asObject()["version"].asString()
                        val latest = DefaultArtifactVersion(json.getString("tag_name", pluginData.pluginVersion))
                        val current = DefaultArtifactVersion(pluginData.pluginVersion)

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
                    pluginData.pluginVersion = mod.meta.version
                    return@forEach
                }
            }
        }
    }
}
