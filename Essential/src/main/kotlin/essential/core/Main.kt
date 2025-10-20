package essential.core

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import essential.bridge.BridgeService
import essential.chat.ChatService
import essential.common.*
import essential.common.config.Config
import essential.common.database.data.createPluginData
import essential.common.database.data.getPluginData
import essential.common.database.data.migrateMapRatingsFromPluginData
import essential.common.database.databaseInit
import essential.common.permission.Permission
import essential.common.service.fileWatchService
import essential.core.generated.registerGeneratedClientCommands
import essential.core.generated.registerGeneratedEventHandlers
import essential.core.generated.registerGeneratedServerCommands
import essential.protect.ProtectService
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mindustry.Vars
import mindustry.Vars.state
import mindustry.game.EventType.WorldLoadEvent
import mindustry.game.Team
import mindustry.mod.Plugin
import mindustry.net.Administration
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import java.util.*

class Main : Plugin() {
    companion object {
        const val CONFIG_PATH = "config/config"
        internal lateinit var conf: CoreConfig
        
        private var bridgeBridgeService: BridgeService? = null
        private var chatChatService: ChatService? = null
        private var protectProtectService: ProtectService? = null
        private var achievementsAchievementsMain: essential.achievements.Main? = null
        private var discordDiscordMain: essential.discord.Main? = null

        val scope = CoroutineScope(Dispatchers.IO)
    }

    override fun init() {
        // 플러그인 언어 설정 및 태그 추가
        bundle.prefix = "[Essential]"

        Log.debug(bundle["event.plugin.starting"])

        // 플러그인 설정 불러오기
        val config = Config.load("config", CoreConfig.serializer(), CoreConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
        }

        conf = config

        bundle.locale = Locale(conf.plugin.lang)

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
                data = createPluginData()
            }

            pluginData = data

            // Migrate map ratings from PluginData to the new MapRating table
            try {
                Log.info("Migrating map ratings to the new database table...")
                migrateMapRatingsFromPluginData(data)
                Log.info("Map ratings migration completed successfully.")
            } catch (e: Exception) {
                Log.err("Error migrating map ratings: ${e.message}")
                e.printStackTrace()
            }

            // 권한 기능 설정
            Permission.load()
        }

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

        // Optional module initialization based on CoreConfig
        try {
            if (conf.plugin.enableBridge) {
                bridgeBridgeService = BridgeService().also { it.init() }
            }
            if (conf.plugin.enableChat) {
                chatChatService = ChatService().also { it.init() }
            }
            if (conf.plugin.enableProtect) {
                protectProtectService = ProtectService().also { it.init() }
            }
            if (conf.plugin.enableAchievements) {
                achievementsAchievementsMain = essential.achievements.Main().also { it.init() }
            }
            if (conf.plugin.enableDiscord) {
                discordDiscordMain = essential.discord.Main().also { it.init() }
            }
        } catch (e: Exception) {
            Log.err(bundle["event.plugin.load.failed"], e)
        }

        Vars.netServer.admins.addActionFilter(object : Administration.ActionFilter {
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
        registerGeneratedServerCommands(handler)
        // todo 명령어 제외 기능 추가
    }


    override fun registerClientCommands(handler: CommandHandler) {
        val commandClass = Class.forName("arc.util.CommandHandler\$Command")
        val runnerField = commandClass.getDeclaredField("runner")
        runnerField.isAccessible = true

        val vote = Vars.netServer.clientCommands.commandList.find { command -> command.text.equals("vote", true) }
        val votekick = Vars.netServer.clientCommands.commandList.find { command -> command.text.equals("votekick", true) }

        registerGeneratedClientCommands(handler)

        if (!conf.feature.vote.enabled && vote != null) {
            val voteRunner = runnerField.get(vote)
            handler.register(vote.text, vote.paramText, vote.description, voteRunner as CommandHandler.CommandRunner<*>)

            if (conf.feature.vote.enableVotekick && votekick != null) {
                val votekickRunner = runnerField.get(votekick)
                handler.register(votekick.text, votekick.paramText, votekick.description, votekickRunner as CommandHandler.CommandRunner<*>)
            } else {
                handler.removeCommand("votekick")
            }
        } else {
            if (!conf.feature.vote.enableVotekick) {
                handler.removeCommand("votekick")
            }
        }

        // Delegate command registration to optional modules
        if (conf.plugin.enableBridge) {
            if (bridgeBridgeService == null) bridgeBridgeService = BridgeService()
            bridgeBridgeService?.registerClientCommands(handler)
        }
        if (conf.plugin.enableChat) {
            if (chatChatService == null) chatChatService = ChatService()
            chatChatService?.registerClientCommands(handler)
        }
        if (conf.plugin.enableProtect) {
            if (protectProtectService == null) protectProtectService = ProtectService()
            protectProtectService?.registerClientCommands(handler)
        }
        if (conf.plugin.enableAchievements) {
            if (achievementsAchievementsMain == null) achievementsAchievementsMain = essential.achievements.Main()
            achievementsAchievementsMain?.registerClientCommands(handler)
        }
        if (conf.plugin.enableDiscord) {
            if (discordDiscordMain == null) discordDiscordMain = essential.discord.Main()
            discordDiscordMain?.registerClientCommands(handler)
        }
    }

    private fun checkUpdate() {
        if (conf.plugin.autoUpdate) {
            Http.get("https://api.github.com/repos/kieaer/Essentials/releases/latest").timeout(1000)
                .error { _ -> Log.warn(bundle["event.plugin.update.check.failed"]) }
                .block {
                    if (it.status == Http.HttpStatus.OK) {
                        val json = Json { ignoreUnknownKeys = true; isLenient = true }
                        val jsonObject = json.parseToJsonElement(it.resultAsString).jsonObject
                        
                        val tagName = jsonObject["tag_name"]?.jsonPrimitive?.content ?: PLUGIN_VERSION
                        val latest = DefaultArtifactVersion(tagName)
                        val current = DefaultArtifactVersion(PLUGIN_VERSION)
                        
                        // Parse assets array and get download URL from first asset
                        val browserDownloadUrl = jsonObject["assets"]?.jsonArray?.getOrNull(0)?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content ?: ""
                        val body = jsonObject["body"]?.jsonPrimitive?.content ?: ""

                        when {
                            latest > current -> Log.info(bundle["config.update.new", browserDownloadUrl, body])
                            latest.compareTo(current) == 0 -> Log.info(bundle["config.update.current"])
                            latest < current -> Log.info(bundle["config.update.devel"])
                        }
                    }
                }
        }
    }
}
