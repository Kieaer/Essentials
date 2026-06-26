import arc.ApplicationCore
import arc.Core
import arc.Events
import arc.Settings
import arc.backend.headless.HeadlessApplication
import arc.files.Fi
import arc.graphics.Camera
import arc.graphics.Color
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import arc.util.Time
import essential.common.DATABASE_VERSION
import essential.common.bundle
import essential.common.bundle.Bundle
import essential.common.database.data.PlayerData
import essential.common.database.data.checkPlayerBanned
import essential.common.database.data.createPlayerData
import essential.common.database.data.getPlayerData
import essential.common.database.databaseClose
import essential.common.database.defaultDatabase
import essential.common.database.worldHistoryDatabase
import essential.common.players
import essential.common.rootPath
import essential.core.Main
import kotlinx.coroutines.runBlocking
import mindustry.Vars
import mindustry.Vars.*
import mindustry.content.UnitTypes
import mindustry.core.*
import mindustry.ctype.ContentType
import mindustry.game.EventType
import mindustry.game.EventType.ServerLoadEvent
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.maps.Map
import mindustry.mod.Mod
import mindustry.net.Net
import mindustry.net.NetConnection
import mindustry.world.Block
import mindustry.world.Tile
import net.datafaker.Faker
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PluginTest {
    companion object {
        private lateinit var main: Main
        private val r = Random()
        lateinit var player: Playerc
        lateinit var path: Fi
        val serverCommand: CommandHandler = CommandHandler("")
        val clientCommand: CommandHandler = CommandHandler("/")

        private var gameLoaded = false
        private var pluginLoaded = false

        private val baseLogHandler: Log.LogHandler = Log.logger

        var testMap: Map? = null

        @OptIn(ExperimentalPathApi::class)
        fun loadGame(loadPlugin: Boolean = false, deleteConfig: Boolean = true, logHandler: (String) -> Unit = {}, force: Boolean = false) {
            if (gameLoaded && !force) {
                if (loadPlugin) loadPlugin()
                return
            }
            if (System.getProperty("os.name").contains("Windows")) {
                val pathToBeDeleted: Path = Paths.get("${System.getenv("AppData")}\\app").resolve("mods")
                if (File("${System.getenv("AppData")}\\app\\mods").exists()) {
                    Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map { obj: Path -> obj.toFile() }
                        .forEach { obj: File -> obj.delete() }
                }
            }

            Core.settings = Settings()
            Core.settings.dataDirectory = Fi("")
            path = Core.settings.dataDirectory

            path.child("maps").deleteDirectory()
            path.child("scripts").deleteDirectory()
            if (deleteConfig) {
                path.child("config").deleteDirectory()
            }

            path.child("locales").writeString("en", false)
            path.child("version.properties")
                .writeString("modifier=release\ntype=official\nnumber=7\nbuild=custom build", false)

            if (!path.child("maps").exists()) {
                path.child("maps").mkdirs()

                ZipFile(Paths.get("src", "test", "resources", "maps.zip").toFile().absolutePath).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        if (entry.isDirectory) {
                            File(path.child("maps").absolutePath(), entry.name).mkdirs()
                        } else {
                            zip.getInputStream(entry).use { input ->
                                File(path.child("maps").absolutePath(), entry.name).outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }

            if (!path.child("scripts").exists()) {
                path.child("scripts").mkdirs()
                Http.get("https://raw.githubusercontent.com/Anuken/Mindustry/v157.2/core/assets/scripts/global.js")
                    .submit { res ->
                        path.child("scripts/global.js").writeString(res.resultAsString)
                    }
                Http.get("https://raw.githubusercontent.com/Anuken/Mindustry/v157.2/core/assets/scripts/base.js")
                    .submit { res ->
                        path.child("scripts/base.js").writeString(res.resultAsString)
                    }
            }

            if (loadPlugin) {
                path.child("mods/Essentials").deleteDirectory()
                path.child("config/mods/Essentials").deleteDirectory()
            }

            try {
                val begins = booleanArrayOf(false)
                val exceptionThrown = arrayOf<Throwable?>(null)
                Log.useColors = false

                val core: ApplicationCore = object : ApplicationCore() {
                    override fun setup() {
                        // Reset to the pristine logger first so prior tests' handlers don't stack.
                        Log.logger = baseLogHandler
                        val originalLogger = Log.logger
                        Log.logger = Log.LogHandler { level, text ->
                            originalLogger.log(level, text)
                            logHandler(text)
                            if (level == Log.LogLevel.err) {
                                throw RuntimeException("Error detected in logs: $text")
                            }
                        }
                        headless = true
                        net = Net(null)
                        tree = FileTree()
                        Vars.init()
                        world = object : World() {
                            override fun getDarkness(x: Int, y: Int): Float {
                                return 0F
                            }
                        }
                        content.createBaseContent()
                        mods.loadScripts()
                        content.createModContent()

                        add(Logic().also { logic = it })
                        add(NetServer().also { netServer = it })

                        content.init()

                        mods.eachClass(Mod::init)

                        if (mods.hasContentErrors()) {
                            for (mod in mods.list()) {
                                if (mod.hasContentErrors()) {
                                    for (cont in mod.erroredContent) {
                                        throw RuntimeException(
                                            "error in file: " + cont.minfo.sourceFile.path(),
                                            cont.minfo.baseError
                                        )
                                    }
                                }
                            }
                        }
                    }

                    override fun init() {
                        super.init()
                        if (loadPlugin) loadPlugin()

                        begins[0] = true
                        testMap = maps.loadInternalMap("serpulo/groundZero")
                        Thread.currentThread().interrupt()
                    }
                }
                HeadlessApplication(core) { throwable: Throwable? -> exceptionThrown[0] = throwable }
                while (!begins[0]) {
                    if (exceptionThrown[0] != null) {
                        fail(exceptionThrown[0]!!.stackTraceToString())
                    }
                    sleep(10)
                }

                val block: Block? = content.getByName(ContentType.block, "build2")
                assertEquals("build2", block?.name, "2x2 construct block doesn't exist?")

                // Reset status
                Time.setDeltaProvider { 1f }
                logic.reset()
                state.set(GameState.State.menu)

                // Avoid load errors
                Version.build = 146
                Version.revision = 1

                path.child("locales").delete()
                path.child("version.properties").delete()

                Core.settings.put("debugMode", true)

                netClient = NetClient()
                Core.camera = Camera()

                // Load map
                world.loadMap(testMap)
                logic.play()
                state.set(GameState.State.playing)
                state.rules.limitMapArea = false

                gameLoaded = true
            } catch (r: Throwable) {
                fail(r.stackTraceToString())
            }
        }

        fun loadPlugin(force: Boolean = false) {
            if (pluginLoaded && !force) return

            Main.conf = Main.conf.copy(
                module = Main.conf.module.copy(
                    achievement = true,
                    bridge = true,
                    chat = true,
                    discord = true,
                    protect = true,
                    web = true
                )
            )

            main = Main()

            main.init()

            main.registerClientCommands(clientCommand)
            main.registerServerCommands(serverCommand)

            Events.fire(ServerLoadEvent())
            pluginLoaded = true
        }

        fun stopPlugin() {
            Log.logger = baseLogHandler
            runBlocking {
                listOfNotNull(defaultDatabase, worldHistoryDatabase).forEach { db ->
                    try {
                        suspendTransaction(db = db) { exec("SHUTDOWN") }
                    } catch (_: Throwable) {
                    }
                }
            }
            databaseClose()

            val dataDir = Paths.get("config", "mods", "Essentials", "data")
            if (Files.exists(dataDir)) {
                try {
                    Files.walk(dataDir).use { stream ->
                        stream.filter { path ->
                            val name = path.fileName.toString()
                            (name.startsWith("database") || name.startsWith("worldHistory")) &&
                            path != dataDir
                        }.sorted(Comparator.reverseOrder()).forEach { path ->
                            path.toFile().delete()
                        }
                    }
                } catch (_: Throwable) {
                }
            }

            TransactionManager.defaultDatabase = null
            pluginLoaded = false
        }

        fun updateTick(times: Int) {
            repeat(times) {
                logic.update()
            }
        }

        fun updateTick(times: Int, codes: () -> Unit) {
            repeat(times) {
                logic.update()
                codes()
            }
        }

        private fun getSaltString(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"
            val salt = StringBuilder()
            while (salt.length < 25) {
                val index = (r.nextFloat() * chars.length).toInt()
                salt.append(chars[index])
            }
            return salt.toString()
        }

        /**
         * 플레이어 생성
         * @return 플레이어
         */
        @OptIn(ExperimentalTime::class)
        fun createPlayer(): Player {
            val player = Player.create()
            val faker = Faker(Locale.ENGLISH)
            val ip = r.nextInt(255).toString() + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255)

            player.reset()
            player.con = object : NetConnection(ip) {
                override fun send(`object`: Any?, reliable: Boolean) {
                    return
                }

                override fun close() {
                    return
                }
            }
            val name = faker.name().lastName() + Clock.System.now().toEpochMilliseconds()
            player.name(name)
            player.con.uuid = getSaltString()
            player.con.usid = getSaltString()
            player.set(r.nextInt(300).toFloat(), r.nextInt(500).toFloat())
            player.color.set(Color.rgb(r.nextInt(255), r.nextInt(255), r.nextInt(255)))
            player.color.a = r.nextFloat()
            player.team(Team.sharded)
            player.unit(UnitTypes.dagger.spawn(r.nextInt(300).toFloat(), r.nextInt(500).toFloat()))
            player.add()
            netServer.admins.getInfo(player.uuid())
            netServer.admins.updatePlayerJoined(player.uuid(), player.con.address, player.name)
            Groups.player.update()

            assertNotNull(player)
            return player
        }

        fun randomTile(): Tile {
            val random = Random()
            return world.tile(random.nextInt(100), random.nextInt(100))
        }

        /**
         * DB 에 계정이 등록된 플레이어 생성
         * @return 1번째 값에 플레이어, 2번째 값에 플레이어 정보
         */
        fun newPlayer(): Pair<Player, PlayerData> {
            val player = createPlayer()
            Events.fire(EventType.PlayerJoin(player))
            var data: PlayerData? = null
            val deadline = System.currentTimeMillis() + 15000
            while (data == null && System.currentTimeMillis() < deadline) {
                sleep(16)
                data = players.find { it.uuid == player.uuid() }
            }
            return Pair(player, data ?: fail("Player ${player.uuid()} was not registered within timeout"))
        }

        /**
         * 대상 플레이어가 서버에서 나갔다고 하기
         * @param player 플레이어
         */
        fun leavePlayer(player: Playerc) {
            NetServer.onDisconnect(player.self(), "Player leaved")
            Events.fire(EventType.PlayerLeave(player.self()))
            player.remove()
            Groups.player.update()

            // Wait for database save time
            while (Groups.player.find { a -> a.uuid() == player.uuid() } != null) {
                sleep(10)
            }

            sleep(500)
        }

        /**
         * 현재 유저의 권한을 변경함
         * @param group 그룹명 (visitor, user, admin, owner)
         * @param admin 관리자 유무 (true, false)
         */
        fun setPermission(group: String, admin: Boolean) {
            serverCommand.handleMessage("setperm ${player.name()} $group")
            if (admin) {
                serverCommand.handleMessage("admin ${player.name()}")
            }
        }

        /**
         * 대상 플레이어의 권한을 변경함
         * @param player 플레이어
         * @param group 그룹명 (visitor, user, admin, owner)
         * @param admin 관리자 유무 (true, false)
         */
        fun setPermission(player: Playerc, group: String, admin: Boolean) {
            serverCommand.handleMessage("setperm ${player.name()} $group")
            if (admin) {
                serverCommand.handleMessage("admin ${player.name()}")
            }
        }

        fun err(key: String, vararg parameters: Any): String {
            return "[scarlet]" + Bundle().get(key, *parameters)
        }

        fun log(msg: String, vararg parameters: Any): String {
            return Bundle().get(msg, *parameters)
        }

        /**
         * Waits until the given condition becomes true or the timeout elapses.
         */
        fun waitUntil(timeoutMs: Long = 2000, intervalMs: Long = 16, condition: () -> Boolean): Boolean {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < timeoutMs) {
                if (condition()) return true
                sleep(intervalMs)
            }
            return condition()
        }

        /**
         * Wait for a player's lastReceivedMessage to satisfy the predicate.
         * Returns the final message that satisfied the predicate (or the latest one when timed out).
         */
        fun waitForMessage(
            data: PlayerData,
            timeoutMs: Long = 2000,
            intervalMs: Long = 16,
            predicate: (String) -> Boolean
        ): String {
            val start = System.currentTimeMillis()
            var last = data.lastReceivedMessage
            while (System.currentTimeMillis() - start < timeoutMs) {
                val msg = data.lastReceivedMessage
                if (msg != last && predicate(msg)) return msg
                if (predicate(msg)) return msg
                last = msg
                sleep(intervalMs)
            }
            return data.lastReceivedMessage
        }
    }

    @AfterTest
    fun resetEnv() {
        System.clearProperty("test")
    }

    @Test
    fun dbUpgradeTest_20() {
        if (Core.app != null) stopPlugin()

        val dataDir = Paths.get("config", "mods", "Essentials", "data")
        dataDir.toFile().mkdirs()

        Files.walk(dataDir).use { stream ->
            stream.filter { path ->
                path.fileName.toString().startsWith("database") ||
                path.fileName.toString().startsWith("worldHistory")
            }.sorted(Comparator.reverseOrder()).forEach { path ->
                path.toFile().delete()
            }
        }

        val file = Paths.get("src", "test", "resources", "database-v3.mv.db").toFile()
        val target = dataDir.resolve("database.mv.db").toFile()
        file.copyTo(target, true)

        loadGame(deleteConfig = false, logHandler = {
            val alreadyUpgraded = bundle["database.upgrade.upToDate", DATABASE_VERSION]
            if (it.contains(alreadyUpgraded)) {
                fail("Upgrade logic not executed")
            }
        })

        loadPlugin()

        runBlocking {
            val uuid = "UPQJIWNSHAQAAAAAAAAAAA=="
            val player = getPlayerData(uuid)
            assertNotNull(player)
            assertEquals(56, player.exp)
            assertEquals(uuid, player.uuid)
            assertFalse(checkPlayerBanned(player.player))
            assertEquals(0, player.blockPlaceCount)
        }

        stopPlugin()
    }

    @Test
    fun dbUpgradeTest_20_postgres() {
        if (Core.app != null) stopPlugin()

        loadGame(deleteConfig = false)
        val originalConf = Main.conf
        PostgreSQLContainer("postgres:17.0").apply {
            withDatabaseName("essential")
            withUsername("plugins")
            withPassword("plugind")
            withInitScript("v3_postgres.sql")
            start()
        }.use { container ->
            try {
                val dbUrl = "postgresql://${container.host}:${container.firstMappedPort}/${container.databaseName}"
                rootPath.child("config/config.yaml").writeString(
                    """
                    plugin:
                      lang: en
                      autoUpdate: true
                      database:
                        url: $dbUrl
                        username: ${container.username}
                        password: ${container.password}
                    """.trimIndent(),
                    false
                )

                Main.conf = originalConf.copy(
                    plugin = originalConf.plugin.copy(
                        database = originalConf.plugin.database.copy(
                            url = dbUrl,
                            username = container.username,
                            password = container.password
                        )
                    )
                )

                loadGame()
                loadPlugin()

                runBlocking {
                    val dialect = defaultDatabase?.dialect
                    assertNotNull(dialect, "Database should be initialized")
                    assertIs<PostgreSQLDialect>(dialect)
                    val uuid = "hMHCIDJpHKQAAAAAbzCq5A=="
                    val player = getPlayerData(uuid) ?: createPlayerData("upgrade-test", uuid, "upgrade-test", "upgrade-test")
                    assertNotNull(player, "Player should exist after upgrade")
                    assertFalse(checkPlayerBanned(player.player))
                    assertEquals(122213, player.blockPlaceCount)
                }
            } finally {
                stopPlugin()
                Main.conf = originalConf
            }
        }
    }

    @Test
    fun verifyAllModulesTrueTest() {
        if (Core.app != null) stopPlugin()
        loadGame(loadPlugin = true)

        assertTrue(Main.conf.module.achievement, "achievement module should be true")
        assertTrue(Main.conf.module.bridge, "bridge module should be true")
        assertTrue(Main.conf.module.chat, "chat module should be true")
        assertTrue(Main.conf.module.discord, "discord module should be true")
        assertTrue(Main.conf.module.protect, "protect module should be true")
        assertTrue(Main.conf.module.web, "web module should be true")

        stopPlugin()
    }

    @Test
    fun configUpgradeTest() {
        if (Core.app != null) stopPlugin()

        loadGame(loadPlugin = false)

        val configDir = rootPath.child("config")
        configDir.mkdirs()

        val oldConfigs = listOf(
            "config_bridge.yaml",
            "config_chat.yaml",
            "config_discord.yaml",
            "config_protect.yaml",
            "config_web.yaml"
        )

        for (configName in oldConfigs) {
            val resourceStream = Companion::class.java.getResourceAsStream("/$configName")
            assertNotNull(resourceStream, "Resource /$configName should exist")
            val destFile = configDir.child(configName)
            destFile.write(resourceStream, false)
            resourceStream.close()
        }

        loadPlugin()

        assertEquals(32148, essential.core.service.web.WebService.conf.port)

        val updatedWebContent = configDir.child("config_web.yaml").readString()
        assertTrue(updatedWebContent.contains("sessionSecret"), "config_web.yaml should be upgraded with sessionSecret")
        assertTrue(updatedWebContent.contains("enableWebSocket"), "config_web.yaml should be upgraded with enableWebSocket")

        assertEquals("%player.name[orange] >[white] %chat", essential.core.service.chat.ChatService.conf.chatFormat)

        val updatedChatContent = configDir.child("config_chat.yaml").readString()
        assertTrue(updatedChatContent.contains("strict"), "config_chat.yaml should be upgraded with strict")
        assertTrue(updatedChatContent.contains("blacklist"), "config_chat.yaml should be upgraded with blacklist")

        stopPlugin()
    }
}
