import arc.*
import arc.backend.headless.HeadlessApplication
import arc.files.Fi
import arc.graphics.Camera
import arc.graphics.Color
import arc.util.CommandHandler
import arc.util.Log
import arc.util.Log.LogLevel
import essential.core.Bundle
import essential.core.Main
import junit.framework.TestCase.assertNotNull
import mindustry.Vars
import mindustry.content.UnitTypes
import mindustry.core.*
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.maps.Map
import mindustry.mod.Mod
import mindustry.net.Net
import mindustry.net.NetConnection
import mindustry.world.Tile
import net.datafaker.Faker
import org.junit.Assert
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.MessageFormat
import java.util.*
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.test.Test

class PluginTest {
    companion object {
        private lateinit var essential : Main
        private val r = Random()
        lateinit var player : Player
        lateinit var path : Fi
        val serverCommand : CommandHandler = CommandHandler("")
        val clientCommand : CommandHandler = CommandHandler("/")

        @Mock
        lateinit var mockApplication : Application

        fun loadGame() {
            Core.settings = Settings()
            Core.settings.dataDirectory = Fi("")
            Log.level = LogLevel.debug

            path = Core.settings.dataDirectory

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
                        Vars.headless = true
                        Vars.net = Net(null)
                        Vars.tree = FileTree()
                        Vars.init()
                        Vars.world = object: World() {
                            override fun getDarkness(x : Int, y : Int) : Float {
                                return 0F
                            }
                        }
                        Vars.content.createBaseContent()
                        Vars.mods.loadScripts()
                        Vars.content.createModContent()
                        add(Logic().also { Vars.logic = it })
                        add(NetServer().also { Vars.netServer = it })
                        Vars.content.init()
                        Vars.mods.eachClass(Mod::init)
                        if (Vars.mods.hasContentErrors()) {
                            for (mod in Vars.mods.list()) {
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
                        testMap = Vars.maps.loadInternalMap("groundZero")
                    }
                }
                HeadlessApplication(core) { throwable: Throwable? -> exceptionThrown[0] = throwable }
                while (!begins[0]) {
                    if (exceptionThrown[0] != null) {
                        Assert.fail(exceptionThrown[0]!!.stackTraceToString())
                    }
                    Thread.sleep(10)
                }

                Groups.init()
                // wait for map load
                while (testMap == null) {
                    Thread.sleep(10)
                }
                Vars.world.loadMap(testMap!!)
                Vars.state.set(GameState.State.playing)
                Version.build = 145
                Version.revision = 1

                path.child("locales").delete()
                path.child("version.properties").delete()

                Vars.netClient = NetClient()
                Core.camera = Camera()
            } catch (r : Throwable) {
                Assert.fail(r.stackTraceToString())
            }
        }

        fun loadPlugin() {
            path.child("mods/Essentials").deleteDirectory()

            essential = Main()

            essential.init()
            essential.registerClientCommands(clientCommand)
            essential.registerServerCommands(serverCommand)

            Events.fire(EventType.ServerLoadEvent())
        }

        fun cleanTest() {
            if (System.getProperty("os.name").contains("Windows")) {
                val pathToBeDeleted : Path = Path("${System.getenv("AppData")}\\app").resolve("mods")
                if (File("${System.getenv("AppData")}\\app\\mods").exists()) {
                    Files.walk(pathToBeDeleted)
                        .sorted(Comparator.reverseOrder()).map { obj : Path -> obj.toFile() }.forEach { obj : File -> obj.delete() }
                }
            }

            path.child("maps").deleteDirectory()
            path.child("essential_database.db").delete()
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
            Vars.netServer.admins.getInfo(player.uuid())
            Vars.netServer.admins.updatePlayerJoined(player.uuid(), player.con.address, player.name)
            Groups.player.update()

            assertNotNull(player)
            return player
        }

        fun randomTile() : Tile {
            val random = Random()
            return Vars.world.tile(random.nextInt(100), random.nextInt(100))
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
        try {
            loadPlugin()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cleanTest()
        }
    }
}