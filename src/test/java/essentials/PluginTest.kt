package essentials

import arc.ApplicationCore
import arc.Core
import arc.Events
import arc.Settings
import arc.backend.headless.HeadlessApplication
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Log
import arc.util.Time
import essentials.Main.Companion.configs
import essentials.Main.Companion.playerCore
import essentials.Main.Companion.pluginData
import essentials.Main.Companion.pluginRoot
import essentials.Main.Companion.pluginVars
import essentials.network.Client
import essentials.network.Server
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.Mechs
import mindustry.core.FileTree
import mindustry.core.GameState
import mindustry.core.Logic
import mindustry.core.NetServer
import mindustry.entities.traits.BuilderTrait.BuildRequest
import mindustry.entities.type.BaseUnit
import mindustry.entities.type.Player
import mindustry.game.Difficulty
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.maps.Map
import mindustry.net.Net
import org.hjson.JsonObject
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import java.io.*
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PluginTest {
    companion object {
        val out = SystemOutRule()
        val testVars = PluginTestVars()
        val r = SecureRandom()
        lateinit var root: Fi
        lateinit var testroot: Fi
        lateinit var main: Main
        var serverHandler = CommandHandler("")
        var clientHandler = CommandHandler("/")
        lateinit var player: Player

        @BeforeClass
        @JvmStatic
        fun init() {
            out.enableLog()
            Core.settings = Settings()
            Core.settings.dataDirectory = Fi("")
            Core.settings.dataDirectory.child("locales").writeString("en")
            Core.settings.dataDirectory.child("version.properties").writeString("modifier=release\ntype=official\nnumber=5\nbuild=custom build")
            testroot = Core.settings.dataDirectory
            val testMap = arrayOfNulls<Map>(1)
            try {
                val begins = booleanArrayOf(false)
                val exceptionThrown = arrayOf<Throwable?>(null)
                Log.setUseColors(false)
                val core: ApplicationCore = object : ApplicationCore() {
                    override fun setup() {
                        headless = true
                        net = Net(null)
                        tree = FileTree()
                        init()
                        content.createBaseContent()
                        add(Logic().also { logic = it })
                        add(NetServer().also { netServer = it })
                        content.init()
                    }

                    override fun init() {
                        super.init()
                        begins[0] = true
                        testMap[0] = maps.loadInternalMap("maze")
                        Thread.currentThread().interrupt()
                    }
                }
                HeadlessApplication(core, null) { throwable: Throwable? -> exceptionThrown[0] = throwable }
                while (!begins[0]) {
                    if (exceptionThrown[0] != null) {
                        exceptionThrown[0]!!.printStackTrace()
                        Assert.fail()
                    }
                    Thread.sleep(10)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Core.settings.dataDirectory = Fi("config")
            root = Core.settings.dataDirectory.child("mods/Essentials")

            // Reset status
            if (testVars.clean) testroot.deleteDirectory()
            try {
                FileInputStream("./build/libs/Essentials.jar").use { fis ->
                    FileOutputStream("./config/mods/Essentials.jar").use { fos ->
                        val availableLen = fis.available()
                        val buf = ByteArray(availableLen)
                        fis.read(buf)
                        fos.write(buf)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            playerGroup = entities.add(Player::class.java).enableMapping()
            world.loadMap(testMap[0])
            state.set(GameState.State.playing)
            Thread {
                while (true) {
                    Events.fire(Trigger.update)
                    try {
                        state.enemies = unitGroup.count { b: BaseUnit -> b.team === state.rules.waveTeam && b.countsAsEnemy() }
                        Time.update()
                        unitGroup.update()
                        puddleGroup.update()
                        shieldGroup.update()
                        bulletGroup.update()
                        tileGroup.update()
                        fireGroup.update()
                        collisions.collideGroups(bulletGroup, unitGroup)
                        collisions.collideGroups(bulletGroup, playerGroup)
                        unitGroup.updateEvents()
                        collisions.updatePhysics(unitGroup)
                        playerGroup.update()
                        effectGroup.update()
                        Thread.sleep(16)
                    } catch (ignored: InterruptedException) {
                    }
                }
            }.start()
            //testpluginRoot.child("locales").delete()
            //testpluginRoot.child("version.properties").delete()
            pluginRoot.child("config.hjson").writeString(testVars.config)
            main = Main()
            main.init()
            main.registerServerCommands(serverHandler)
            main.registerClientCommands(clientHandler)
            player = PluginTestDB.createNewPlayer(true)
        }

        @AfterClass
        @JvmStatic
        fun shutdown() {
            Core.app.listeners[1].dispose()
            Assert.assertTrue(out.logWithNormalizedLineSeparator.contains(configs.bundle["thread-disable-waiting"]))
        }
    }

    @Test
    @Order(1)
    fun configTest() {
        assertEquals(configs.dbUrl, "jdbc:h2:file:./config/mods/Essentials/data/player")
    }

    @Test
    @Order(2)
    fun networkTest() {
        try {
            val server = Server()
            val client = Client()

            // Server start test
            Main.mainThread.submit(server)
            Thread.sleep(1000)
            Assert.assertNotNull(server.serverSocket)

            // Client start test
            Main.mainThread.submit(client)
            client.wakeup()
            Thread.sleep(1000)
            Assert.assertTrue(client.activated)

            // Ban data sharing test
            client.request(Client.Request.BanSync, null, null)
            TimeUnit.SECONDS.sleep(1)
            Assert.assertTrue(client.activated)
            Assert.assertTrue(server.list.size != 0)
            out.clearLog()
            client.request(Client.Request.Chat, player, "Cross-chat message!")
            Assert.assertTrue(out.logWithNormalizedLineSeparator.contains("[EssentialClient]"))
            client.request(Client.Request.UnbanIP, null, "127.0.0.1")
            client.request(Client.Request.UnbanID, null, player.uuid)

            // Ban check test
            try {
                Socket("127.0.0.1", 25000).use { socket ->
                    val gen = KeyGenerator.getInstance("AES")
                    gen.init(128)
                    val key = gen.generateKey()
                    val raw = key.encoded
                    val skey: SecretKey = SecretKeySpec(raw, "AES")
                    BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).use { `is` ->
                        DataOutputStream(socket.getOutputStream()).use { os ->
                            os.writeBytes(String(Base64.getEncoder().encode(raw)).trimIndent())
                            os.flush()
                            val json = JsonObject()
                            json.add("type", "checkban")
                            json.add("target_uuid", player.uuid)
                            json.add("target_ip", player.con.address)
                            val en = Main.tool.encrypt(json.toString(), skey)
                            os.writeBytes(en.trimIndent())
                            os.flush()
                            val receive = Main.tool.decrypt(`is`.readLine(), skey)
                            val kick = receive.toBoolean()
                            Assert.assertFalse(kick)
                        }
                    }
                }
            } catch (ignored: Exception) {

            }

            // Connection close test
            client.request(Client.Request.Exit, null, null)
            Thread.sleep(1000)
            server.shutdown()
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    @Test
    fun test10_blank() {
    }

    @Test
    fun test11_serverCommand() {
        serverHandler.handleMessage("saveall")
        serverHandler.handleMessage("edit " + player.uuid + " lastchat Manually")
        assertEquals("Manually", playerCore[player.uuid].lastchat)
        pluginRoot.child("README.md").delete()
        serverHandler.handleMessage("gendocs")
        Assert.assertTrue(pluginRoot.child("README.md").exists())
        serverHandler.handleMessage("admin " + player.name)
        assertEquals("newadmin", playerCore[player.uuid].permission)
        serverHandler.handleMessage("bansync")
        serverHandler.handleMessage("info " + player.uuid)
        assertNotEquals("Player not found!\n", out.logWithNormalizedLineSeparator)
        serverHandler.handleMessage("setperm " + player.name + " owner")
        assertEquals("owner", playerCore[player.uuid].permission)
        serverHandler.handleMessage("reload")
    }

    @Test
    @Throws(InterruptedException::class)
    fun test12_clientCommand() {
        playerCore[player.uuid].level = 50
        clientHandler.handleMessage("/alert", player)
        Assert.assertTrue(playerCore[player.uuid].alert)
        clientHandler.handleMessage("/ch", player)
        Assert.assertTrue(playerCore[player.uuid].crosschat)
        clientHandler.handleMessage("/changepw testpw123 testpw123", player)
        assertNotEquals("none", playerCore[player.uuid].accountpw)
        clientHandler.handleMessage("/chars hobc0283qz ?!", player)
        Assert.assertSame(world.tile(player.tileX(), player.tileY()).block(), Blocks.copperWall)
        clientHandler.handleMessage("/color", player)
        Assert.assertTrue(playerCore[player.uuid].colornick)
        clientHandler.handleMessage("/difficulty easy", player)
        assertEquals(state.rules.waveSpacing.toDouble(), Difficulty.easy.waveTime * 60 * 60 * 2.toDouble(), 0.0)
        clientHandler.handleMessage("/killall", player)
        clientHandler.handleMessage("/help", player)
        clientHandler.handleMessage("/info", player)
        clientHandler.handleMessage("/warp count mindustry.indielm.com", player)
        assertEquals(1, pluginData.warpcounts.size.toLong())
        clientHandler.handleMessage("/warp zone mindustry.indielm.com 20 true", player)
        assertEquals(1, pluginData.warpzones.size.toLong())
        Thread.sleep(4000)
        clientHandler.handleMessage("/warp total", player)
        assertEquals(1, pluginData.warptotals.size.toLong())
        val dummy1 = PluginTestDB.createNewPlayer(true)
        clientHandler.handleMessage("/kill " + dummy1.name, player)
        Assert.assertTrue(dummy1.isDead)
        dummy1.isDead = false
        clientHandler.handleMessage("/maps", player)
        clientHandler.handleMessage("/me It's me!", player)
        clientHandler.handleMessage("/motd", player)
        clientHandler.handleMessage("/players", player)
        clientHandler.handleMessage("/save", player)
        clientHandler.handleMessage("/r " + dummy1.name + " Hi!", player)
        clientHandler.handleMessage("/reset count mindustry.indielm.com", player)
        assertEquals(0, pluginData.warpcounts.size.toLong())
        clientHandler.handleMessage("/reset zone mindustry.indielm.com", player)
        assertEquals(0, pluginData.warpzones.size.toLong())
        clientHandler.handleMessage("/reset total", player)
        assertEquals(0, pluginData.warptotals.size.toLong())

        //clientHandler.handleMessage("/register testacount testas123 testas123", player);
        clientHandler.handleMessage("/spawn dagger 5 crux", player)
        clientHandler.handleMessage("/setperm " + player.name + " newadmin", player)
        assertEquals("newadmin", playerCore[player.uuid].permission)
        serverHandler.handleMessage("setperm " + player.name + " owner")
        player[80f] = 80f
        player.setNet(80f, 80f)
        clientHandler.handleMessage("/spawn-core smail", player)
        Assert.assertSame(Blocks.coreShard, world.tileWorld(80f, 80f).block())
        clientHandler.handleMessage("/setmech alpha", player)
        Assert.assertSame(Mechs.alpha, player.mech)
        clientHandler.handleMessage("/setmech dart", player)
        Assert.assertSame(Mechs.dart, player.mech)
        clientHandler.handleMessage("/setmech glaive", player)
        Assert.assertSame(Mechs.glaive, player.mech)
        clientHandler.handleMessage("/setmech javelin", player)
        Assert.assertSame(Mechs.javelin, player.mech)
        clientHandler.handleMessage("/setmech omega", player)
        Assert.assertSame(Mechs.omega, player.mech)
        clientHandler.handleMessage("/setmech tau", player)
        Assert.assertSame(Mechs.tau, player.mech)
        clientHandler.handleMessage("/setmech trident", player)
        Assert.assertSame(Mechs.trident, player.mech)
        clientHandler.handleMessage("/status", player)
        clientHandler.handleMessage("/suicide", player)
        Assert.assertTrue(player.isDead)
        player.dead = false
        state.rules.pvp = true
        Call.onConstructFinish(world.tile(100, 40), Blocks.coreFoundation, 1, 0.toByte(), Team.crux, true)
        clientHandler.handleMessage("/team crux", player)
        Assert.assertSame(Team.crux, player.team)
        state.rules.pvp = false
        val dummy2 = PluginTestDB.createNewPlayer(true)
        clientHandler.handleMessage("/tempban " + dummy2.name + " 10 test", player)
        assertNotEquals(0L, playerCore[dummy2.uuid].bantime)
        clientHandler.handleMessage("/time", player)
        clientHandler.handleMessage("/tp " + dummy2.name, player)
        Assert.assertTrue(player.x == dummy2.x && player.y == dummy2.y)
        val dummy3 = PluginTestDB.createNewPlayer(true)
        clientHandler.handleMessage("/tpp " + dummy2.name + " " + dummy3.name, player)
        Assert.assertTrue(dummy2.x == dummy3.x && dummy2.y == dummy3.y)
        clientHandler.handleMessage("/tppos 50 50", player)
        Assert.assertTrue(player.x == 50f && player.y == 50f)
        val dummy4 = PluginTestDB.createNewPlayer(true)
        println("== votekick")
        clientHandler.handleMessage("/vote kick " + dummy4.id, player)
        Events.fire(PlayerChatEvent(player, "y"))
        TimeUnit.MILLISECONDS.sleep(150)
        Events.fire(PlayerChatEvent(dummy1, "y"))
        TimeUnit.MILLISECONDS.sleep(150)
        Events.fire(PlayerChatEvent(dummy3, "y"))
        TimeUnit.SECONDS.sleep(1)
        // Can't check player kicked

        // vote map check sucks
        /*System.out.println("== votemap");
        clientHandler.handleMessage("/vote map Glacier", player);
        Events.fire(new PlayerChatEvent(player, "y"));
        TimeUnit.MILLISECONDS.sleep(150);
        Events.fire(new PlayerChatEvent(dummy1, "y"));
        TimeUnit.MILLISECONDS.sleep(150);
        Events.fire(new PlayerChatEvent(dummy3, "y"));
        TimeUnit.SECONDS.sleep(1);
        assertEquals("Glacier", world.getMap().name());
        */println("== vote gameover")
        clientHandler.handleMessage("/vote gameover", player)
        Events.fire(PlayerChatEvent(player, "y"))
        TimeUnit.MILLISECONDS.sleep(150)
        Events.fire(PlayerChatEvent(dummy1, "y"))
        TimeUnit.MILLISECONDS.sleep(150)
        Events.fire(PlayerChatEvent(dummy3, "y"))
        TimeUnit.SECONDS.sleep(1)
        println("== vote rollback")
        serverHandler.handleMessage("save 1000")
        clientHandler.handleMessage("/vote rollback", player)
        TimeUnit.SECONDS.sleep(1)
        Events.fire(PlayerChatEvent(player, "y"))
        TimeUnit.MILLISECONDS.sleep(150)
        Events.fire(PlayerChatEvent(dummy1, "y"))
        TimeUnit.MILLISECONDS.sleep(150)
        Events.fire(PlayerChatEvent(dummy3, "y"))
        TimeUnit.SECONDS.sleep(1)
        println("== vote skipwave")
        clientHandler.handleMessage("/vote skipwave 5", player)
        Events.fire(PlayerChatEvent(player, "y"))
        TimeUnit.MILLISECONDS.sleep(150)
        Events.fire(PlayerChatEvent(dummy1, "y"))
        TimeUnit.MILLISECONDS.sleep(150)
        Events.fire(PlayerChatEvent(dummy3, "y"))
        TimeUnit.SECONDS.sleep(1)
        clientHandler.handleMessage("/weather day", player)
        assertEquals(0.0f, state.rules.ambientLight.a, 0.0f)
        clientHandler.handleMessage("/weather eday", player)
        assertEquals(0.3f, state.rules.ambientLight.a, 0.0f)
        clientHandler.handleMessage("/weather night", player)
        assertEquals(0.7f, state.rules.ambientLight.a, 0.0f)
        clientHandler.handleMessage("/weather enight", player)
        assertEquals(0.85f, state.rules.ambientLight.a, 0.0f)
        Assert.assertNotNull(playerGroup.find { p: Player -> p.uuid == dummy3.uuid })
        assertEquals("owner", playerCore[player.uuid].permission)
        clientHandler.handleMessage("/mute " + dummy3.name, player)
        Assert.assertTrue(playerCore[dummy3.uuid].mute)
    }

    @Test
    @Throws(InterruptedException::class)
    fun test13_events() {
        Events.fire(TapConfigEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, 5))
        Events.fire(TapEvent(world.tile(r.nextInt(50), r.nextInt(50)), player))
        Events.fire(WithdrawEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Items.coal, 10))
        state.rules.attackMode = true
        Call.onSetRules(state.rules)
        Events.fire(GameOverEvent(player.team))
        assertEquals(1, playerCore[player.uuid].attackclear)
        Events.fire(WorldLoadEvent())
        assertEquals(0L, pluginVars.playtime)
        assertEquals(0, pluginData.powerblocks.size)
        Events.fire(PlayerConnect(player))
        Events.fire(DepositEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Items.copper, 5))
        val dummy = PluginTestDB.createNewPlayer(false)
        Events.fire(PlayerJoin(dummy))
        clientHandler.handleMessage("/register hello testas123", dummy)
        clientHandler.handleMessage("/logout", dummy)
        val dummy2 = PluginTestDB.createNewPlayer(false)
        Events.fire(PlayerJoin(dummy2))
        clientHandler.handleMessage("/login hello testas123", dummy2)
        Assert.assertTrue(playerCore[dummy2.uuid].login)
        Events.fire(PlayerLeave(dummy2))
        Assert.assertTrue(playerCore[dummy2.uuid].error)
        Events.fire(PlayerChatEvent(player, "hi"))

        player.addBuildRequest(BuildRequest(5, 5, 0, Blocks.copperWall))
        Call.onConstructFinish(world.tile(5, 5), Blocks.copperWall, player.id, 0.toByte(), Team.sharded, false)
        Events.fire(BlockBuildEndEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Team.sharded, false))
        Call.onConstructFinish(world.tile(78, 78), Blocks.message, player.id, 0.toByte(), Team.sharded, false)
        Events.fire(BlockBuildEndEvent(world.tile(78, 78), player, Team.sharded, false))
        Call.setMessageBlockText(player, world.tile(78, 78), "warp mindustry.indielm.com")
        Thread.sleep(4000)
        player.buildQueue().clear()
        player.addBuildRequest(BuildRequest(5, 5))
        Call.onDeconstructFinish(world.tile(5, 5), Blocks.air, player.id)
        Events.fire(BuildSelectEvent(world.tile(r.nextInt(50), r.nextInt(50)), Team.sharded, player, true))
        player.buildQueue().clear()
        Events.fire(UnitDestroyEvent(player))
        Events.fire(PlayerBanEvent(dummy))
        Events.fire(PlayerIpBanEvent("127.0.0.3"))
        Events.fire(PlayerUnbanEvent(dummy))
        Events.fire(PlayerIpUnbanEvent("127.0.0.3"))
        Events.fire(ServerLoadEvent())
    }
}