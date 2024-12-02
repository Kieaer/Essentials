import PluginTest.Companion.clientCommand
import PluginTest.Companion.createPlayer
import PluginTest.Companion.err
import PluginTest.Companion.leavePlayer
import PluginTest.Companion.loadGame
import PluginTest.Companion.loadPlugin
import PluginTest.Companion.log
import PluginTest.Companion.newPlayer
import PluginTest.Companion.player
import PluginTest.Companion.setPermission
import arc.Events
import essential.core.Bundle
import essential.core.DB
import essential.core.Event
import essential.core.Main.Companion.database
import junit.framework.TestCase.*
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.Liquids
import mindustry.game.EventType.GameOverEvent
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import net.datafaker.Faker
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import org.mindrot.jbcrypt.BCrypt
import java.lang.Thread.sleep

class ClientCommandTest {
    companion object {
        private var done = false
        lateinit var playerData: DB.PlayerData

        @BeforeClass
        @JvmStatic
        fun setup() {
            if (!done) {
                System.setProperty("test", "yes")

                loadGame()
                loadPlugin()

                val p = newPlayer()
                Vars.player = p.first.self()
                player = p.first.self()
                playerData = p.second

                done = true
            }
        }

        @After
        fun resetEnv() {
            System.clearProperty("test")
        }
    }

    @Test
    fun client_changemap() {
        fun reload() {
            val newPlayer = newPlayer()
            player = newPlayer.first
            playerData = newPlayer.second

            // Require admin or above permission
            setPermission("owner", true)
        }
        reload()

        // If map not found
        clientCommand.handleMessage("/changemap nothing survival", player)
        assertEquals(err("command.changemap.map.not.found", "nothing"), playerData.lastSentMessage)

        // Number method
        clientCommand.handleMessage("/changemap 0 survival", player)
        assertEquals("Ancient Caldera", Vars.state.map.name())
        reload()

        // Name method
        clientCommand.handleMessage("/changemap fork survival", player)
        assertEquals("Fork", Vars.state.map.name())
        reload()

        // If player enter wrong gamemode
        clientCommand.handleMessage("/changemap fork creative", player)
        assertEquals(err("command.changemap.mode.not.found", "creative"), playerData.lastSentMessage)

        // If player enter only map name
        clientCommand.handleMessage("/changemap glacier", player)
        assertEquals("Glacier", Vars.state.map.name())
        assertEquals(Gamemode.survival, Vars.state.rules.mode())
        reload()
    }

    @Test
    fun client_changename() {
        // Require admin or above permission
        setPermission("owner", true)

        // Change self name
        clientCommand.handleMessage("/changename ${player.name()} Kieaer", player)
        assertEquals("Kieaer", player.name())

        // Change other player name
        val registeredUser = newPlayer()
        val randomName = Faker().name().lastName()
        clientCommand.handleMessage("/changename ${registeredUser.first.name()} $randomName", player)
        assertEquals(randomName, database.players.find { p -> p.uuid == registeredUser.second.uuid }!!.name)
        leavePlayer(registeredUser.first)

        // If target player not found
        clientCommand.handleMessage("/changename yammi eat", player)
        assertEquals(err("player.not.found"), playerData.lastSentMessage)
    }

    @Test
    fun client_changepw() {
        // Require user or above permission
        setPermission("user", true)

        // Change password
        clientCommand.handleMessage("/changepw pass pass", player)
        assertTrue(BCrypt.checkpw("pass", playerData.accountPW))
        assertEquals(log("command.changepw.apply"), playerData.lastSentMessage)

        // If password isn't same
        clientCommand.handleMessage("/changepw pass wd", player)
        assertEquals(err("command.changepw.same"), playerData.lastSentMessage)
    }

    @Test
    fun client_chat() {
        // Require admin or above permission
        setPermission("admin", true)

        val dummy = newPlayer()

        // Set all players mute
        clientCommand.handleMessage("/chat off", player)
        assertNull(Vars.netServer.admins.filterMessage(dummy.first, "hello"))

        // But if player has chat.admin permission, still can chat
        assertNotNull(Vars.netServer.admins.filterMessage(player.self(), "hello"))

        // Set all players unmute
        clientCommand.handleMessage("/chat on", player)
        assertNotNull(Vars.netServer.admins.filterMessage(dummy.first, "yes"))

        leavePlayer(dummy.first)
    }

    fun client_chars() {
        // Require admin or above permission
        setPermission("admin", true)

        // todo chars 명령어
    }

    @Test
    fun client_color() {
        fun checkChanged(condition: Boolean): Boolean {
            for (i in 0 until 120) {
                sleep(16)
                if (player.name().contains("[#ff0000]") && condition) {
                    return true
                } else if (!player.name().contains("[#ff0000]") && !condition) {
                    return false
                }
            }
            fail()
            return false
        }
        // Require admin or above permission
        setPermission("admin", true)

        // Enable animated name
        clientCommand.handleMessage("/color", player)
        assertTrue(checkChanged(true))

        // Disable animated name
        clientCommand.handleMessage("/color", player)
        assertFalse(checkChanged(false))
    }

    fun client_discord() {
        // Require user or above permission
        setPermission("user", true)

        clientCommand.handleMessage("/discord", player)

        // todo mock discord
    }

    @Test
    fun client_dps() {
        // Require admin or above permission
        setPermission("admin", true)

        // Place damage per seconds meter block
        clientCommand.handleMessage("/dps", player)
        assertEquals(Blocks.thoriumWallLarge, player.tileOn().block())

        // Wait for show damage meter
        sleep(1250)

        // If block deleted
        Call.deconstructFinish(player.tileOn(), Blocks.air, player.unit())
        sleep(64)
        assertNull(Event.dpsTile)

        // Replace damage per seconds meter block
        clientCommand.handleMessage("/dps", player)
        assertEquals(Blocks.thoriumWallLarge, player.tileOn().block())

        // Remove damage per seconds meter block
        clientCommand.handleMessage("/dps", player)
        assertEquals(Blocks.air, player.tileOn().block())
    }

    @Test
    fun client_effect() {
        // Require user or above permission
        setPermission("user", true)
        playerData.exp = 100000

        // todo call effect mock

        // Disable all other player effects
        clientCommand.handleMessage("/effect off", player)

        // Enable all other player effects
        clientCommand.handleMessage("/effect on", player)

        // Use level-by-level effects manually
        clientCommand.handleMessage("/effect 10", player)

        // When setting a value higher than the current player level
        clientCommand.handleMessage("/effect 500", player)

        // Set effect color using hex
        clientCommand.handleMessage("/effect 10 #ffffff", player)

        // Set effect color using color name
        clientCommand.handleMessage("/effect 10 red", player)
    }

    @Test
    fun client_exp() {
        fun assert(expected: Int, actual: Int) : Boolean {
            for (i in 0 until 60) {
                sleep(16)
                if (expected == actual) {
                    return true
                }
            }
            return false
        }

        fun assertFalse(condition: Boolean) : Boolean {
            for (i in 0 until 60) {
                sleep(16)
                if (!condition) {
                    return true
                }
            }
            return false
        }

        fun assertTrue(condition: Boolean) : Boolean {
            for (i in 0 until 60) {
                sleep(16)
                if (condition) {
                    return true
                }
            }
            return false
        }

        fun assertHide(name: String, condition: Boolean) : Boolean {
            var next = true
            var buffer = playerData.lastSentMessage
            var count = 0
            var exists = false
            while (next) {
                clientCommand.handleMessage("/ranking exp $count", player)
                sleep(200)
                next = playerData.lastSentMessage != buffer
                buffer = playerData.lastSentMessage
                count++
                if (buffer.contains(name)) {
                    exists = true
                }
            }
            if (exists && condition) {
                return false
            } else if (!exists && !condition) {
                return false
            }
            return true
        }

        fun assertExp(uuid: String, exp: Int) : Boolean {
            for (time in 1..10) {
                if (database[uuid]!!.exp != exp) {
                    sleep(100)
                } else if (time == 10 && database[uuid]!!.exp != exp) {
                    return false
                }
            }
            return true
        }

        // Require owner permission
        setPermission("owner", true)

        // Set EXP value
        clientCommand.handleMessage("/exp set 1000", player)
        assertEquals(1000, playerData.exp)

        // Set another player EXP value
        val dummy = newPlayer()
        clientCommand.handleMessage("/exp set 500 ${dummy.first.name}", player)
        assertTrue(assert(500, dummy.second.exp))

        // If player enter wrong value
        clientCommand.handleMessage("/exp set number", player)
        assertEquals(err("command.exp.invalid"), playerData.lastSentMessage)

        // Hides player's rank in the ranking list
        clientCommand.handleMessage("/exp hide", player)
        database.update(player.uuid(), playerData)
        assertEquals(Bundle()["command.exp.ranking.hide"], playerData.lastSentMessage)
        clientCommand.handleMessage("/ranking exp", player)
        assertFalse(playerData.lastSentMessage.contains(player.name()))

        // Un-hides player's rank in the ranking list
        clientCommand.handleMessage("/exp hide", player)
        database.update(player.uuid(), playerData)
        assertEquals(Bundle()["command.exp.ranking.unhide"], playerData.lastSentMessage)
        clientCommand.handleMessage("/ranking exp", player)
        assertTrue(playerData.lastSentMessage.contains(player.name()))

        // Hide other players' rankings in the ranking list
        clientCommand.handleMessage("/exp hide ${dummy.first.name}", player)
        database.update(dummy.first.uuid(), dummy.second)
        assertEquals(Bundle()["command.exp.ranking.hide"], playerData.lastSentMessage)
        assertTrue(assertHide(dummy.first.name, true))

        // Un-hide other players' rankings in the ranking list
        clientCommand.handleMessage("/exp hide ${dummy.second.name}", player)
        database.update(dummy.first.uuid(), dummy.second)
        assertTrue(assertHide(dummy.first.name, false))

        // Add exp value
        clientCommand.handleMessage("/exp add 500", player)
        assertTrue(playerData.exp >= 1500)

        // Add other player exp value
        clientCommand.handleMessage("/exp add 500 ${dummy.first.name}", player)
        assertTrue(dummy.second.exp >= 1000)

        // Subtract value from current experience
        clientCommand.handleMessage("/exp remove 300", player)
        assertTrue(playerData.exp in 1200..1499)

        // Subtract the value from another player's current experience
        clientCommand.handleMessage("/exp remove 300 ${dummy.first.name}", player)
        assertTrue(dummy.second.exp in 700..999)

        // Set EXP for players who are not currently logged in
        leavePlayer(dummy.first)
        clientCommand.handleMessage("/exp set 10 ${dummy.first.name}", player)
        assertExp(dummy.first.uuid(), 10)

        // Add EXP for players who are not currently logged in
        clientCommand.handleMessage("/exp add 10 ${dummy.first.name}", player)
        assertExp(dummy.first.uuid(), 20)

        // Subtract EXP for players who are not currently logged in
        clientCommand.handleMessage("/exp remove 5 ${dummy.first.name}", player)
        assertExp(dummy.first.uuid(), 15)

        // If target player not found
        clientCommand.handleMessage("/exp set 10 dummy", player)
        assertEquals(err("player.not.found"), playerData.lastSentMessage)

        // If target player exist but not registered
        val bot = createPlayer()
        clientCommand.handleMessage("/exp set 10 ${bot.name}", player)
        assertEquals(err("player.not.registered"), playerData.lastSentMessage)

        // If the target player is not logged in and looking for a player that isn't in the database
        clientCommand.handleMessage("/exp hide 냠냠", player)
        assertEquals(err("player.not.found"), playerData.lastSentMessage)

        // If player enter wrong command
        clientCommand.handleMessage("/exp wrongCommand", player)
        assertEquals(err("command.exp.invalid.command"), playerData.lastSentMessage)
    }

    @Test
    fun client_fillitems() {
        // Require admin or above permission
        setPermission("admin", true)

        // Fill core items
        clientCommand.handleMessage("/fillitems", player)
        assertEquals(Vars.state.teams.cores(player.team()).first().storageCapacity, Vars.state.teams.cores(player.team()).first().items.get(Items.copper))

        // If player core doesn't exist
        Call.deconstructFinish(Vars.state.teams.cores(player.team()).first().tile, Blocks.air, player.unit())
        clientCommand.handleMessage("/fillitems", player)
        assertEquals(err("command.fillitems.core.empty"), playerData.lastSentMessage)

        // If target team core doesn't exist
        clientCommand.handleMessage("/fillitems green", player)
        assertEquals(err("command.fillitems.core.empty"), playerData.lastSentMessage)

        // If target team core exists
        Call.constructFinish(player.tileOn(), Blocks.coreShard, player.unit(), 0, Team.green, null)
        clientCommand.handleMessage("/fillitems green", player)
        assertEquals(Vars.state.teams.cores(Team.green).first().storageCapacity, Vars.state.teams.cores(Team.green).first().items.get(Items.copper))
    }

    @Test
    fun client_freeze() {
        // Require admin or above permission
        setPermission("admin", true)

        // Freeze target player
        val dummy = newPlayer()
        var oldX = dummy.first.unit().x
        var oldY = dummy.first.unit().y
        clientCommand.handleMessage("/freeze ${dummy.first.name}", player)
        dummy.first.unit().x = 24f
        dummy.first.unit().y = 24f
        for (time in 0..10) {
            if (dummy.first.unit().x != oldX || dummy.first.unit().y != oldY) {
                sleep(16)
            } else if (time == 10 && (dummy.first.unit().x != oldX || dummy.first.unit().y != oldY)) {
                fail()
            }
        }
        assertEquals(log("command.freeze.done", dummy.first.name), playerData.lastSentMessage)

        // Un-freeze target player
        oldX = dummy.first.unit().x
        oldY = dummy.first.unit().y
        clientCommand.handleMessage("/freeze ${dummy.first.name}", player)
        dummy.first.unit().x = 48f
        dummy.first.unit().y = 48f
        for (time in 0..10) {
            if (dummy.first.unit().x == oldX || dummy.first.unit().y == oldY) {
                sleep(16)
            } else if (time == 10 && (dummy.first.unit().x == oldX && dummy.first.unit().y == oldY)) {
                fail()
            }
        }
        assertEquals(log("command.freeze.undo", dummy.first.name), playerData.lastSentMessage)

        // If player exists but not registered
        val bot = createPlayer()
        clientCommand.handleMessage("/freeze ${bot.name}", player)
        assertEquals(err("player.not.registered"), playerData.lastSentMessage)

        // If player not found
        clientCommand.handleMessage("/freeze nothing", player)
        assertEquals(err("player.not.found"), playerData.lastSentMessage)
    }

    @Test
    fun client_gg() {
        // Require admin or above permission
        setPermission("admin", true)

        var winner = Team.derelict
        Events.on(GameOverEvent::class.java) {
            winner = it.winner
        }

        // No args
        clientCommand.handleMessage("/gg", player)
        while (Vars.state.gameOver) {
            sleep(16)
        }
        assertEquals(Vars.state.rules.waveTeam, winner)

        // Select win team
        clientCommand.handleMessage("/gg green", player)
        while (Vars.state.gameOver) {
            sleep(16)
        }
        assertEquals(Team.green, winner)

        // If only the first letter of the team name is entered
        clientCommand.handleMessage("/gg b", player)
        while (Vars.state.gameOver) {
            sleep(16)
        }
        assertEquals(Team.blue, winner)

        // If player select wrong team
        clientCommand.handleMessage("/gg ah!", player)
        while (Vars.state.gameOver) {
            sleep(16)
        }
        assertEquals(player.team(), winner)
    }

    @Test
    fun client_god() {
        // Require admin or above permission
        setPermission("admin", true)

        Vars.state.rules.infiniteResources = true
        Vars.state.rules.attackMode = true

        // Wait for spawn unit from core
        sleep(1000)
        Call.setPosition(player.con(), 240f, 240f)
        clientCommand.handleMessage("/god", player)

        // Spawn spector turret and maximum boost
        Call.constructFinish(player.tileOn(), Blocks.spectre, player.unit(), 0, Team.crux, null)
        Call.constructFinish(Vars.world.tile(player.tileX(), player.tileY() + 3), Blocks.itemSource, player.unit(), 0, Team.crux, null)
        Call.constructFinish(Vars.world.tile(player.tileX() + 1, player.tileY() + 3), Blocks.liquidSource, player.unit(), 0, Team.crux, null)
        Vars.world.tile(player.tileX(), player.tileY() + 3).build.configureAny(Items.thorium)
        Vars.world.tile(player.tileX() + 1, player.tileY() + 3).build.configureAny(Liquids.cryofluid)

        // Check unit not dead
        for (time in 0..10) {
            assert(!player.unit().dead)
            sleep(100)
        }
    }

    @Test
    fun client_help() {
        // Test permission
        setPermission("visitor", true)

        clientCommand.handleMessage("/help", player)
        assertTrue(playerData.lastSentMessage.contains("help"))
        clientCommand.handleMessage("/help 1", player)
        assertTrue(playerData.lastSentMessage.contains("help"))
        clientCommand.handleMessage("/help 3", player)
        assertFalse(playerData.lastSentMessage.contains("help"))

        setPermission("user", true)
        clientCommand.handleMessage("/help", player)
        assertTrue(playerData.lastSentMessage.contains("vote"))
        clientCommand.handleMessage("/help 3", player)
        clientCommand.handleMessage("/help 5", player)

        setPermission("owner", true)
        clientCommand.handleMessage("/help", player)
        clientCommand.handleMessage("/help 2", player)
        clientCommand.handleMessage("/help 7", player)
    }

    @Test
    fun client_hub() {

    }

    @Test
    fun client_hud() {

    }

    @Test
    fun client_info() {

    }

    @Test
    fun client_js() {
        // Test js command work
        setPermission("owner", true)
        clientCommand.handleMessage("/js Vars.state.rules.infiniteResources = true", player)
        assertTrue(Vars.state.rules.infiniteResources)

        clientCommand.handleMessage("/js Vars.state.rules.infiniteResources = false", player)
        assertFalse(Vars.state.rules.infiniteResources)

        // Test js command works only owner
        val dummy = newPlayer()
        clientCommand.handleMessage("/js yes", dummy.first)
        assertTrue(dummy.first.con.kicked)

        // Test admin status
        val fakeAdmin = newPlayer()
        setPermission(fakeAdmin.first, "user", true)
        clientCommand.handleMessage("/js yeah", fakeAdmin.first)
        assertTrue(fakeAdmin.first.con.kicked)
    }

    @Test
    fun client_kickall() {

    }

    @Test
    fun client_kill() {
        setPermission("owner", true)
        clientCommand.handleMessage("/kill", player)
        assertTrue(Groups.player.find { p -> p.name == player.name() }.unit().dead())
    }

    @Test
    fun client_killall() {

    }

    @Test
    fun client_killunit() {

    }

    @Test
    fun client_lang() {

    }

    @Test
    fun client_log() {

    }

    @Test
    fun client_login() {

    }

    @Test
    fun client_maps() {

    }

    @Test
    fun client_me() {

    }

    @Test
    fun client_meme() {

    }

    @Test
    fun client_motd() {

    }

    @Test
    fun client_mute() {

    }

    @Test
    fun client_pause() {

    }

    @Test
    fun client_players() {

    }

    @Test
    fun client_pm() {

    }

    @Test
    fun client_ranking() {

    }

    @Test
    fun client_register() {

    }

    @Test
    fun client_report() {

    }

    @Test
    fun client_rollback() {

    }

    @Test
    fun client_search() {

    }

    @Test
    fun client_setitem() {
        setPermission("owner", true)
        clientCommand.handleMessage("/setitem all 500", player)
        assertEquals(500, player.team().core().items().get(Items.copper))
        assertEquals(500, player.team().core().items().get(Items.lead))

        clientCommand.handleMessage("/setitem copper 1000", player)
        assertEquals(1000, player.team().core().items().get(Items.copper))
        assertEquals(500, player.team().core().items().get(Items.lead))

        clientCommand.handleMessage("/setitem copper 1000 sharded", player)
        assertEquals(1000, Vars.state.teams.cores(Team.sharded).first().items().get(Items.copper))
    }

    @Test
    fun client_setperm() {
        setPermission("owner", true)
        val dummy = newPlayer()
        clientCommand.handleMessage("/setperm ${dummy.first.name} admin", player)
        assertEquals("admin", dummy.second.permission)

        clientCommand.handleMessage("/setperm ${dummy.first.name} user", player)
        assertEquals("user", dummy.second.permission)

        leavePlayer(dummy.first)
    }

    @Test
    fun client_skip() {

    }

    @Test
    fun client_spawn() {

    }

    @Test
    fun client_status() {

    }

    @Test
    fun client_t() {

    }

    @Test
    fun client_team() {

    }

    @Test
    fun client_tempban() {

    }

    @Test
    fun client_time() {

    }

    @Test
    fun client_tp() {

    }

    @Test
    fun client_tpp() {

    }

    @Test
    fun client_click_track() {

    }

    @Test
    fun client_unban() {

    }

    @Test
    fun client_unmute() {

    }

    @Test
    fun client_url() {

    }

    @Test
    fun client_weather() {

    }

    @Test
    fun client_vote() {

    }

    @Test
    fun client_votekick() {

    }
}