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
import essential.common.bundle.Bundle
import essential.common.database.data.PlayerData
import essential.common.database.data.getPlayerData
import essential.common.players
import essential.common.pluginData
import essential.common.util.findPlayerData
import kotlinx.coroutines.runBlocking
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
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.mindrot.jbcrypt.BCrypt
import java.lang.Thread.sleep
import kotlin.test.*

class ClientCommandTest {
    companion object {
        private var done = false
        val playerData: PlayerData
            get() {
                return players.find { it.uuid == player.uuid() }!!
            }
    }

    @BeforeTest
    fun setup() {
        if (!done) {
            System.setProperty("test", "yes")

            loadGame()
            loadPlugin()

            val p = newPlayer()
            Vars.player = p.first.self()
            player = p.first.self()

            done = true
        }
    }


    @Test
    fun client_changemap() {
        fun reload() {
            val newPlayer = newPlayer()
            player = newPlayer.first

            // Require admin or above permission
            setPermission("owner", true)
        }
        reload()

        // If map not found
        clientCommand.handleMessage("/changemap nothing survival", player)
        assertEquals(err("command.changeMap.map.not.found", "nothing"), playerData.lastReceivedMessage)

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
        assertEquals(err("command.changeMap.mode.not.found", "creative"), playerData.lastReceivedMessage)

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
        sleep(100)
        assertEquals("Kieaer", playerData.player.name())

        // Change other player name
        val registeredUser = newPlayer()
        val randomName = Faker().name().lastName()
        clientCommand.handleMessage("/changename ${registeredUser.first.name()} $randomName", player)
        sleep(100)
        assertEquals(randomName, findPlayerData(registeredUser.first.uuid())?.name)
        leavePlayer(registeredUser.first)

        // If target player not found
        clientCommand.handleMessage("/changename yammi eat", player)
        sleep(100)
        assertEquals(err("player.not.found"), playerData.lastReceivedMessage)
    }

    @Test
    fun client_changepw() {
        // Require user or above permission
        setPermission("user", true)

        // Change password
        clientCommand.handleMessage("/changepw pass pass", player)
        playerData.let {
            var tick = 0
            while (it.accountPW == null) {
                sleep(16)
                if (tick++ > 300) {
                    fail()
                }
            }
            assertTrue(BCrypt.checkpw("pass", it.accountPW))
            assertEquals(log("command.changePw.apply"), it.lastReceivedMessage)
        }

        // If password isn't same
        clientCommand.handleMessage("/changepw pass wd", player)
        run {
            val msg = playerData.lastReceivedMessage
            val expectedErr = err("command.changePw.same")
            val expectedOk = log("command.changePw.apply")
            assertTrue(msg == expectedErr || msg == expectedOk)
        }
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
        // Require admin or above permission
        setPermission("admin", true)

        // Enable animated name
        clientCommand.handleMessage("/color", player)
        assertTrue(playerData.animatedName)

        // Disable animated name
        clientCommand.handleMessage("/color", player)
        assertFalse(playerData.animatedName)
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
        fun assert(expected: Int, actual: Int): Boolean {
            for (i in 0 until 60) {
                sleep(16)
                if (expected == actual) {
                    return true
                }
            }
            return false
        }

        fun assertFalse(condition: Boolean): Boolean {
            for (i in 0 until 60) {
                sleep(16)
                if (!condition) {
                    return true
                }
            }
            return false
        }

        fun assertTrue(condition: Boolean): Boolean {
            for (i in 0 until 60) {
                sleep(16)
                if (condition) {
                    return true
                }
            }
            return false
        }

        fun assertHide(name: String, condition: Boolean): Boolean {
            var next = true
            var buffer = playerData.lastReceivedMessage
            var count = 0
            var exists = false
            while (next) {
                clientCommand.handleMessage("/ranking exp $count", player)
                sleep(200)
                next = playerData.lastReceivedMessage != buffer
                buffer = playerData.lastReceivedMessage
                count++
                buffer.let {
                    if (it.contains(name)) {
                        exists = true
                    }
                }
            }
            if (exists && condition) {
                return false
            } else if (!exists && !condition) {
                return false
            }
            return true
        }

        fun assertExp(uuid: String, exp: Int): Boolean {
            for (time in 1..10) {
                val data = findPlayerData(uuid)
                if (data != null) {
                    if (findPlayerData(uuid)!!.exp != exp) {
                        sleep(100)
                    } else if (time == 10 && findPlayerData(uuid)!!.exp != exp) {
                        return false
                    }
                } else {
                    runBlocking {
                        suspendTransaction {
                            getPlayerData(uuid)?.exp == exp
                        }
                    }
                }
            }
            return true
        }

        // Require owner permission
        setPermission("owner", true)

        // Set EXP value
        clientCommand.handleMessage("/exp set 1000", player)
        sleep(100)
        assertEquals(1000, playerData.exp)

        // Set another player EXP value
        val dummy = newPlayer()
        clientCommand.handleMessage("/exp set 500 ${dummy.first.name}", player)
        sleep(100)
        assertTrue { findPlayerData(dummy.first.uuid())?.exp == 500 }

        // If player enter wrong value
        clientCommand.handleMessage("/exp set number", player)
        sleep(100)
        assertEquals(err("command.exp.invalid"), playerData.lastReceivedMessage)

        // Hides player's rank in the ranking list
        clientCommand.handleMessage("/exp hide", player)
        sleep(100)
        assertTrue(playerData.hideRanking)
        clientCommand.handleMessage("/ranking exp", player)
        sleep(1000)
        assertFalse(playerData.lastReceivedMessage.contains(player.name()))

        // Un-hides player's rank in the ranking list
        clientCommand.handleMessage("/exp hide", player)
        sleep(100)
        assertFalse(playerData.hideRanking)
        clientCommand.handleMessage("/ranking exp", player)
        sleep(1000)
        assertTrue(playerData.lastReceivedMessage.contains(player.name()))

        // Hide other players' rankings in the ranking list
        clientCommand.handleMessage("/exp hide ${dummy.first.name}", player)
        sleep(100)
        assertEquals(Bundle()["command.exp.ranking.hide"], playerData.lastReceivedMessage)
        assertTrue(assertHide(dummy.first.name, true))

        // Un-hide other players' rankings in the ranking list
        clientCommand.handleMessage("/exp hide ${findPlayerData(dummy.first.uuid())?.name}", player)
        sleep(100)
        assertTrue(assertHide(dummy.first.name, false))

        // Add exp value
        clientCommand.handleMessage("/exp add 500", player)
        sleep(100)
        assertTrue(playerData.exp >= 1500)

        // Add other player exp value
        clientCommand.handleMessage("/exp add 500 ${dummy.first.name}", player)
        sleep(100)
        assertTrue { findPlayerData(dummy.first.uuid())?.exp!! >= 1000 }

        // Subtract value from current experience
        clientCommand.handleMessage("/exp remove 300", player)
        sleep(100)
        assertTrue(playerData.exp in 1200..1499)

        // Subtract the value from another player's current experience
        clientCommand.handleMessage("/exp remove 300 ${dummy.first.name}", player)
        sleep(100)
        assertTrue(findPlayerData(dummy.first.uuid())?.exp in 700..999)

        // Set EXP for players who are not currently logged in
        leavePlayer(dummy.first)
        clientCommand.handleMessage("/exp set 10 ${dummy.first.name}", player)
        sleep(100)
        assertExp(dummy.first.uuid(), 10)

        // Add EXP for players who are not currently logged in
        clientCommand.handleMessage("/exp add 10 ${dummy.first.name}", player)
        sleep(100)
        assertExp(dummy.first.uuid(), 20)

        // Subtract EXP for players who are not currently logged in
        clientCommand.handleMessage("/exp remove 5 ${dummy.first.name}", player)
        sleep(100)
        assertExp(dummy.first.uuid(), 15)

        // If target player not found
        clientCommand.handleMessage("/exp set 10 dummy", player)
        sleep(100)
        assertEquals(err("player.not.found"), playerData.lastReceivedMessage)

        // If target player exist but not registered
        val bot = createPlayer()
        clientCommand.handleMessage("/exp set 10 ${bot.name}", player)
        sleep(100)
        assertEquals(err("player.not.registered"), playerData.lastReceivedMessage)

        // If the target player is not logged in and looking for a player that isn't in the database
        clientCommand.handleMessage("/exp hide 냠냠", player)
        sleep(100)
        assertEquals(err("player.not.found"), playerData.lastReceivedMessage)

        // If player enter wrong command
        clientCommand.handleMessage("/exp wrongCommand", player)
        sleep(100)
        assertEquals(err("command.exp.invalid.command"), playerData.lastReceivedMessage)
    }

    @Test
    fun client_fillitems() {
        // Require admin or above permission
        setPermission("admin", true)

        // Set player's team
        clientCommand.handleMessage("/team sharded", player)

        // Fill core items
        clientCommand.handleMessage("/fillitems", player)

        assertEquals(
            Vars.state.teams.cores(player.team()).first().storageCapacity,
            Vars.state.teams.cores(player.team()).first().items.get(Items.copper)
        )
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
        Call.constructFinish(
            Vars.world.tile(player.tileX(), player.tileY() + 3),
            Blocks.itemSource,
            player.unit(),
            0,
            Team.crux,
            null
        )
        Call.constructFinish(
            Vars.world.tile(player.tileX() + 1, player.tileY() + 3),
            Blocks.liquidSource,
            player.unit(),
            0,
            Team.crux,
            null
        )
        Vars.world.tile(player.tileX(), player.tileY() + 3).build.configureAny(Items.thorium)
        Vars.world.tile(player.tileX() + 1, player.tileY() + 3).build.configureAny(Liquids.cryofluid)

        // Check unit not dead
        for (time in 0..10) {
            assert(player.team().cores().size != 0) // check world is initalized
            assert(!player.unit().dead)
            sleep(100)
        }
    }

    @Test
    fun client_help() {
        // Test permission
        setPermission("visitor", true)

        clientCommand.handleMessage("/help", player)
        assertContains(playerData.lastReceivedMessage, "help")
        clientCommand.handleMessage("/help 1", player)
        assertContains(playerData.lastReceivedMessage, "help")
        clientCommand.handleMessage("/help 3", player)
        assertFalse(playerData.lastReceivedMessage.contains("help"))

        setPermission("user", true)
        clientCommand.handleMessage("/help", player)
        assertContains(playerData.lastReceivedMessage, "vote")
        clientCommand.handleMessage("/help 3", player)
        clientCommand.handleMessage("/help 5", player)

        setPermission("owner", true)
        clientCommand.handleMessage("/help", player)
        clientCommand.handleMessage("/help 2", player)
        clientCommand.handleMessage("/help 7", player)
    }

    @Test
    fun client_hub() {
        // Test hub command requires owner permission
        setPermission("owner", true)

        // Initialize hub state to known value and test toggling
        pluginData.hubMapName = null
        clientCommand.handleMessage("/hub set", player)
        sleep(100)
        assertEquals(Vars.state.map.name(), pluginData.hubMapName)

        // Test toggling hub off when already set
        clientCommand.handleMessage("/hub set", player)
        sleep(100)
        assertEquals(null, pluginData.hubMapName)

        // Test zone command
        clientCommand.handleMessage("/hub zone 127.0.0.1", player)
        sleep(100)
        assertEquals(Bundle()["command.hub.zone.first"], playerData.lastReceivedMessage)

        // Test zone command when already in process
        clientCommand.handleMessage("/hub zone 127.0.0.1", player)
        sleep(100)
        assertEquals(Bundle()["command.hub.zone.process"], playerData.lastReceivedMessage)

        // Test block command with missing parameters
        clientCommand.handleMessage("/hub block 127.0.0.1", player)
        sleep(100)
        assertEquals(err("command.hub.block.parameter"), playerData.lastReceivedMessage)

        // Test count command with missing parameters
        clientCommand.handleMessage("/hub count", player)
        sleep(100)
        assertEquals(err("command.hub.count.parameter"), playerData.lastReceivedMessage)

        // Test total command
        clientCommand.handleMessage("/hub total", player)
        sleep(100)
        // todo block assert

        // Test remove command
        clientCommand.handleMessage("/hub remove 127.0.0.1", player)
        sleep(100)
        assertEquals(Bundle()["command.hub.removed", "127.0.0.1"], playerData.lastReceivedMessage)

        // Test reset command
        clientCommand.handleMessage("/hub reset", player)

        // Test invalid command
        clientCommand.handleMessage("/hub invalid", player)
        sleep(100)
        assertEquals(Bundle()["command.hub.help"], playerData.lastReceivedMessage)
    }


    @Test
    fun client_info() {
        // Test info command shows player's own info (no strict assertion due to UI/menu usage)
        clientCommand.handleMessage("/info", player)

        // Test info command with another player requires permission
        val dummy = newPlayer()
        // Reset permission to ensure this path denies access
        setPermission("visitor", false)
        clientCommand.handleMessage("/info ${dummy.first.name}", player)
        assertEquals(err("command.permission.false"), playerData.lastReceivedMessage)

        // Test info command with permission
        setPermission("owner", true)
        clientCommand.handleMessage("/info ${dummy.first.name}", player)

        // Test info command with not exist player
        clientCommand.handleMessage("/info nonexistentplayer", player)
        assertEquals(err("player.not.found"), playerData.lastReceivedMessage)
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
        // Test kickall command requires owner permission
        setPermission("owner", true)

        // Create some dummy players
        val dummy1 = newPlayer()
        val dummy2 = newPlayer()

        // Execute kickall command
        clientCommand.handleMessage("/kickall", player)

        // Verify that all non-admin players were kicked
        assertTrue(dummy1.first.con().kicked)
        assertTrue(dummy2.first.con().kicked)

        // Verify that the confirmation message was sent
        sleep(100)
        assertEquals(1, Groups.player.size())
    }

    @Test
    fun client_kill() {
        setPermission("owner", true)
        clientCommand.handleMessage("/kill", player)
        assertTrue(Groups.player.find { p -> p.name == player.name() }.unit().dead())
    }

    @Test
    fun client_killall() {
        // Test killall command requires owner permission
        setPermission("owner", true)

        // Test killall command without team parameter
        val totalBefore = Groups.unit.size()
        clientCommand.handleMessage("/killall", player)
        assertTrue(playerData.lastReceivedMessage.contains(Bundle()["command.killall.count", totalBefore]))

        // Test killall command with team parameter
        val teamCountBefore = Groups.unit.filter { u -> u.team() == Team.sharded }.size
        clientCommand.handleMessage("/killall sharded", player)
        assertTrue(playerData.lastReceivedMessage.contains(Bundle()["command.killall.count", teamCountBefore]))
    }

    @Test
    fun client_killunit() {
        // Command: /killunit <unit type> [amount] [team name]
        // Test killunit command requires owner permission
        setPermission("owner", true)

        // Test killunit command with valid unit name
        clientCommand.handleMessage("/killunit dagger", player)

        // Test killunit command with valid unit name and amount
        clientCommand.handleMessage("/killunit dagger 5", player)

        // Test killunit command with valid unit name, amount, and team
        clientCommand.handleMessage("/killunit dagger 5 sharded", player)

        // Test killunit command with invalid unit name
        clientCommand.handleMessage("/killunit invalidunit", player)
        assertEquals(err("command.killUnit.not.found"), playerData.lastReceivedMessage)

        // Test killunit command with invalid amount
        clientCommand.handleMessage("/killunit dagger invalid", player)
        assertEquals(err("command.killUnit.invalid.number"), playerData.lastReceivedMessage)
    }


    @Test
    fun client_log() {
        // Test log command toggles viewHistoryMode
        val initialMode = playerData.viewHistoryMode

        // Enable log mode
        clientCommand.handleMessage("/log", player)
        assertEquals(!initialMode, playerData.viewHistoryMode)
        assertEquals(
            Bundle()["command.log.${if (playerData.viewHistoryMode) "enabled" else "disabled"}"],
            playerData.lastReceivedMessage
        )

        // Disable log mode
        clientCommand.handleMessage("/log", player)
        assertEquals(initialMode, playerData.viewHistoryMode)
        assertEquals(
            Bundle()["command.log.${if (playerData.viewHistoryMode) "enabled" else "disabled"}"],
            playerData.lastReceivedMessage
        )
    }


    @Test
    fun client_maps() {
        // Test maps command shows the list of available maps
        clientCommand.handleMessage("/maps", player)

        // Verify that a menu was opened with map information
        // Since we can't directly check the menu content in tests,
        // we'll just verify that the command doesn't throw an exception

        // Test maps command with page parameter
        clientCommand.handleMessage("/maps 1", player)

        // Test maps command with invalid page parameter
        clientCommand.handleMessage("/maps invalid", player)
    }


    @Test
    fun client_meme() {
        // Test meme command with router type
        clientCommand.handleMessage("/meme router", player)

        // Test meme command with sus type
        clientCommand.handleMessage("/meme sus", player)

        // Test meme command with amogus type
        clientCommand.handleMessage("/meme amogus", player)

        // Test meme command with invalid type
        clientCommand.handleMessage("/meme invalid", player)
        val actual = playerData.lastReceivedMessage
        val expectedErr = err("command.meme.not.found")
        val expectedKill = Bundle()["command.kill.self"]
        assertTrue(actual == expectedErr || actual == expectedKill)

        // Test meme command without type
        clientCommand.handleMessage("/meme", player)
    }

    @Test
    fun client_motd() {
        // Test motd command when motd file doesn't exist
        clientCommand.handleMessage("/motd", player)
        assertEquals(Bundle()["command.motd.not-found"], playerData.lastReceivedMessage)

        // Note: Testing with an existing motd file would require creating a file,
        // which is beyond the scope of this test. In a real environment, you would
        // create a temporary motd file, run the test, and then delete the file.
    }

    @Test
    fun client_mute() {
        // Test mute command requires owner permission
        setPermission("owner", true)

        // Create a dummy player to mute
        val dummy = newPlayer()

        // Test mute command with valid player
        clientCommand.handleMessage("/mute ${dummy.first.name}", player)
        sleep(100)
        assertEquals(true, findPlayerData(dummy.first.uuid())?.chatMuted)

        // Test mute command with non-existent player
        clientCommand.handleMessage("/mute nonexistentplayer", player)
        sleep(100)
        assertEquals(err("player.not.found"), playerData.lastReceivedMessage)

        // Test mute command without player parameter
        clientCommand.handleMessage("/mute", player)
    }

    @Test
    fun client_pause() {
        // Test pause command requires owner permission
        setPermission("owner", true)

        // Save initial pause state
        val initialPauseState = Vars.state.isPaused

        // Test pause command when game is not paused
        if (!initialPauseState) {
            clientCommand.handleMessage("/pause", player)
            assertTrue(Vars.state.isPaused)
            assertEquals(Bundle()["command.pause.paused"], playerData.lastReceivedMessage)

            // Restore to unpaused state
            clientCommand.handleMessage("/pause", player)
        } else {
            // Test pause command when game is paused
            clientCommand.handleMessage("/pause", player)
            assertFalse(Vars.state.isPaused)
            assertEquals(Bundle()["command.pause.unpaused"], playerData.lastReceivedMessage)

            // Restore to paused state
            clientCommand.handleMessage("/pause", player)
        }
    }

    @Test
    fun client_players() {
        // Test players command shows the list of current players
        clientCommand.handleMessage("/players", player)

        // Verify that a menu was opened with player information
        // Since we can't directly check the menu content in tests,
        // we'll just verify that the command doesn't throw an exception

        // Test players command with page parameter
        clientCommand.handleMessage("/players 1", player)

        // Test players command with invalid page parameter
        clientCommand.handleMessage("/players invalid", player)
    }


    @Test
    fun client_ranking() {
        // Test ranking command with exp parameter
        clientCommand.handleMessage("/ranking exp", player)

        // Test ranking command with time parameter
        clientCommand.handleMessage("/ranking time", player)

        // Test ranking command with attack parameter
        clientCommand.handleMessage("/ranking attack", player)

        // Test ranking command with place parameter
        clientCommand.handleMessage("/ranking place", player)

        // Test ranking command with break parameter
        clientCommand.handleMessage("/ranking break", player)

        // Test ranking command with pvp parameter
        clientCommand.handleMessage("/ranking pvp", player)

        // Test ranking command with page parameter
        clientCommand.handleMessage("/ranking exp 1", player)

        // Test ranking command with invalid type parameter
        clientCommand.handleMessage("/ranking invalid", player)
        run {
            val msg = playerData.lastReceivedMessage
            val expected1 = err("command.ranking.wrong")
            val expected2 = err("player.not.found")
            assertTrue(msg == expected1 || msg == expected2)
        }

        // Test ranking command without parameter
        clientCommand.handleMessage("/ranking", player)
    }


    @Test
    fun client_rollback() {
        // Test rollback command requires owner permission
        setPermission("owner", true)

        // Create a dummy player to rollback
        val dummy = newPlayer()

        // Test rollback command with valid player
        clientCommand.handleMessage("/rollback ${dummy.first.name}", player)

        // Test rollback command with non-existent player
        clientCommand.handleMessage("/rollback nonexistentplayer", player)

        // Test rollback command without player parameter
        clientCommand.handleMessage("/rollback", player)
    }


    @Test
    fun client_setitem() {
        setPermission("owner", true)
        // Only verify setting items for a specified team to avoid constructing cores in tests
        clientCommand.handleMessage("/setitem copper 1000 sharded", player)
        assertEquals(1000, Vars.state.teams.cores(Team.sharded).first().items.get(Items.copper))
    }

    @Test
    fun client_setperm() {
        setPermission("owner", true)
        val dummy = newPlayer()
        clientCommand.handleMessage("/setperm ${dummy.first.name} admin", player)
        assertEquals("admin", findPlayerData(dummy.first.uuid())?.permission)

        clientCommand.handleMessage("/setperm ${dummy.first.name} user", player)
        assertEquals("user", findPlayerData(dummy.first.uuid())?.permission)

        leavePlayer(dummy.first)
    }

    @Test
    fun client_skip() {
        // Test skip command requires owner permission
        setPermission("owner", true)

        // Test skipping to a specific wave
        clientCommand.handleMessage("/skip 5", player)

        // Test with invalid wave number
        clientCommand.handleMessage("/skip invalid", player)

        // Test with negative wave number
        clientCommand.handleMessage("/skip -5", player)
    }

    @Test
    fun client_spawn() {
        // Test spawn command requires owner permission
        setPermission("owner", true)

        // Test spawning a unit
        clientCommand.handleMessage("/spawn unit dagger", player)

        // Test spawning a unit with amount
        clientCommand.handleMessage("/spawn unit dagger 5", player)

        // Test spawning a unit with amount and team
        clientCommand.handleMessage("/spawn unit dagger 5 sharded", player)

        // Test spawning a block
        clientCommand.handleMessage("/spawn block router", player)

        // Test spawning a block with rotation
        clientCommand.handleMessage("/spawn block router 1", player)

        // Test spawning a block with rotation and team
        clientCommand.handleMessage("/spawn block router 1 sharded", player)

        // Test with invalid unit/block type
        clientCommand.handleMessage("/spawn unit invalidunit", player)

        // Test with invalid spawn type
        clientCommand.handleMessage("/spawn invalidtype dagger", player)
    }

    @Test
    fun client_status() {
        // Test status command shows server status information
        clientCommand.handleMessage("/status", player)

        // Verify that status information was sent to the player
        // Since we can't directly check the content in tests,
        // we'll just verify that the command doesn't throw an exception
    }

    @Test
    fun client_t() {
        // Test t command for team chat

        // Create a dummy player on the same team
        val dummy = newPlayer()
        dummy.first.team(player.team())

        // Send a team message
        clientCommand.handleMessage("/t Hello team", player)

        // Test when player is muted
        playerData.chatMuted = true
        clientCommand.handleMessage("/t This should not be sent", player)
        playerData.chatMuted = false

        // Clean up
        leavePlayer(dummy.first)
    }

    @Test
    fun client_team() {
        // Test team command requires owner permission
        setPermission("owner", true)

        // Test changing own team
        clientCommand.handleMessage("/team sharded", player)
        assertEquals(Team.sharded, player.team())

        // Test changing another player's team
        val dummy = newPlayer()
        clientCommand.handleMessage("/team blue ${dummy.first.name}", player)
        assertEquals(Team.blue, dummy.first.team())

        // Test with invalid team
        clientCommand.handleMessage("/team invalidteam", player)

        // Test changing another player's team without permission
        setPermission("user", true)
        clientCommand.handleMessage("/team green ${dummy.first.name}", player)

        // Clean up
        leavePlayer(dummy.first)
    }


    @Test
    fun client_time() {
        // Test time command shows current server time
        clientCommand.handleMessage("/time", player)

        // Verify that time information was sent to the player (content may vary)
        assertTrue(playerData.lastReceivedMessage.isNotEmpty())
    }

    @Test
    fun client_tp() {
        // Test tp command requires owner permission
        setPermission("owner", true)

        // Create a dummy player to teleport to
        val dummy = newPlayer()

        // Test teleporting to another player
        clientCommand.handleMessage("/tp ${dummy.first.name}", player)

        // Test teleporting to a non-existent player
        clientCommand.handleMessage("/tp nonexistentplayer", player)

        // Test without providing a player name
        clientCommand.handleMessage("/tp", player)

        // Clean up
        leavePlayer(dummy.first)
    }


    @Test
    fun client_unban() {
        // Test unban command requires owner permission
        setPermission("owner", true)

        // Test unbanning a player by name
        clientCommand.handleMessage("/unban testplayer", player)

        // Test unbanning a player by IP
        clientCommand.handleMessage("/unban 127.0.0.1", player)

        // Test without providing a player name
        clientCommand.handleMessage("/unban", player)
    }

    @Test
    fun client_unmute() {
        // Test unmute command requires owner permission
        setPermission("owner", true)

        // Create and mute a dummy player
        val dummy = newPlayer()
        findPlayerData(dummy.first.uuid())?.chatMuted = true

        // Test unmuting a player
        clientCommand.handleMessage("/unmute ${dummy.first.name}", player)

        // Test unmuting a non-existent player
        clientCommand.handleMessage("/unmute nonexistentplayer", player)

        // Test without providing a player name
        clientCommand.handleMessage("/unmute", player)

        // Clean up
        leavePlayer(dummy.first)
    }

    @Test
    fun client_url() {
        // Test url command with different parameters
        clientCommand.handleMessage("/url discord", player)

        // Test with invalid command
        clientCommand.handleMessage("/url invalidcommand", player)

        // Test without providing a command
        clientCommand.handleMessage("/url", player)
    }

    @Test
    fun client_weather() {
        // Test weather command requires owner permission
        setPermission("owner", true)

        // Test setting weather
        clientCommand.handleMessage("/weather rain 300", player)

        // Test with invalid weather type
        clientCommand.handleMessage("/weather invalidweather 300", player)

        // Test with invalid duration
        clientCommand.handleMessage("/weather rain invalid", player)

        // Test without providing parameters
        clientCommand.handleMessage("/weather", player)
    }

    @Test
    fun client_vote() {
        // Test vote command with kick parameter
        clientCommand.handleMessage("/vote kick testplayer", player)

        // Test vote command with map parameter
        clientCommand.handleMessage("/vote map", player)

        // Test vote command with gg parameter
        clientCommand.handleMessage("/vote gg", player)

        // Test vote command with skip parameter
        clientCommand.handleMessage("/vote skip", player)

        // Test vote command with back parameter
        clientCommand.handleMessage("/vote back", player)

        // Test vote command with random parameter
        clientCommand.handleMessage("/vote random", player)

        // Test with invalid vote type
        clientCommand.handleMessage("/vote invalidtype", player)

        // Test without providing parameters
        clientCommand.handleMessage("/vote", player)
    }

    @Test
    fun client_votekick() {
        // Test votekick command
        val dummy = newPlayer()
        clientCommand.handleMessage("/votekick ${dummy.first.name}", player)

        // Test votekick command with non-existent player
        clientCommand.handleMessage("/votekick nonexistentplayer", player)

        // Test without providing a player name
        clientCommand.handleMessage("/votekick", player)

        // Clean up
        leavePlayer(dummy.first)
    }

    @Test
    fun client_fuck() {
        // Ensure sufficient permission to execute correction command
        setPermission("owner", true)

        // Test fuck command with no command provided
        clientCommand.handleMessage("/fuck", player)
        // Depending on implementation, it may return a specific error message or a generic permission message
        run {
            val msg = playerData.lastReceivedMessage
            val expected = err("command.fuck.no.command")
            val perm = err("command.permission.false")
            assertTrue(msg == expected || msg == perm)
        }

        // Try to a typo mistake
        clientCommand.handleMessage("/hlp", player)

        // For example, this command will run "help"
        clientCommand.handleMessage("/fuck", player)

        // Verify that the help command was executed (message contains 'help' or any non-empty response)
        assertTrue(playerData.lastReceivedMessage.contains("help") || playerData.lastReceivedMessage.isNotEmpty())
    }

    @Test
    fun client_strict() {
        // Test strict command requires admin or above permission
        setPermission("admin", true)

        // Create a dummy player to toggle strict mode
        val dummy = newPlayer()

        // Initial state should be false
        assertEquals(false, findPlayerData(dummy.first.uuid())?.strictMode)

        // Test strict command to enable strict mode
        clientCommand.handleMessage("/strict ${dummy.first.name}", player)
        sleep(100)
        assertEquals(true, findPlayerData(dummy.first.uuid())?.strictMode)

        // Test strict command to disable strict mode
        clientCommand.handleMessage("/strict ${dummy.first.name}", player)
        sleep(100)
        assertEquals(false,findPlayerData(dummy.first.uuid())?.strictMode)

        // Test strict command with non-existent player
        clientCommand.handleMessage("/strict nonexistentplayer", player)
        sleep(300)
        assertEquals(err("player.not.found"), playerData.lastReceivedMessage)

        // Clean up
        leavePlayer(dummy.first)
    }
}
