import PluginTest.Companion.clientCommand
import PluginTest.Companion.loadGame
import PluginTest.Companion.newPlayer
import PluginTest.Companion.player
import PluginTest.Companion.setPermission
import essential.common.database.data.checkRoutingPermission
import essential.common.database.data.plugin.WarpBlock
import essential.common.database.table.ServerRoutingTable
import essential.common.players
import essential.common.pluginData
import essential.common.systemTimezone
import essential.core.Main
import essential.core.connectPacket
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toLocalDateTime
import mindustry.Vars
import mindustry.game.EventType.ConnectPacketEvent
import mindustry.game.Team
import mindustry.net.NetConnection
import mindustry.net.Packets
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime


class FeatureTest {
    companion object {
        private var done = false
    }

    private class TestConnection(address: String) : NetConnection(address) {
        var kickedMessage: String? = null

        override fun send(`object`: Any?, reliable: Boolean) {}

        override fun close() {}

        override fun kick(reason: String, kickDuration: Long) {
            kicked = true
            kickedMessage = reason
        }
    }

    private fun awaitCondition(timeoutMs: Long = 3000L, intervalMs: Long = 50L, condition: () -> Boolean): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) return true
            Thread.sleep(intervalMs)
        }
        return condition()
    }

    private fun makeConnectPacket(name: String, uuid: String): Packets.ConnectPacket {
        return Packets.ConnectPacket().apply {
            this.name = name
            this.uuid = uuid
            this.locale = "ko"
            this.version = 0
            this.versionType = "test"
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun seedRoutingPermission(playerUuid: String, hubServerName: String, targetServerName: String, targetPort: Int = 6567, validSeconds: Int = 60) {
        val now = Clock.System.now().toLocalDateTime(systemTimezone)
        val expiresAt = (Clock.System.now() + validSeconds.seconds).toLocalDateTime(systemTimezone)

        runBlocking {
            suspendTransaction {
                ServerRoutingTable.insert {
                    it[ServerRoutingTable.playerUuid] = playerUuid
                    it[ServerRoutingTable.hubServerName] = hubServerName
                    it[ServerRoutingTable.targetServerName] = targetServerName
                    it[ServerRoutingTable.targetPort] = targetPort
                    it[ServerRoutingTable.hubConnectionTime] = now
                    it[ServerRoutingTable.routingAllowedTime] = now
                    it[ServerRoutingTable.isUsed] = false
                    it[ServerRoutingTable.usedTime] = null
                    it[ServerRoutingTable.expiresAt] = expiresAt
                }
            }
        }
    }

    private fun clearRoutingPermission(playerUuid: String) {
        runBlocking {
            suspendTransaction {
                ServerRoutingTable.deleteWhere { ServerRoutingTable.playerUuid eq playerUuid }
            }
        }
    }

    @BeforeTest
    fun setup() {
        if (!done) {
            System.setProperty("test", "yes")
            loadGame(true)

            val p = newPlayer()
            Vars.player = p.first.self()
            player = p.first.self()

            done = true
        }
    }

    @Test
    fun serverRoutingAllowWhenReachedExpectedServerFromHubWarpBlock() {
        val testPlayer: mindustry.gen.Player = player.self()
        val currentMapName = Vars.state.map.name()
        val originalHubMapName = pluginData.hubMapName
        val originalWarpBlocks = pluginData.data.warpBlock.map { it.copy().apply { online = it.online } }

        try {
            val hubServerName = "hub-server"
            pluginData.hubMapName = hubServerName
            pluginData.data.warpBlock.clear()
            pluginData.data.warpBlock.add(
                WarpBlock(
                    mapName = hubServerName,
                    x = 0,
                    y = 0,
                    tileName = "router",
                    size = 1,
                    ip = "127.0.0.1",
                    port = 6567,
                    description = currentMapName
                ).apply { online = true }
            )

            val targetPort = Vars.port
            seedRoutingPermission(testPlayer.uuid(), hubServerName, currentMapName, targetPort)

            assertTrue(
                awaitCondition {
                    runBlocking { checkRoutingPermission(testPlayer.uuid(), targetPort) }
                },
                "허브 워프 블록 이동 후 현재 서버 포트에 대한 라우팅 권한이 생성되어야 합니다."
            )

            val connection = TestConnection("127.0.0.1")
            val packet = makeConnectPacket(testPlayer.name(), testPlayer.uuid())
            connectPacket(ConnectPacketEvent(connection, packet))

            assertTrue(
                awaitCondition {
                    !runBlocking { checkRoutingPermission(testPlayer.uuid(), targetPort) }
                },
                "대상 서버 접속이 허용되면 라우팅 권한이 사용 처리되어야 합니다."
            )
            assertFalse(connection.kicked)
            assertNull(connection.kickedMessage)
        } finally {
            pluginData.hubMapName = originalHubMapName
            pluginData.data.warpBlock.clear()
            pluginData.data.warpBlock.addAll(originalWarpBlocks)
            clearRoutingPermission(testPlayer.uuid())
        }
    }

    @Test
    fun serverRoutingDenyWhenReachedDifferentServerFromHubWarpBlock() {
        val testPlayer: mindustry.gen.Player = player.self()
        val originalHubMapName = pluginData.hubMapName
        val originalWarpBlocks = pluginData.data.warpBlock.map { it.copy().apply { online = it.online } }

        try {
            val hubServerName = "hub-server"
            pluginData.hubMapName = hubServerName
            pluginData.data.warpBlock.clear()
            pluginData.data.warpBlock.add(
                WarpBlock(
                    mapName = hubServerName,
                    x = 0,
                    y = 0,
                    tileName = "router",
                    size = 1,
                    ip = "127.0.0.1",
                    port = 6567,
                    description = "another-server"
                ).apply { online = true }
            )

            val targetPort = Vars.port + 1
            seedRoutingPermission(testPlayer.uuid(), hubServerName, "another-server", targetPort)

            assertTrue(
                awaitCondition {
                    runBlocking { checkRoutingPermission(testPlayer.uuid(), targetPort) }
                },
                "허브 워프 블록 이동 후 설정된 대상 서버 포트로 라우팅 권한이 생성되어야 합니다."
            )

            val connection = TestConnection("127.0.0.1")
            val packet = makeConnectPacket(testPlayer.name(), testPlayer.uuid())
            connectPacket(ConnectPacketEvent(connection, packet))

            assertTrue(
                awaitCondition { connection.kicked },
                "허용되지 않은 대상 서버 포트(현재 서버 포트와 불일치)로 직접 접속 시도가 오면 연결이 거부되어야 합니다."
            )
            assertEquals(connection.kickedMessage?.contains("Direct connection denied"), true)
            assertTrue(runBlocking { checkRoutingPermission(testPlayer.uuid(), targetPort) })
        } finally {
            pluginData.hubMapName = originalHubMapName
            pluginData.data.warpBlock.clear()
            pluginData.data.warpBlock.addAll(originalWarpBlocks)
            clearRoutingPermission(testPlayer.uuid())
        }
    }

    @Test
    fun pvpBalanceTest() {
        setPermission("owner", true)

        // Enable autoTeam in config
        Main.conf = Main.conf.copy(feature = Main.conf.feature.copy(pvp = Main.conf.feature.pvp.copy(autoTeam = true)))

        clientCommand.handleMessage("/changemap Glacier pvp", player)
        assertTrue(awaitCondition(7000L, 100L) { Vars.state.rules.pvp })

        // Scenario: 4 teams (A, B, C, D) with 2 players each
        // Win rates: A=100%, B=75%, C=50%, D=25%
        val testTeams = listOf(Team.sharded, Team.crux, Team.green, Team.blue)
        for (team in testTeams) {
            val tile = PluginTest.randomTile()
            tile.setNet(mindustry.content.Blocks.coreShard, team, 0)
        }
        
        val activeTeams = Vars.state.teams.active.filter { testTeams.contains(it.team) }.toList()
        assertEquals(4, activeTeams.size, "Need 4 teams for test")
        
        val winRates = listOf(1.0, 0.75, 0.5, 0.25)
        
        // Fill teams with 2 players each
        for (i in activeTeams.indices) {
            val team = activeTeams[i].team
            val rate = winRates[i]
            repeat(2) {
                val p = newPlayer()
                // Force join the specific team for initial setup
                p.first.team(team)
                p.second.pvpWinCount = (rate * 100).toInt().toShort()
                p.second.pvpLoseCount = ((1.0 - rate) * 100).toInt().toShort()
            }
        }

        // The teams should be sorted by win rate: D, C, B, A (lowest first)
        val sortedActiveTeams = activeTeams.sortedBy { teamData ->
            val teamPlayers = players.filter { it.player.team() == teamData.team }
            if (teamPlayers.isEmpty()) 0.5 else teamPlayers.map {
                val total = it.pvpWinCount + it.pvpLoseCount
                if (total == 0) 0.5 else it.pvpWinCount.toDouble() / total
            }.average()
        }
        
        val teamLowest = sortedActiveTeams[0].team
        val teamSecondLowest = sortedActiveTeams[1].team

        // Add one more player - should go to teamLowest (lowest win rate)
        val p9 = newPlayer()
        assertEquals(teamLowest, p9.first.team(), "Player 9 should go to lowest win rate team")

        // Add one more player - should go to teamLowest (it can have up to 2 more than others)
        val p10 = newPlayer()
        assertEquals(teamLowest, p10.first.team(), "Player 10 should go to lowest win rate team")

        // Now teamLowest has 4 players (2 initial + 2 new), others have 2. 
        // min=2, Lowest=4. 4 < 2 + 2 is false.
        // Next player should go to teamSecondLowest
        val p11 = newPlayer()
        assertEquals(teamSecondLowest, p11.first.team(), "Player 11 should go to second lowest win rate team")

        clientCommand.handleMessage("/status", player)

        // Verify final counts
        val finalCounts = activeTeams.map { teamData ->
            players.count { it.player.team() == teamData.team }
        }
        // The one with lowest win rate should have 4, one with second lowest should have 3, others 2.
        assertTrue(finalCounts.contains(4))
        assertTrue(finalCounts.contains(3))
    }
}
