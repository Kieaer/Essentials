
import arc.ApplicationCore
import arc.Core
import arc.Settings
import arc.backend.headless.HeadlessApplication
import arc.files.Fi
import arc.graphics.Color
import arc.util.CommandHandler
import essentials.Main
import junit.framework.TestCase.assertNotNull
import mindustry.Vars
import mindustry.Vars.netServer
import mindustry.Vars.world
import mindustry.content.UnitTypes
import mindustry.core.*
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.maps.Map
import mindustry.net.Net
import mindustry.net.NetConnection
import mindustry.world.Tile
import net.datafaker.Faker
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import java.io.File
import java.lang.Thread.sleep
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipFile
import kotlin.io.path.Path


class PluginTest {
    companion object {
        private lateinit var main: Main
        private val r = Random()
        private lateinit var player: Playerc
        private lateinit var path: Fi
        private val serverCommand: CommandHandler = CommandHandler("")
        private val clientCommand: CommandHandler = CommandHandler("/")

        @BeforeClass
        @JvmStatic
        fun init() {
            if (System.getProperty("os.name").contains("Windows")) {
                val pathToBeDeleted: Path = Path("${System.getenv("AppData")}\\app").resolve("mods")
                if (File("${System.getenv("AppData")}\\app\\mods").exists()) {
                    Files.walk(pathToBeDeleted).sorted(Comparator.reverseOrder()).map { obj: Path -> obj.toFile() }.forEach { obj: File -> obj.delete() }
                }
            }

            Core.settings = Settings()
            Core.settings.dataDirectory = Fi("")
            path = Core.settings.dataDirectory

            path.child("locales").writeString("en")
            path.child("version.properties").writeString("modifier=release\ntype=official\nnumber=7\nbuild=custom build")

            if(!path.child("maps").exists()) {
                path.child("maps").mkdirs()

                ZipFile(Paths.get("src", "test", "resources", "maps.zip").toFile().absolutePath).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        if(entry.isDirectory) {
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

            val testMap = arrayOfNulls<Map>(1)
            try {
                val begins = booleanArrayOf(false)
                val exceptionThrown = arrayOf<Throwable?>(null)
                val core: ApplicationCore = object : ApplicationCore() {
                    override fun setup() {
                        Vars.headless = true
                        Vars.net = Net(null)
                        Vars.tree = FileTree()
                        Vars.init()
                        Vars.content.createBaseContent()
                        add(Logic().also { Vars.logic = it })
                        add(NetServer().also { netServer = it })
                        Vars.content.init()
                    }

                    override fun init() {
                        super.init()
                        begins[0] = true
                        testMap[0] = Vars.maps.loadInternalMap("maze")
                        Thread.currentThread().interrupt()
                    }
                }
                HeadlessApplication(core, 60f) { throwable: Throwable? -> exceptionThrown[0] = throwable }
                while(!begins[0]) {
                    if(exceptionThrown[0] != null) {
                        exceptionThrown[0]!!.printStackTrace()
                        Assert.fail()
                    }
                    sleep(10)
                }
            } catch(e: Exception) {
                e.printStackTrace()
            }

            Groups.init()
            world.loadMap(testMap[0])
            Vars.state.set(GameState.State.playing)
            Version.build = 128
            Version.revision = 0

            path.child("locales").delete()
            path.child("version.properties").delete()

            Core.settings.put("debugMode", true)

            main = Main()
            main.init()
            main.registerClientCommands(clientCommand)
            main.registerServerCommands(serverCommand)

            // 플레이어 생성
            player = createPlayer()

            // Call 오류 해결
            Vars.player = player as Player
            Vars.netClient = NetClient()
        }

        @AfterClass
        @JvmStatic
        fun shutdown() {
            path.child("mods/Essentials").deleteDirectory()
            path.child("maps").deleteDirectory()
        }

        private fun getSaltString(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"
            val salt = StringBuilder()
            while(salt.length < 25) {
                val index = (r.nextFloat() * chars.length).toInt()
                salt.append(chars[index])
            }
            return salt.toString()
        }

        private fun createPlayer(): Player {
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
            player.name(faker.name().lastName())
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
    }

    fun randomTile(): Tile {
        val random = Random()
        return world.tile(random.nextInt(100), random.nextInt(100))
    }
}