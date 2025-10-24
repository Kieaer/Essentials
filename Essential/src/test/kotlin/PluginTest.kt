import arc.*
import arc.backend.headless.HeadlessApplication
import arc.files.Fi
import arc.graphics.Camera
import arc.graphics.Color
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.database.data.PlayerData
import essential.common.players
import essential.common.rootPath
import essential.core.Main
import mindustry.Vars
import mindustry.Vars.*
import mindustry.content.UnitTypes
import mindustry.core.*
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
import mindustry.world.Tile
import net.datafaker.Faker
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Paths
import java.text.MessageFormat
import java.util.*
import java.util.zip.ZipFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.fail

class PluginTest {
    companion object {
        private lateinit var main : Main
        private val r = Random()
        lateinit var player : Playerc
        lateinit var path : Fi
        val serverCommand : CommandHandler = CommandHandler("")
        val clientCommand : CommandHandler = CommandHandler("/")
        
        private var gameLoaded = false
        private var pluginLoaded = false

        @Mock
        lateinit var mockApplication : Application

        fun loadGame() {
            if (gameLoaded) return
            /*if (System.getProperty("os.name").contains("Windows")) {
                val pathToBeDeleted : Path = Paths.get("${System.getenv("AppData")}\\app").resolve("mods")
                if (File("${System.getenv("AppData")}\\app\\mods").exists()) {
                    Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map { obj : Path -> obj.toFile() }.forEach { obj : File -> obj.delete() }
                }
            }*/

            Core.settings = Settings()
            Core.settings.dataDirectory = Fi("")
            path = Core.settings.dataDirectory

            path.child("maps").deleteDirectory()
            path.child("scripts").deleteDirectory()

            path.child("locales").writeString("en", false)
            path.child("version.properties").writeString("modifier=release\ntype=official\nnumber=7\nbuild=custom build", false)

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
                Http.get("https://raw.githubusercontent.com/Anuken/Mindustry/refs/heads/master/core/assets/scripts/global.js").submit { res ->
                    path.child("scripts/global.js").writeString(res.resultAsString)
                }
                Http.get("https://raw.githubusercontent.com/Anuken/Mindustry/refs/heads/master/core/assets/scripts/base.js").submit { res ->
                    path.child("scripts/base.js").writeString(res.resultAsString)
                }
            }

            var testMap : Map? = null
            try {
                val begins = booleanArrayOf(false)
                val exceptionThrown = arrayOf<Throwable?>(null)
                Log.useColors = false
                val core : ApplicationCore = object: ApplicationCore() {
                    override fun setup() {
                        headless = true
                        net = Net(null)
                        tree = FileTree()
                        Vars.init()
                        world = object: World() {
                            override fun getDarkness(x : Int, y : Int) : Float {
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
                                        throw RuntimeException("error in file: " + cont.minfo.sourceFile.path(), cont.minfo.baseError)
                                    }
                                }
                            }
                        }
                    }

                    override fun init() {
                        super.init()
                        begins[0] = true
                        testMap = maps.loadInternalMap("groundZero")
                    }
                }
                HeadlessApplication(core) { throwable: Throwable? -> exceptionThrown[0] = throwable }
                while (!begins[0]) {
                    if (exceptionThrown[0] != null) {
                        fail(exceptionThrown[0]!!.stackTraceToString())
                    }
                    sleep(10)
                }

                Groups.init()
                // wait for map load
                while (testMap == null) {
                    sleep(10)
                }
                world.loadMap(testMap!!)
                state.set(GameState.State.playing)

                Version.build = 145
                Version.revision = 1

                path.child("locales").delete()
                path.child("version.properties").delete()

                Core.settings.put("debugMode", true)

                netClient = NetClient()
                Core.camera = Camera()
                gameLoaded = true
            } catch (r : Throwable) {
                fail(r.stackTraceToString())
            }
        }

        fun loadPlugin() {
            if (pluginLoaded) return
            path.child("mods/Essentials").deleteDirectory()

            main = Main()

            main.init()
            main.registerClientCommands(clientCommand)
            main.registerServerCommands(serverCommand)

            Events.fire(ServerLoadEvent())
            pluginLoaded = true
        }

        fun stopPlugin() {
            Core.app.exit()
            gameLoaded = false
            pluginLoaded = false
        }

        fun runPost() {
            MockitoAnnotations.openMocks(this)
            Core.app = mockApplication

            `when`(mockApplication.post(any(Runnable::class.java))).thenAnswer { invocation ->
                val task = invocation.getArgument(0) as Runnable
                task.run()
                null
            }
        }

        private fun getSaltString() : String {
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
        fun createPlayer() : Player {
            val player = Player.create()
            val faker = Faker(Locale.ENGLISH)
            val ip = r.nextInt(255).toString() + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255)

            player.reset()
            player.con = object: NetConnection(ip) {
                override fun send(`object` : Any?, reliable : Boolean) {
                    return
                }

                override fun close() {
                    return
                }
            }
            val name = faker.name().lastName().toCharArray()
            name.shuffle()
            player.name(name.concatToString())
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

        fun randomTile() : Tile {
            val random = Random()
            return world.tile(random.nextInt(100), random.nextInt(100))
        }

        /**
         * DB 에 계정이 등록된 플레이어 생성
         * @return 1번째 값에 플레이어, 2번째 값에 플레이어 정보
         */
        fun newPlayer() : Pair<Player, PlayerData> {
            val player = createPlayer()
            Events.fire(EventType.PlayerJoin(player))

            // Wait for database add time
            var time = 0
            while (players.find { data -> data.uuid == player.uuid() } == null) {
                sleep(16)
                ++time
                if (time == 500) {
                    fail()
                }
            }
            // todo 항상 데이터가 있는지 확인
            return Pair(player, players.find { data -> data.uuid == player.uuid() }!!)
        }

        /**
         * 대상 플레이어가 서버에서 나갔다고 하기
         * @param player 플레이어
         */
        fun leavePlayer(player : Playerc) {
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
        fun setPermission(group : String, admin : Boolean) {
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
        fun setPermission(player: Playerc, group : String, admin : Boolean) {
            serverCommand.handleMessage("setperm ${player.name()} $group")
            if (admin) {
                serverCommand.handleMessage("admin ${player.name()}")
            }
        }

        fun err(key : String, vararg parameters : Any) : String {
            return "[scarlet]" + Bundle().get(key, *parameters)
        }

        fun log(msg : String, vararg parameters : Any) : String {
            return Bundle().get(msg, *parameters)
        }
    }

    @Test
    fun startPlugin() {
        System.setProperty("test", "yes")

        loadGame()
        loadPlugin()
        stopPlugin()
    }

    @AfterTest
    fun resetEnv() {
        System.clearProperty("test")
    }

    @Test
    fun dbUpgradeTest_14() {
        loadGame()

        // Copy Essentials 14.1 database
        val file = Paths.get("src", "test", "resources", "database-v0.db").toFile()
        val desc = File("database.mv.db")
        file.copyRecursively(desc, true)

        loadPlugin()
        stopPlugin()
    }

    @Test
    fun dbUpgradeTest_18() {
        loadGame()

        // Copy Essentials 18.2 database
        val file = Paths.get("src", "test", "resources", "database-v1.db").toFile()
        val desc = rootPath.child("database.mv.db").file()
        file.copyRecursively(desc, true)
        // todo db 업글 확인
        /*Config.database = "C:/Users/cloud/AppData/Roaming/app/mods/Essentials/database"
        println(Config.database)*/

        loadPlugin()
        stopPlugin()
    }
}
