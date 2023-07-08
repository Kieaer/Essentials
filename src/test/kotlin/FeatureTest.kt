import PluginTest.clientCommand
import PluginTest.loadGame
import PluginTest.loadPlugin
import PluginTest.newPlayer
import PluginTest.path
import PluginTest.player
import PluginTest.setPermission
import essentials.DB
import essentials.Main.Companion.database
import essentials.Trigger
import mindustry.Vars
import mindustry.game.Team
import mindustry.gen.Groups
import org.junit.AfterClass
import org.junit.Test
import java.util.*

class FeatureTest {
    companion object {
        @AfterClass
        @JvmStatic
        fun shutdown() {
            path.child("mods/Essentials").deleteDirectory()
            path.child("maps").deleteDirectory()
        }
    }

    var playerData : DB.PlayerData

    init {
        loadGame()
        loadPlugin()

        val p = newPlayer()
        Vars.player = p.first.self()
        player = p.first.self()
        playerData = p.second
    }

    @Test
    fun pvpBalanceTest() {
        // Require user or above permission
        setPermission("owner", true)

        clientCommand.handleMessage("/changemap Glacier pvp", player)
        Thread.sleep(1000)

        // Create 10 players
        for (index in 0..9) newPlayer()

        val random = Random()
        for (data in database.players) {
            data.pvpDefeatCount = random.nextInt(10)
            data.pvpVictoriesCount = random.nextInt(10)
            data.pvpEliminationTeamCount = random.nextInt(20)
            println(data.name + " / " + Trigger.pvpMatch(data.player).name)
        }

        clientCommand.handleMessage("/status", player)
        println(playerData.lastSentMessage)

        println("core 개수: " + Vars.state.teams.active.size)

        val s = StringBuilder()
        for (team in Vars.state.teams.active) {
            s.append(team.team.name + " ")
        }
        println("활성화된 팀 : $s")

        val teams = mutableListOf<Pair<Team, Double>>()
        database.players.forEachIndexed { _, data ->
            val winRate = (data.pvpVictoriesCount.toDouble() / (data.pvpVictoriesCount + data.pvpDefeatCount)) * 100
            teams += Pair(data.player.team(), winRate)
        }

        println("플레이어 인원 : " + Groups.player.size())
        println("플레이어 비율 : $teams")
    }
}