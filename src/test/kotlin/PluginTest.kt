import arc.*
import arc.backend.headless.HeadlessApplication
import arc.files.Fi
import arc.graphics.Camera
import arc.graphics.Color
import arc.util.CommandHandler
import arc.util.Log
import essentials.*
import essentials.Main.Companion.daemon
import essentials.Main.Companion.root
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.fail
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
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.text.MessageFormat
import java.util.*
import java.util.zip.ZipFile
import kotlin.io.path.Path

class PluginTest {
    companion object {
        private lateinit var main : Main
        private val r = Random()
        lateinit var player : Player
        lateinit var path : Fi
        val serverCommand : CommandHandler = CommandHandler("")
        val clientCommand : CommandHandler = CommandHandler("/")

        @Mock
        lateinit var mockApplication : Application

        fun loadGame() {
            if (System.getProperty("os.name").contains("Windows")) {
                val pathToBeDeleted : Path = Path("${System.getenv("AppData")}\\app").resolve("mods")
                if (File("${System.getenv("AppData")}\\app\\mods").exists()) {
                    Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map { obj : Path -> obj.toFile() }.forEach { obj : File -> obj.delete() }
                }
            }

            Core.settings = Settings()
            Core.settings.dataDirectory = Fi("")
            path = Core.settings.dataDirectory

            path.child("maps").deleteDirectory()

            path.child("locales").writeString("en")
            path.child("version.properties").writeString("modifier=release\ntype=official\nnumber=7\nbuild=custom build")

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
                HeadlessApplication(core) { throwable : Throwable? -> exceptionThrown[0] = throwable }
                while (!begins[0]) {
                    if (exceptionThrown[0] != null) {
                        Assert.fail(exceptionThrown[0]!!.stackTraceToString())
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
            } catch (r : Throwable) {
                Assert.fail(r.stackTraceToString())
            }
        }

        fun loadPlugin() {
            path.child("mods/Essentials").deleteDirectory()

            Config.databasePW = "pk1450"
            main = Main()

            Config.border = true
            Config.antiVPN = true
            Config.antiGrief = true
            Config.chatlimit = true
            Config.chatBlacklist = true
            Config.blockfooclient = true
            Config.webServer = true

            main.init()
            main.registerClientCommands(clientCommand)
            main.registerServerCommands(serverCommand)

            daemon.submit(Trigger.Client)

            Events.fire(ServerLoadEvent())
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


        fun newPlayer() : Pair<Player, DB.PlayerData> {
            val player = createPlayer()
            Events.fire(EventType.PlayerJoin(player))

            // Wait for database add time
            var time = 0
            while (Main.database.players.find { data -> data.uuid == player.uuid() } == null) {
                sleep(16)
                ++time
                if (time == 500) {
                    fail()
                }
            }
            return Pair(player, Main.database.players.find { data -> data.uuid == player.uuid() })
        }

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

        fun setPermission(group : String, admin : Boolean) {
            val json = JsonArray()
            val obj = JsonObject()
            obj.add("name", player.name())
            obj.add("uuid", player.uuid())
            obj.add("group", group)
            obj.add("admin", admin)
            json.add(obj)

            Core.settings.dataDirectory.child("mods/Essentials/permission_user.txt").writeString(json.toString())
            Permission.load()
        }

        fun err(key : String, vararg parameters : Any) : String {
            return "[scarlet]" + MessageFormat.format(Bundle().resource.getString(key), *parameters)
        }

        fun log(msg : String, vararg parameters : Any) : String {
            return MessageFormat.format(Bundle().resource.getString(msg), *parameters)
        }
    }

    @Test
    fun startPlugin() {
        loadGame()
        loadPlugin()
    }

    @Test
    fun dbUpgradeTest_14() {
        loadGame()

        // Copy Essentials 14.1 database
        val file = Paths.get("src", "test", "resources", "database-v0.db").toFile()
        val desc = File(Config.database + ".mv.db")
        file.copyRecursively(desc, true)
        println(Config.database)

        loadPlugin()
    }

    @Test
    fun dbUpgradeTest_18() {
        loadGame()

        // Copy Essentials 18.2 database
        val file = Paths.get("src", "test", "resources", "database-v1.db").toFile()
        val desc = root.child("database.mv.db").file()
        file.copyRecursively(desc, true)
        Config.database = "C:/Users/cloud/AppData/Roaming/app/mods/Essentials/database"
        println(Config.database)

        loadPlugin()
    }
}