import ClientCommandTest.Companion.playerData
import PluginTest.Companion.clientCommand
import PluginTest.Companion.loadGame
import PluginTest.Companion.loadPlugin
import PluginTest.Companion.newPlayer
import PluginTest.Companion.player
import PluginTest.Companion.setPermission
import essentials.DB
import essentials.Main.Companion.database
import mindustry.Vars
import mindustry.game.Team
import mindustry.gen.Groups
import org.junit.BeforeClass
import org.junit.Test


class FeatureTest {
    lateinit var playerData: DB.PlayerData

    companion object {
        var done = false

        @BeforeClass
        @JvmStatic fun setup() {
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
    fun pvpBalanceTest() {
        // Require user or above permission
        setPermission("owner", true)

        clientCommand.handleMessage("/changemap Glacier pvp", player)
        Thread.sleep(1000)
        assert(Vars.state.rules.pvp)

        // Create 10 players
        for (index in 0..9) newPlayer()

        /* val random = Random()
         for (data in database.players) {
             data.pvpDefeatCount = random.nextInt(10)
             data.pvpVictoriesCount = random.nextInt(10)
             data.pvpEliminationTeamCount = random.nextInt(20)
             println(data.name + " / " + Trigger.pvpMatch(data.player).name)
         }*/

        clientCommand.handleMessage("/status", player)
        println(playerData.lastSentMessage)

        println("core 개수: " + Vars.state.teams.active.size)

        val s = StringBuilder()
        for (team in Vars.state.teams.active) {
            s.append(team.team.name + " ")
        }
        println("활성화된 팀 : $s")

        var players = mutableListOf<Pair<Team, Double>>()
        database.players.forEach { it ->
            val rate = it.pvpVictoriesCount.toDouble() / (it.pvpVictoriesCount + it.pvpDefeatCount).toDouble()
            players += Pair(it.player.team(), if (rate.equals(Double.NaN)) 0.0 else rate)
        }

        fun winPercentage(team : Team) : Double {
            var players = arrayOf<Pair<Team, Double>>()
            database.players.forEach {
                var rate = it.pvpVictoriesCount.toDouble() / (it.pvpVictoriesCount + it.pvpDefeatCount).toDouble()
                players += Pair(it.player.team(), if (rate.equals(Double.NaN)) 0.0 else rate)
            }

            val targetTeam = players.filter { it.first == team }
            val rate = targetTeam.map { it.second }
            return rate.average()
        }

        println("플레이어 인원 : " + Groups.player.size())
        println("플레이어 비율 : ${players.toString()}")
    }
}