package essentials

import arc.ApplicationCore
import arc.Core
import arc.Events
import arc.Settings
import arc.backend.headless.HeadlessApplication
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Log
import essentials.Main.Companion.pluginRoot
import essentials.features.Vote
import essentials.internal.Tool
import essentials.network.Client
import essentials.network.Server
import mindustry.Vars
import mindustry.Vars.world
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.core.FileTree
import mindustry.core.GameState
import mindustry.core.Logic
import mindustry.core.NetServer
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.maps.Map
import mindustry.net.Net
import org.hjson.JsonObject
import org.junit.Assert
import org.junit.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import java.io.*
import java.lang.Thread.sleep
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PluginTest {
    companion object {
        private val out = SystemOutRule()
        private val r = SecureRandom()
        private lateinit var root: Fi
        private lateinit var testroot: Fi
        private lateinit var main: Main
        private val serverHandler = CommandHandler("")
        private val clientHandler = CommandHandler("/")
        private lateinit var player: Player

        const val clean = true

        @BeforeClass
        @JvmStatic
        fun init() {
            out.enableLog()
            Core.settings = Settings()
            Core.settings.dataDirectory = Fi("")
            Core.settings.dataDirectory.child("locales").writeString("en")
            Core.settings.dataDirectory.child("version.properties").writeString("modifier=release\ntype=official\nnumber=5\nbuild=custom build")
            testroot = Core.settings.dataDirectory

            // Reset status
            if (clean) testroot.child("config/mods/Essentials").deleteDirectory()

            val testMap = arrayOfNulls<Map>(1)
            try {
                val begins = booleanArrayOf(false)
                val exceptionThrown = arrayOf<Throwable?>(null)
                Log.setUseColors(false)
                val core: ApplicationCore = object : ApplicationCore() {
                    override fun setup() {
                        Vars.headless = true
                        Vars.net = Net(null)
                        Vars.tree = FileTree()
                        Vars.init()
                        Vars.content.createBaseContent()
                        add(Logic().also { Vars.logic = it })
                        add(NetServer().also { Vars.netServer = it })
                        Vars.content.init()
                    }

                    override fun init() {
                        super.init()
                        begins[0] = true
                        testMap[0] = Vars.maps.loadInternalMap("maze")
                        Thread.currentThread().interrupt()
                    }
                }
                HeadlessApplication(core, null) { throwable: Throwable? -> exceptionThrown[0] = throwable }
                while (!begins[0]) {
                    if (exceptionThrown[0] != null) {
                        exceptionThrown[0]!!.printStackTrace()
                        fail()
                    }
                    sleep(10)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Core.settings.dataDirectory = Fi("config")
            root = Core.settings.dataDirectory.child("mods/Essentials")

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
            Groups.init()
            world.loadMap(testMap[0])
            Vars.state.set(GameState.State.playing)
            testroot.child("locales").delete()
            testroot.child("version.properties").delete()
            pluginRoot.child("config.hjson").writeString(testroot.child("src/test/kotlin/essentials/config.hjson").readString("UTF-8"))

            main = Main()
            main.init()
            main.registerServerCommands(serverHandler)
            main.registerClientCommands(clientHandler)

            player = PluginTestDB.createNewPlayer(true)
            serverHandler.handleMessage("setperm " + player.name + " owner")
        }
    }

    /*@Test
    @Order(999)
    fun shutdown() {
        Core.app.listeners[1].dispose()
        Client.request(Client.Request.Exit, null, null)
        sleep(1000)
        Server.shutdown()
    }*/

    @Test
    fun server_saveAll(){
        serverHandler.handleMessage("saveall")
    }

    @Test
    fun server_edit(){
        serverHandler.handleMessage("edit " + player.uuid() + " lastchat Manually")
        assertEquals("Manually", PlayerCore[player.uuid()].lastchat)
    }

    @Test
    fun server_gendocs(){
        pluginRoot.child("README.md").delete()
        serverHandler.handleMessage("gendocs")
        assertTrue(pluginRoot.child("README.md").exists())
    }

    @Test
    fun server_admin(){
        serverHandler.handleMessage("admin " + player.name)
        assertEquals("newadmin", PlayerCore[player.uuid()].permission)
    }

    @Test
    fun server_bansync(){
        serverHandler.handleMessage("bansync")
    }

    @Test
    fun server_info(){
        serverHandler.handleMessage("info " + player.uuid())
        assertNotEquals("Player not found!\n", out.logWithNormalizedLineSeparator)
    }

    @Test
    fun server_setperm(){
        serverHandler.handleMessage("setperm " + player.name + " owner")
        assertEquals("owner", PlayerCore[player.uuid()].permission)
    }

    @Test
    fun server_reload(){
        serverHandler.handleMessage("reload")
    }

    @Test
    fun client_alert(){
        clientHandler.handleMessage("/alert", player)
        assertTrue(PlayerCore[player.uuid()].alert)
    }

    @Test
    fun client_ch(){
        clientHandler.handleMessage("/ch", player)
        assertTrue(PlayerCore[player.uuid()].crosschat)
    }

    @Test
    fun client_changepw(){
        clientHandler.handleMessage("/changepw testpw123 testpw123", player)
        assertNotEquals("none", PlayerCore[player.uuid()].accountpw)
    }

    @Test
    @Throws(InterruptedException::class)
    fun client_chars() {
        clientHandler.handleMessage("/chars hobc0283qz ?!", player)
        assertSame(world.tile(player.tileX(), player.tileY()).block(), Blocks.copperWall)
    }

    @Test
    fun client_color() {
        clientHandler.handleMessage("/color", player)
        assertTrue(PlayerCore[player.uuid()].colornick)
    }

    @Test
    fun client_killall(){
        clientHandler.handleMessage("/killall", player)
    }

    @Test
    fun client_help(){
        clientHandler.handleMessage("/help", player)
    }

    @Test
    fun client_info(){
        clientHandler.handleMessage("/info", player)
    }

    @Test
    fun client_warp(){
        clientHandler.handleMessage("/warp count mindustry.indielm.com", player)
        assertEquals(1, PluginData.warpcounts.size.toLong())
        clientHandler.handleMessage("/warp zone mindustry.indielm.com 20 true", player)
        assertEquals(1, PluginData.warpzones.size.toLong())
        clientHandler.handleMessage("/warp total", player)
        assertEquals(1, PluginData.warptotals.size.toLong())
    }

    @Test
    fun client_kill(){
        val dummy = PluginTestDB.createNewPlayer(false)
        clientHandler.handleMessage("/kill " + dummy.name, player)
        assertTrue(dummy.dead())
        dummy.unit().kill()
        Events.fire(PlayerLeave(dummy))
    }

    @Test
    fun client_maps(){
        clientHandler.handleMessage("/maps", player)
    }

    @Test
    fun client_me(){
        clientHandler.handleMessage("/me It's me!", player)
    }

    @Test
    fun client_motd(){
        clientHandler.handleMessage("/motd", player)
    }

    @Test
    fun client_players(){
        clientHandler.handleMessage("/players", player)
    }

    @Test
    fun client_save(){
        clientHandler.handleMessage("/save", player)
    }

    @Test
    fun client_r(){
        val dummy = PluginTestDB.createNewPlayer(false)
        clientHandler.handleMessage("/r " + dummy.name + " Hi!", player)

        Events.fire(PlayerLeave(dummy))
    }

    @Test
    fun client_reset(){
        clientHandler.handleMessage("/reset count mindustry.indielm.com", player)
        assertEquals(0, PluginData.warpcounts.size.toLong())
        clientHandler.handleMessage("/reset zone mindustry.indielm.com", player)
        assertEquals(0, PluginData.warpzones.size.toLong())
        clientHandler.handleMessage("/reset total", player)
        assertEquals(0, PluginData.warptotals.size.toLong())
    }

    @Test
    fun client_spawn(){
        clientHandler.handleMessage("/spawn dagger 5 crux", player)
    }

    @Test
    fun client_setperm(){
        clientHandler.handleMessage("/setperm " + player.name + " newadmin", player)
        assertEquals("newadmin", PlayerCore[player.uuid()].permission)
    }

    @Test
    fun client_spawncore(){
        if(world.map.name().equals("Glacier")){
            player.set(648f,312f)
        } else {
            player.set(912f,720f)
        }
        clientHandler.handleMessage("/spawn-core small", player)
        assertSame(Blocks.coreShard, player.tileOn().block())
    }

    @Test
    fun client_setmech(){
        clientHandler.handleMessage("/setmech alpha", player)
        assertSame(Mechs.alpha, player.mech)
        clientHandler.handleMessage("/setmech dart", player)
        assertSame(Mechs.dart, player.mech)
        clientHandler.handleMessage("/setmech glaive", player)
        assertSame(Mechs.glaive, player.mech)
        clientHandler.handleMessage("/setmech javelin", player)
        assertSame(Mechs.javelin, player.mech)
        clientHandler.handleMessage("/setmech omega", player)
        assertSame(Mechs.omega, player.mech)
        clientHandler.handleMessage("/setmech tau", player)
        assertSame(Mechs.tau, player.mech)
        clientHandler.handleMessage("/setmech trident", player)
        assertSame(Mechs.trident, player.mech)
    }

    @Test
    fun client_status(){
        clientHandler.handleMessage("/status", player)
    }

    @Test
    fun client_suicide(){
        clientHandler.handleMessage("/suicide", player)
        assertTrue(player.dead())

        player.unit().kill() // Reset status
    }

    @Test
    fun client_team(){
        clientHandler.handleMessage("/team crux", player)
        assertSame(Team.crux, player.team())
    }

    @Test
    fun client_tempban() {
        val dummy = PluginTestDB.createNewPlayer(true)
        clientHandler.handleMessage("/tempban " + dummy.name + " 10 test", player)
        assertNotEquals(0L, PlayerCore[dummy.uuid()].bantime)

        Events.fire(PlayerLeave(dummy))
    }

    @Test
    fun client_time() {
        clientHandler.handleMessage("/time", player)
    }

    @Test
    fun client_tp() {
        val dummy = PluginTestDB.createNewPlayer(false)
        clientHandler.handleMessage("/tp " + dummy.name, player)
        assertTrue(player.x == dummy.x && player.y == dummy.y)

        Events.fire(PlayerLeave(dummy))
    }

    @Test
    fun client_tpp() {
        val dummy1 = PluginTestDB.createNewPlayer(false)
        val dummy2 = PluginTestDB.createNewPlayer(false)
        clientHandler.handleMessage("/tpp " + dummy1.name + " " + dummy2.name, player)
        assertTrue(dummy1.x == dummy2.x && dummy1.y == dummy2.y)

        Events.fire(PlayerLeave(dummy1))
        Events.fire(PlayerLeave(dummy2))
    }

    @Test
    fun client_tppos() {
        clientHandler.handleMessage("/tppos 50 50", player)
        assertTrue(player.x == 50f && player.y == 50f)
    }

    @Test
    fun client_vote() {
        val dummy1 = PluginTestDB.createNewPlayer(true)
        val dummy2 = PluginTestDB.createNewPlayer(true)
        val dummy3 = PluginTestDB.createNewPlayer(true)
        val dummy4 = PluginTestDB.createNewPlayer(true)
        val dummy5 = PluginTestDB.createNewPlayer(true)

        println("== votekick")
        clientHandler.handleMessage("/vote kick " + dummy3.id, player)
        sleep(500)
        assertTrue(Vote.service.process)
        Events.fire(PlayerChatEvent(player, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy1, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy2, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy4, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy5, "y"))
        sleep(1000)
        assertFalse(Vote.service.process)

        println("== vote gameover")
        clientHandler.handleMessage("/vote gameover", player)
        sleep(500)
        assertTrue(Vote.service.process)
        Events.fire(PlayerChatEvent(player, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy1, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy2, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy4, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy5, "y"))
        sleep(1000)
        assertFalse(Vote.service.process)

        println("== vote skipwave")
        clientHandler.handleMessage("/vote skipwave 5", player)
        sleep(500)
        assertTrue(Vote.service.process)
        Events.fire(PlayerChatEvent(player, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy1, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy2, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy4, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy5, "y"))
        sleep(1000)
        assertFalse(Vote.service.process)

        println("== vote rollback")
        serverHandler.handleMessage("save 1000")
        clientHandler.handleMessage("/vote rollback", player)
        sleep(500)
        assertTrue(Vote.service.process)
        Events.fire(PlayerChatEvent(player, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy1, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy2, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy4, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy5, "y"))
        sleep(1000)
        assertFalse(Vote.service.process)

        println("== votemap")
        clientHandler.handleMessage("/vote map Glacier", player);
        sleep(500)
        assertTrue(Vote.service.process)
        Events.fire(PlayerChatEvent(player, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy1, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy2, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy4, "y"))
        sleep(100)
        Events.fire(PlayerChatEvent(dummy5, "y"))
        sleep(1000)
        assertEquals("Glacier", world.map.name());
        assertFalse(Vote.service.process)

        Events.fire(PlayerLeave(dummy1))
        Events.fire(PlayerLeave(dummy2))
        Events.fire(PlayerLeave(dummy3))
        Events.fire(PlayerLeave(dummy4))
        Events.fire(PlayerLeave(dummy5))
    }

    @Test
    fun client_weather() {
        clientHandler.handleMessage("/weather day", player)
        assertEquals(0.0f, Vars.state.rules.ambientLight.a, 0.0f)
        clientHandler.handleMessage("/weather eday", player)
        assertEquals(0.3f, Vars.state.rules.ambientLight.a, 0.0f)
        clientHandler.handleMessage("/weather night", player)
        assertEquals(0.7f, Vars.state.rules.ambientLight.a, 0.0f)
        clientHandler.handleMessage("/weather enight", player)
        assertEquals(0.85f, Vars.state.rules.ambientLight.a, 0.0f)
    }

    @Test
    fun client_mute() {
        val dummy = PluginTestDB.createNewPlayer(true)
        clientHandler.handleMessage("/mute " + dummy.name, player)
        assertTrue(PlayerCore[dummy.uuid()].mute)

        Events.fire(PlayerLeave(dummy))
    }

    @Test
    fun event_TapConfig(){
        Events.fire(TapConfigEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, 5))
    }

    @Test
    fun event_Tap(){
        Events.fire(TapEvent(world.tile(r.nextInt(50), r.nextInt(50)), player))
    }

    @Test
    fun event_Withdraw(){
        Events.fire(WithdrawEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Items.coal, 10))
    }

    @Test
    fun event_Gameover(){
        Vars.state.rules.attackMode = true
        Call.setRules(Vars.state.rules)
        Events.fire(GameOverEvent(player.team))
        assertEquals(1, PlayerCore[player.uuid].attackclear)
    }

    @Test
    fun event_WorldLoad(){
        Events.fire(WorldLoadEvent())
        assertEquals(0L, PluginVars.playtime)
        assertEquals(0, PluginData.powerblocks.size)
    }

    @Test
    fun event_PlayerConnect(){
        Events.fire(PlayerConnect(player))
    }

    @Test
    fun event_Deposit(){
        Events.fire(DepositEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Items.copper, 5))
    }

    @Test
    fun event_PlayerJoin(){
        val dummy = PluginTestDB.createNewPlayer(false)
        Events.fire(PlayerJoin(dummy))
        clientHandler.handleMessage("/register hello testas123", dummy)
        clientHandler.handleMessage("/logout", dummy)

        Events.fire(PlayerLeave(dummy))
    }

    @Test
    fun event_PlayerJoin_Leave(){
        val dummy1 = PluginTestDB.createNewPlayer(false)
        Events.fire(PlayerJoin(dummy1))
        clientHandler.handleMessage("/register hello testas123", dummy1)
        clientHandler.handleMessage("/logout", dummy1)

        val dummy2 = PluginTestDB.createNewPlayer(false)
        Events.fire(PlayerJoin(dummy2))
        clientHandler.handleMessage("/login hello testas123", dummy2)
        assertTrue(PlayerCore[dummy2.uuid()].login)

        Events.fire(PlayerLeave(dummy2))
        assertTrue(PlayerCore[dummy2.uuid()].error)

        Events.fire(PlayerLeave(dummy1))
        Events.fire(PlayerLeave(dummy2))
    }

    @Test
    fun event_PlayerChat(){
        Events.fire(PlayerChatEvent(player, "hi"))
    }

    @Test
    fun event_BlockBuildEnd(){
        player.addBuildRequest(BuildRequest(5, 5, 0, Blocks.copperWall))
        Call.constructFinish(world.tile(5, 5), Blocks.copperWall, player.id, 0.toByte(), Team.sharded, false)
        Events.fire(BlockBuildEndEvent(world.tile(r.nextInt(50), r.nextInt(50)), player, Team.sharded, false))

        Call.constructFinish(world.tile(78, 78), Blocks.message, player.id, 0.toByte(), Team.sharded, false)
        Events.fire(BlockBuildEndEvent(world.tile(78, 78), player, Team.sharded, false))
        Call.setMessageBlockText(player, world.tile(78, 78), "warp mindustry.indielm.com")
    }

    @Test
    fun event_DeconstructFinish(){
        player.buildQueue().clear()
        player.addBuildRequest(BuildRequest(5, 5))
        Call.deconstructFinish(world.tile(5, 5), Blocks.air, player.id)
    }

    @Test
    fun event_BuildSelect(){
        Events.fire(BuildSelectEvent(world.tile(r.nextInt(50), r.nextInt(50)), Team.sharded, player, true))
        player.buildQueue().clear()
    }

    @Test
    fun event_UnitDestroy(){
        Events.fire(UnitDestroyEvent(player.unit()))
    }

    @Test
    fun event_PlayerBan(){
        Events.fire(PlayerBanEvent(player))
    }

    @Test
    fun event_PlayerIpBan(){
        Events.fire(PlayerIpBanEvent("127.0.0.3"))
    }

    @Test
    fun event_PlayerUnban(){
        Events.fire(PlayerUnbanEvent(player))
    }

    @Test
    fun event_PlayerIpUnban(){
        Events.fire(PlayerIpUnbanEvent("127.0.0.3"))
    }

    @Test
    fun event_ServerLoad() {
        Events.fire(ServerLoadEvent())
    }

    @Test
    fun network_online() {
        assertTrue(Config.clientEnable)
        assertTrue(Config.serverEnable)

        assertNotNull(Server.serverSocket)
        assertTrue(Client.activated)
    }

    @Test
    fun network_banSharing(){
        Client.request(Client.Request.BanSync, null, null)
        assertTrue(Client.activated)
        assertTrue(Server.list.size != 0)
        Client.request(Client.Request.Chat, player, "Cross-chat message!")
        Client.request(Client.Request.UnbanIP, null, "127.0.0.1")
        Client.request(Client.Request.UnbanID, null, player.uuid())
    }

    @Test
    fun network_banChecking(){
        try {
            Socket("127.0.0.1", 25000).use { socket ->
                val gen = KeyGenerator.getInstance("AES")
                gen.init(128)
                val key = gen.generateKey()
                val raw = key.encoded
                val skey: SecretKey = SecretKeySpec(raw, "AES")
                BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).use { `is` ->
                    DataOutputStream(socket.getOutputStream()).use { os ->
                        os.writeBytes(String(Base64.getEncoder().encode(raw))+"\n")
                        os.flush()
                        val json = JsonObject()
                        json.add("type", "CheckBan")
                        json.add("uuid", player.uuid())
                        json.add("ip", player.con.address)
                        val en = Tool.encrypt(json.toString(), skey)
                        os.writeBytes(en+"\n")
                        os.flush()
                        val receive = Tool.decrypt(`is`.readLine(), skey)
                        val kick = receive.toBoolean()
                        assertFalse(kick)
                    }
                }
            }
        } catch (ignored: Exception) {

        }
    }
}