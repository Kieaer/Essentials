import PluginTest.Companion.clientCommand
import PluginTest.Companion.loadGame
import PluginTest.Companion.newPlayer
import PluginTest.Companion.player
import PluginTest.Companion.setPermission
import essential.common.players
import essential.core.Main
import mindustry.Vars
import mindustry.game.Team
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class FeatureTest {
    companion object {
        private var done = false
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
    fun pvpBalanceTest() {
        setPermission("owner", true)

        // Enable autoTeam in config
        Main.conf = Main.conf.copy(feature = Main.conf.feature.copy(pvp = Main.conf.feature.pvp.copy(autoTeam = true)))

        clientCommand.handleMessage("/changemap Glacier pvp", player)
        Thread.sleep(2000)
        assertTrue(Vars.state.rules.pvp)

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
            val teamPlayers = players.filter { it.player != null && it.player.team() == teamData.team }
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
