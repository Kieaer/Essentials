package essential.core

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import essential.DATABASE_VERSION
import essential.PLUGIN_VERSION
import essential.bundle
import essential.config.Config
import essential.core.generated.registerGeneratedClientCommands
import essential.core.generated.registerGeneratedEventHandlers
import essential.core.generated.registerGeneratedServerCommands
import essential.database.data.DisplayData
import essential.database.data.PluginDataEntity
import essential.database.data.getPluginData
import essential.database.databaseInit
import essential.permission.Permission
import essential.players
import essential.rootPath
import essential.service.fileWatchService
import kotlinx.coroutines.*
import mindustry.Vars
import mindustry.Vars.state
import mindustry.game.EventType.WorldLoadEvent
import mindustry.game.Team
import mindustry.mod.Plugin
import mindustry.net.Administration
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class Main : Plugin() {
    companion object {
        const val CONFIG_PATH = "config/config.yaml"
        internal lateinit var conf: CoreConfig
        lateinit var pluginData: PluginDataEntity

        val scope = CoroutineScope(Dispatchers.IO)
    }

    private val clientCommandCache = CommandHandler("/")
    private val serverCommandCache = CommandHandler("")

    override fun init() {
        // 플러그인 언어 설정 및 태그 추가
        bundle.prefix = "[Essential]"

        Log.debug(bundle["event.plugin.starting"])

        // 플러그인 설정 불러오기
        val config = Config.load("config.yaml", CoreConfig.serializer(), true, CoreConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
        }

        conf = config

        bundle.locale = Locale.of(conf.plugin.lang)

        // 업데이트 확인
        checkUpdate()

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
            var data = getPluginData()
            if (data == null) {
                data = transaction {
                    PluginDataEntity.new {
                        pluginVersion = PLUGIN_VERSION
                        databaseVersion = DATABASE_VERSION
                        this.data = DisplayData()
                    }
                }
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
        registerGeneratedEventHandlers()

        // 스레드 등록
        val trigger = Trigger()
        trigger.register()
        scope.launch { Trigger.Thread().init() }

        Vars.netServer.admins.addActionFilter(object : Administration.ActionFilter {
            var isNotTargetMap = false

            init {
                Events.on(WorldLoadEvent::class.java) {
                    isNotTargetMap =
                        !isNotTargetMap && pluginData.data.warpBlock.none { f -> f.mapName == state.map.name() }
                }
            }

            override fun allow(e: Administration.PlayerAction): Boolean {
                if (e.player == null) return true
                val data = players.find { it.uuid == e.player.uuid() }
                val isHub = pluginData.hubMapName

                if (!isNotTargetMap) {
                    pluginData.data.warpBlock.forEach {
                        if (it.mapName == state.map.name() && e.tile != null && it.x.toShort() == e.tile.x && it.y.toShort() == e.tile.y && it.tileName == e.tile.block().name) {
                            return false
                        }
                    }
                }

                if (state.rules.pvp && conf.feature.pvp.autoTeam && e.player.team() == Team.derelict) {
                    return false
                }

                if (data != null) {
                    return when {
                        isHub != null && isHub == state.map.name() -> {
                            Permission.check(data, "hub.build")
                        }

                        data.strictMode -> {
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
                scope.cancel()
            }
        })

        Log.info(bundle["event.plugin.loaded"])
    }

    override fun registerServerCommands(handler: CommandHandler) {
        registerGeneratedServerCommands(serverCommandCache)
        // todo 명령어 제외 기능 추가
    }


    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(clientCommandCache)
        // todo 명령어 제외 기능 추가
    }

    private fun checkUpdate() {
        if (conf.plugin.autoUpdate) {
            Http.get("https://api.github.com/repos/kieaer/Essentials/releases/latest").timeout(1000)
                .error { _ -> Log.warn(bundle["event.plugin.update.check.failed"]) }
                .block {
                    if (it.status == Http.HttpStatus.OK) {
                        val jsonParser = ObjectMapper().configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                        val json = jsonParser.readTree(it.resultAsString)
                        pluginData.pluginVersion = PLUGIN_VERSION
                        val latest = DefaultArtifactVersion(json.get("tag_name").asText(pluginData.pluginVersion))
                        val current = DefaultArtifactVersion(pluginData.pluginVersion)

                        when {
                            latest > current -> Log.info(bundle["config.update.new", json.get("assets").get(0).get("browser_download_url").asText(), json.get("body").asText()])
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
