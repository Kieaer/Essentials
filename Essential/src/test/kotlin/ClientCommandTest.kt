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
import essential.core.*
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
import net.datafaker.Faker
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
                loadGame()
                loadPlugin()

                val p = newPlayer()
                Vars.player = p.first.self()
                player = p.first.self()
                playerData = p.second

                done = true
            }
        }
    }

    @Test
    fun client_changemap() {
        // Require admin or above permission
        setPermission("owner", true)

        // If map not found
        clientCommand.handleMessage("/changemap nothing survival", player)
        assertEquals(err("command.changemap.map.not.found", "nothing"), playerData.lastSentMessage)

        // Number method
        clientCommand.handleMessage("/changemap 0 survival", player)
        assertEquals("Ancient Caldera", Vars.state.map.name())

        // Name method
        clientCommand.handleMessage("/changemap fork survival", player)
        assertEquals("Fork", Vars.state.map.name())

        // If player enter wrong gamemode
        clientCommand.handleMessage("/changemap fork creative", player)
        assertEquals(err("command.changemap.mode.not.found", "creative"), playerData.lastSentMessage)

        // If player enter only map name
        clientCommand.handleMessage("/changemap glacier", player)
        assertEquals("Glacier", Vars.state.map.name())
        assertEquals(Gamemode.survival, Vars.state.rules.mode())
    }

    @Test
    fun client_changename() {
        // Require admin or above permission
        setPermission("owner", true)

        // Change self name
        clientCommand.handleMessage("/changename Kieaer", player)
        assertEquals("Kieaer", player.name())

        // Change other player name
        val registeredUser = newPlayer()
        val randomName = Faker().name().lastName()
        clientCommand.handleMessage("/changename $randomName ${registeredUser.first.name()}", player)
        sleep(100)
        assertEquals(randomName, database.players.find { p -> p.uuid == registeredUser.second.uuid }!!.name)
        leavePlayer(registeredUser.first)

        // If target player not found
        clientCommand.handleMessage("/changename eat yammi", player)
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
        assertNull(Vars.netServer.chatFormatter.format(dummy.first, "hello"))

        // But if player has chat.admin permission, still can chat
        assertNotNull(Vars.netServer.chatFormatter.format(player, "hello"))

        // Set all players unmute
        clientCommand.handleMessage("/chat on", player)
        assertNotNull(Vars.netServer.chatFormatter.format(dummy.first, "hello"))

        leavePlayer(dummy.first)
    }

    fun client_chars() {
        // Require admin or above permission
        setPermission("admin", true)

        // todo chars 명령어
    }

    @Test
    fun client_color() {
        // Require admin or above permission
        setPermission("admin", true)

        // Enable animated name
        clientCommand.handleMessage("/color", player)
        sleep(1250)
        assertTrue(player.name.contains("[#ff0000]"))

        // Disable animated name
        clientCommand.handleMessage("/color", player)
        sleep(1250)
        assertFalse(player.name.contains("[#ff0000]"))
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

        sleep(1000)

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
        // Require owner permission
        setPermission("owner", true)

        // Set EXP value
        clientCommand.handleMessage("/exp set 1000", player)
        assertEquals(1000, playerData.exp)

        // Set another player EXP value
        val dummy = newPlayer()
        clientCommand.handleMessage("/exp set 500 ${dummy.first.name}", player)
        assertEquals(500, dummy.second.exp)

        // If player enter wrong value
        clientCommand.handleMessage("/exp set number", player)
        assertEquals(err("command.exp.invalid"), playerData.lastSentMessage)

        // Hides player's rank in the ranking list
        clientCommand.handleMessage("/exp hide", player)
        database.update(player.uuid(), playerData)
        assertEquals(Bundle()["command.exp.ranking.hide"], playerData.lastSentMessage)
        clientCommand.handleMessage("/ranking exp", player)
        sleep(250)
        assertFalse(playerData.lastSentMessage.contains(player.name))

        // Un-hides player's rank in the ranking list
        clientCommand.handleMessage("/exp hide", player)
        database.update(player.uuid(), playerData)
        assertEquals(Bundle()["command.exp.ranking.unhide"], playerData.lastSentMessage)
        clientCommand.handleMessage("/ranking exp", player)
        sleep(250)
        assertTrue(playerData.lastSentMessage.contains(player.name))

        // Hide other players' rankings in the ranking list
        clientCommand.handleMessage("/exp hide ${dummy.first.name}", player)
        database.update(dummy.first.uuid(), dummy.second)
        assertEquals(Bundle()["command.exp.ranking.hide"], playerData.lastSentMessage)
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
            if (buffer.contains(dummy.first.name)) {
                exists = true
            }
        }
        if (exists) fail()
        buffer = ""
        count = 0
        exists = false
        next = true

        // Un-hide other players' rankings in the ranking list
        clientCommand.handleMessage("/exp hide ${dummy.second.name}", player)
        database.update(dummy.first.uuid(), dummy.second)
        assertEquals(Bundle()["command.exp.ranking.unhide"], playerData.lastSentMessage)
        while (next) {
            clientCommand.handleMessage("/ranking exp $count", player)
            sleep(200)
            next = playerData.lastSentMessage != buffer
            buffer = playerData.lastSentMessage
            count++
            if (buffer.contains(dummy.first.name)) {
                exists = true
            }
        }
        if (!exists) fail()

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
        for (time in 1..10) {
            if (database[dummy.first.uuid()]!!.exp != 10) {
                sleep(200)
            } else if (time == 10 && database[dummy.first.uuid()]!!.exp != 10) {
                fail()
            }
        }

        // Add EXP for players who are not currently logged in
        clientCommand.handleMessage("/exp add 10 ${dummy.first.name}", player)
        for (time in 1..10) {
            if (database[dummy.first.uuid()]!!.exp != 10) {
                sleep(200)
            } else if (time == 10 && database[dummy.first.uuid()]!!.exp != 20) {
                fail()
            }
        }

        // Subtract EXP for players who are not currently logged in
        clientCommand.handleMessage("/exp remove ${dummy.first.name}", player)
        for (time in 1..10) {
            if (database[dummy.first.uuid()]!!.exp != 10) {
                sleep(200)
            } else if (time == 10 && database[dummy.first.uuid()]!!.exp != 0) {
                fail()
            }
        }

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

    }

    @Test
    fun client_kickall() {

    }

    @Test
    fun client_kill() {

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

    }

    @Test
    fun client_setperm() {

    }

    @Test
    fun client_skip() {

    }

    @Test
    fun client_spanw() {

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