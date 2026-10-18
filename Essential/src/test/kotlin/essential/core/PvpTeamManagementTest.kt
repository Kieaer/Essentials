package essential.core

import PluginTest.Companion.loadGame
import PluginTest.Companion.newPlayer
import arc.Events
import arc.func.Cons
import essential.common.config.Config
import essential.common.event.CustomEvents
import mindustry.Vars
import mindustry.content.UnitTypes
import mindustry.content.Blocks
import mindustry.game.EventType.BuildingBulletDestroyEvent
import mindustry.game.EventType.GameOverEvent
import mindustry.game.EventType.PlayerLeave
import mindustry.game.EventType.WorldLoadEvent
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Bullet
import mindustry.net.Administration
import mindustry.world.blocks.storage.CoreBlock.CoreBuild
import org.mockito.Mockito
import kotlin.test.*

class PvpTeamManagementTest {
    private lateinit var originalConfig: CoreConfig
    private var originalPvp = false

    @BeforeTest
    fun setup() {
        System.setProperty("test", "yes")
        loadGame(true)
        originalConfig = Main.conf
        originalPvp = Vars.state.rules.pvp
        Vars.state.rules.pvp = true
        Main.conf = Main.conf.copy(
            feature = Main.conf.feature.copy(pvp = PvP(autoTeam = true, spector = true, manageTeams = false))
        )
    }

    @AfterTest
    fun cleanup() {
        Main.conf = originalConfig
        Vars.state.rules.pvp = originalPvp
        pvpPlayer.clear()
        pvpSpecters.clear()
    }

    @Test
    fun configurationPreservesDefaultsAndSupportsExternalTeamManagement() {
        val oldConfig = Config.yaml.decodeFromString(
            CoreConfig.serializer(), "feature:\n  pvp:\n    autoTeam: true\n    spector: true\n"
        )
        assertTrue(oldConfig.feature.pvp.manageTeams)
        val externalConfig = oldConfig.copy(
            feature = oldConfig.feature.copy(pvp = oldConfig.feature.pvp.copy(manageTeams = false))
        )
        val yaml = Config.yaml.encodeToString(CoreConfig.serializer(), externalConfig)
        val restored = Config.yaml.decodeFromString(CoreConfig.serializer(), yaml)
        assertFalse(restored.feature.pvp.manageTeams)
        assertTrue(restored.feature.pvp.autoTeam)
        assertTrue(restored.feature.pvp.spector)
    }

    @Test
    fun worldLoadClearsPvpStateWithoutReassigningExternalTeams() {
        val (player, data) = newPlayer()
        val team = Team.get(6)
        player.team(team)
        val unit = UnitTypes.dagger.create(team)
        player.unit(unit)
        pvpPlayer[data.uuid] = Team.crux
        pvpSpecters.add(data.uuid)
        data.currentPlayTime = 100
        data.viewHistoryMode = true

        worldLoad(WorldLoadEvent())

        assertEquals(team, player.team())
        assertSame(unit, player.unit())
        assertTrue(pvpPlayer.isEmpty())
        assertTrue(pvpSpecters.isEmpty())
        assertEquals(0, data.currentPlayTime)
        assertFalse(data.viewHistoryMode)
    }

    @Test
    fun dataLoadDoesNotRestoreStaleTeamOverExternalAssignment() {
        val (player, data) = newPlayer()
        val team = Team.get(6)
        player.team(team)
        pvpPlayer[data.uuid] = Team.crux

        playerDataLoad(CustomEvents.PlayerDataLoad(data))

        assertEquals(team, player.team())
        assertTrue(data.isConnected)
    }

    @Test
    fun dataLoadDoesNotMoveExternalPlayersToSpectatorTeam() {
        val (player, data) = newPlayer()
        val team = Team.get(6)
        player.team(team)
        pvpPlayer.remove(data.uuid)
        pvpSpecters.add(data.uuid)

        playerDataLoad(CustomEvents.PlayerDataLoad(data))

        assertEquals(team, player.team())
        assertFalse(data.uuid in pvpPlayer)
    }

    @Test
    fun dataLoadDoesNotAutoAssignExternalPlayersToExistingCoreTeam() {
        val (player, data) = newPlayer()
        val team = Team.get(6)
        player.team(team)
        pvpPlayer.remove(data.uuid)
        pvpSpecters.remove(data.uuid)
        assertTrue(Vars.state.teams.active.any { it.team != team && it.hasCore() })

        playerDataLoad(CustomEvents.PlayerDataLoad(data))

        assertEquals(team, player.team())
        assertFalse(data.uuid in pvpPlayer)
        assertNull(selectAutoTeam(data))
    }

    @Test
    fun automaticTeamActionRestrictionIsDisabledForExternalSpectators() {
        val (player, data) = newPlayer()
        player.team(Team.derelict)
        data.strictMode = false
        val action = Administration.PlayerAction().apply {
            this.player = player
            type = Administration.ActionType.configure
        }

        assertTrue(actionFilter.allow(action))
        Main.conf = Main.conf.copy(
            feature = Main.conf.feature.copy(pvp = Main.conf.feature.pvp.copy(manageTeams = true))
        )
        assertFalse(actionFilter.allow(action))
    }

    @Test
    fun playerLeaveDoesNotEndExternallyManagedMatch() {
        val (player, _) = newPlayer()
        newPlayer()
        val originalTeams = Groups.player.toList().associateWith { it.team() }
        val autoPause = Administration.Config.autoPause.bool()
        var gameOverEvents = 0
        val listener = Cons<GameOverEvent> { gameOverEvents++ }
        try {
            originalTeams.keys.forEach { it.team(Team.sharded) }
            Administration.Config.autoPause.set(true)
            Events.on(GameOverEvent::class.java, listener)

            playerLeave(PlayerLeave(player))

            assertEquals(0, gameOverEvents)
        } finally {
            Events.remove(GameOverEvent::class.java, listener)
            Administration.Config.autoPause.set(autoPause)
            originalTeams.forEach { (p, team) -> p.team(team) }
        }
    }

    @Test
    fun coreDestructionRecordsStatisticsWithoutEndingExternalMatch() {
        val (_, data) = newPlayer()
        val originalTeams = Groups.player.toList().associateWith { it.team() }
        val gameOver = Vars.state.gameOver
        var gameOverEvents = 0
        val listener = Cons<GameOverEvent> { gameOverEvents++ }
        try {
            originalTeams.keys.forEach { it.team(Team.sharded) }
            Vars.state.gameOver = false
            assertTrue(Vars.netServer.isWaitingForPlayers)
            val build = Mockito.mock(CoreBuild::class.java).apply {
                block = Blocks.coreShard
                team = Team.crux
            }
            val bullet = Bullet.create().apply { team = Team.sharded }
            val eliminated = data.pvpEliminatedCount
            Events.on(GameOverEvent::class.java, listener)

            buildingBulletDestroy(BuildingBulletDestroyEvent(build, bullet))

            assertEquals(0, gameOverEvents)
            assertEquals(eliminated + 1, data.pvpEliminatedCount.toInt())
        } finally {
            Events.remove(GameOverEvent::class.java, listener)
            Vars.state.gameOver = gameOver
            originalTeams.forEach { (p, team) -> p.team(team) }
        }
    }
}
