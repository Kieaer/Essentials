import PluginTest.Companion.loadGame
import PluginTest.Companion.newPlayer
import PluginTest.Companion.serverCommand
import arc.Events
import essential.common.database.data.getPlayerData
import kotlinx.coroutines.runBlocking
import mindustry.game.EventType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerCommandTest {
    companion object {
        private var done = false
    }

    @BeforeTest
    fun setup() {
        if (!done) {
            loadGame(true)

            done = true
        }
    }

    @Test
    fun server_mergePlayer() {
        val target = newPlayer()
        val dest = newPlayer()

        target.second.exp = 100000
        dest.second.exp = 100000

        Events.fire(EventType.PlayerLeave(target.first))
        Events.fire(EventType.PlayerLeave(dest.first))

        serverCommand.handleMessage("mergeplayer ${target.first.uuid()} ${dest.first.uuid()}")

        assertEquals(200000, runBlocking {
            getPlayerData(dest.first.uuid())?.exp
        })
    }
}