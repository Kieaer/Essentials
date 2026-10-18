package essential.core

import PluginTest.Companion.loadGame
import PluginTest.Companion.newPlayer
import essential.common.event.CustomEvents
import mindustry.Vars
import mindustry.content.UnitTypes
import mindustry.game.EventType.WorldLoadEvent
import mindustry.game.Team
import kotlin.test.*

class HexedCompatibilityTest {
    private lateinit var originalConfig: CoreConfig
    private var originalPvp = false
    private var originalHexedTag: String? = null

    @BeforeTest
    fun setup() {
        System.setProperty("test", "yes")
        loadGame(true)
        originalConfig = Main.conf
        originalPvp = Vars.state.rules.pvp
        originalHexedTag = Vars.state.rules.tags.get("hexed")
        Vars.state.rules.pvp = true
        Vars.state.rules.tags.put("hexed", "true")
        Main.conf = Main.conf.copy(
            feature = Main.conf.feature.copy(pvp = PvP(autoTeam = true, spector = true))
        )
    }

    @AfterTest
    fun cleanup() {
        Main.conf = originalConfig
        Vars.state.rules.pvp = originalPvp
        val tag = originalHexedTag
        if (tag == null) Vars.state.rules.tags.remove("hexed") else Vars.state.rules.tags.put("hexed", tag)
        pvpPlayer.clear()
        pvpSpecters.clear()
    }

    @Test
    fun worldLoadClearsPvpStateWithoutReassigningHexedPlayers() {
        val (player, data) = newPlayer()
        val personalTeam = Team.get(6)
        player.team(personalTeam)
        val unit = UnitTypes.alpha.create(personalTeam)
        player.unit(unit)
        pvpPlayer[data.uuid] = Team.crux
        pvpSpecters.add(data.uuid)
        data.currentPlayTime = 100
        data.viewHistoryMode = true

        worldLoad(WorldLoadEvent())

        assertEquals(personalTeam, player.team())
        assertSame(unit, player.unit())
        assertTrue(pvpPlayer.isEmpty())
        assertTrue(pvpSpecters.isEmpty())
        assertEquals(0, data.currentPlayTime)
        assertFalse(data.viewHistoryMode)
    }

    @Test
    fun dataLoadDoesNotRestoreStaleTeamOverHexedAssignment() {
        val (player, data) = newPlayer()
        val personalTeam = Team.get(6)
        player.team(personalTeam)
        pvpPlayer[data.uuid] = Team.crux

        playerDataLoad(CustomEvents.PlayerDataLoad(data))

        assertEquals(personalTeam, player.team())
        assertTrue(data.isConnected)
    }

    @Test
    fun dataLoadDoesNotSpectateHexedPlayers() {
        val (player, data) = newPlayer()
        val personalTeam = Team.get(6)
        player.team(personalTeam)
        pvpPlayer.remove(data.uuid)
        pvpSpecters.add(data.uuid)

        playerDataLoad(CustomEvents.PlayerDataLoad(data))

        assertEquals(personalTeam, player.team())
        assertFalse(data.uuid in pvpPlayer)
    }

    @Test
    fun dataLoadDoesNotAutoAssignHexedPlayersToExistingCoreTeam() {
        val (player, data) = newPlayer()
        val personalTeam = Team.get(6)
        player.team(personalTeam)
        pvpPlayer.remove(data.uuid)
        pvpSpecters.remove(data.uuid)
        assertTrue(Vars.state.teams.active.any { it.team != personalTeam && it.hasCore() })

        playerDataLoad(CustomEvents.PlayerDataLoad(data))

        assertEquals(personalTeam, player.team())
        assertFalse(data.uuid in pvpPlayer)
    }
}
