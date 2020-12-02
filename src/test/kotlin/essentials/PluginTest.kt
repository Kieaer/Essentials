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
import mindustry.Vars.state
import mindustry.Vars.world
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.UnitTypes
import mindustry.core.FileTree
import mindustry.core.GameState
import mindustry.core.Logic
import mindustry.core.NetServer
import mindustry.entities.Units
import mindustry.entities.units.BuildPlan
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.maps.Map
import mindustry.net.Net
import org.hjson.JsonObject
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.junit.contrib.java.lang.system.SystemOutRule
import java.io.*
import java.lang.Thread.sleep
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

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
                HeadlessApplication(core, 60f) { throwable: Throwable? -> exceptionThrown[0] = throwable }
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
            state.set(GameState.State.playing)
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

        @AfterClass
        @JvmStatic
        fun shutdown() {
            Core.app.listeners[0].dispose()
            Client.request(Client.Request.Exit, null, null)
            Server.shutdown()
        }
    }

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
        if(state.map.name() == "Glacier"){
            player.set(648f,312f)
        } else {
            player.set(912f,720f)
        }
        clientHandler.handleMessage("/spawn-core small", player)
        assertSame(Blocks.coreShard, player.tileOn().block())
    }

    @Test
    fun client_setmech(){
        clientHandler.handleMessage("/setmech mace", player)
        assertSame(UnitTypes.mace, player.unit().type())
        clientHandler.handleMessage("/setmech dagger", player)
        assertSame(UnitTypes.dagger, player.unit().type())
        clientHandler.handleMessage("/setmech crawler", player)
        assertSame(UnitTypes.crawler, player.unit().type())
        clientHandler.handleMessage("/setmech fortress", player)
        assertSame(UnitTypes.fortress, player.unit().type())
        clientHandler.handleMessage("/setmech scepter", player)
        assertSame(UnitTypes.scepter, player.unit().type())
        clientHandler.handleMessage("/setmech reign", player)
        assertSame(UnitTypes.reign, player.unit().type())
        clientHandler.handleMessage("/setmech nova", player)
        assertSame(UnitTypes.nova, player.unit().type())
        clientHandler.handleMessage("/setmech pulsar", player)
        assertSame(UnitTypes.pulsar, player.unit().type())
        clientHandler.handleMessage("/setmech quasar", player)
        assertSame(UnitTypes.quasar, player.unit().type())
        clientHandler.handleMessage("/setmech vela", player)
        assertSame(UnitTypes.vela, player.unit().type())
        clientHandler.handleMessage("/setmech corvus", player)
        assertSame(UnitTypes.corvus, player.unit().type())
        clientHandler.handleMessage("/setmech atrax", player)
        assertSame(UnitTypes.atrax, player.unit().type())
        clientHandler.handleMessage("/setmech spiroct", player)
        assertSame(UnitTypes.spiroct, player.unit().type())
        clientHandler.handleMessage("/setmech arkyid", player)
        assertSame(UnitTypes.arkyid, player.unit().type())
        clientHandler.handleMessage("/setmech toxopid", player)
        assertSame(UnitTypes.toxopid, player.unit().type())
        clientHandler.handleMessage("/setmech flare", player)
        assertSame(UnitTypes.flare, player.unit().type())
        clientHandler.handleMessage("/setmech eclipse", player)
        assertSame(UnitTypes.eclipse, player.unit().type())
        clientHandler.handleMessage("/setmech horizon", player)
        assertSame(UnitTypes.horizon, player.unit().type())
        clientHandler.handleMessage("/setmech zenith", player)
        assertSame(UnitTypes.zenith, player.unit().type())
        clientHandler.handleMessage("/setmech antumbra", player)
        assertSame(UnitTypes.antumbra, player.unit().type())
        clientHandler.handleMessage("/setmech mono", player)
        assertSame(UnitTypes.mono, player.unit().type())
        clientHandler.handleMessage("/setmech poly", player)
        assertSame(UnitTypes.poly, player.unit().type())
        clientHandler.handleMessage("/setmech mega", player)
        assertSame(UnitTypes.mega, player.unit().type())
        clientHandler.handleMessage("/setmech quad", player)
        assertSame(UnitTypes.quad, player.unit().type())
        clientHandler.handleMessage("/setmech oct", player)
        assertSame(UnitTypes.oct, player.unit().type())
        clientHandler.handleMessage("/setmech alpha", player)
        assertSame(UnitTypes.alpha, player.unit().type())
        clientHandler.handleMessage("/setmech beta", player)
        assertSame(UnitTypes.beta, player.unit().type())
        clientHandler.handleMessage("/setmech gamma", player)
        assertSame(UnitTypes.gamma, player.unit().type())
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
        assertSame(dummy1.tileOn(), dummy2.tileOn())

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
        assertTrue(Vote.voting)
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
        assertFalse(Vote.voting)

        println("== vote gameover")
        clientHandler.handleMessage("/vote gameover", player)
        sleep(500)
        assertTrue(Vote.voting)
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
        assertFalse(Vote.voting)

        println("== vote skipwave")
        clientHandler.handleMessage("/vote skipwave 5", player)
        sleep(500)
        assertTrue(Vote.voting)
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
        assertFalse(Vote.voting)

        println("== vote rollback")
        serverHandler.handleMessage("save 1000")
        clientHandler.handleMessage("/vote rollback", player)
        sleep(500)
        assertTrue(Vote.voting)
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
        assertFalse(Vote.voting)

        println("== votemap")
        clientHandler.handleMessage("/vote map Glacier", player)
        sleep(500)
        assertTrue(Vote.voting)
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
        assertEquals("Glacier", state.map.name())
        assertFalse(Vote.voting)

        Events.fire(PlayerLeave(dummy1))
        Events.fire(PlayerLeave(dummy2))
        Events.fire(PlayerLeave(dummy3))
        Events.fire(PlayerLeave(dummy4))
        Events.fire(PlayerLeave(dummy5))
    }

    /*@Test
    fun client_weather() {
        clientHandler.handleMessage("/weather day", player)
        assertEquals(0.0f, state.rules.ambientLight.a, 0.0f)
        clientHandler.handleMessage("/weather eday", player)
        assertEquals(0.3f, state.rules.ambientLight.a, 0.0f)
        clientHandler.handleMessage("/weather night", player)
        assertEquals(0.7f, state.rules.ambientLight.a, 0.0f)
        clientHandler.handleMessage("/weather enight", player)
        assertEquals(0.85f, state.rules.ambientLight.a, 0.0f)
    }*/

    @Test
    fun client_mute() {
        val dummy = PluginTestDB.createNewPlayer(true)
        clientHandler.handleMessage("/mute " + dummy.name, player)
        assertTrue(PlayerCore[dummy.uuid()].mute)
        Events.fire(PlayerLeave(dummy))
    }

    @Test
    fun event_Tap(){
        Events.fire(TapEvent(player, world.tile(r.nextInt(50), r.nextInt(50))))
    }

    @Test
    fun event_Withdraw(){
        Events.fire(WithdrawEvent(world.tile(r.nextInt(50), r.nextInt(50)).build, player, Items.coal, 10))
    }

    @Test
    fun event_Gameover(){
        state.rules.attackMode = true
        Call.setRules(state.rules)
        Events.fire(GameOverEvent(player.team()))
        assertEquals(1, PlayerCore[player.uuid()].attackclear)
    }

    @Test
    fun event_WorldLoad(){
        Events.fire(WorldLoadEvent())
        assertEquals(0L, PluginVars.playtime)
    }

    @Test
    fun event_PlayerConnect(){
        Events.fire(PlayerConnect(player))
    }

    /*@Test
    fun event_Deposit(){
        Events.fire(DepositEvent(world.tile(r.nextInt(50), r.nextInt(50)).build, player, Items.copper, 5))
        // Miner is null
    }*/

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

    /*@Test
    fun event_BlockBuildEnd(){
        player.unit().addBuild(BuildPlan(5,5,0,Blocks.copperWall))
        Call.constructFinish(world.tile(5, 5), Blocks.copperWall, player.unit(), 0.toByte(), Team.sharded, false)
        Events.fire(BlockBuildEndEvent(world.tile(r.nextInt(50), r.nextInt(50)), player.unit(), Team.sharded, false, false))

        Call.constructFinish(world.tile(78, 78), Blocks.message, player.unit(), 0.toByte(), Team.sharded, false)
        Events.fire(BlockBuildEndEvent(world.tile(78, 78), player.unit(), Team.sharded, false, false))
        Tool.setMessage(world.tile(78, 78), "warp mindustry.kr")
    }*/

    @Test
    fun event_DeconstructFinish(){
        player.unit().clearBuilding()
        player.unit().removeBuild(5, 5, true)
        Call.deconstructFinish(world.tile(5, 5), Blocks.air, player.unit())
    }

    @Test
    fun event_BuildSelect(){
        Events.fire(BuildSelectEvent(world.tile(r.nextInt(50), r.nextInt(50)), Team.sharded, player.unit(), true))
        player.unit().clearBuilding()
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