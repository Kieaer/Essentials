import PluginTest.Companion.loadGame
import PluginTest.Companion.newPlayer
import essential.common.database.data.getAverageContribution
import essential.common.database.data.getContributionCount
import essential.common.database.data.getPlayerContributions
import essential.common.database.data.insertContribution
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContributionTest {
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
    fun contribution_insertAndAverage() {
        val target = newPlayer()
        runBlocking {
            insertContribution(target.second, "pvp", "map-a", 100.0)
            insertContribution(target.second, "pvp", "map-b", 200.0)
            insertContribution(target.second, "survival", "map-c", 300.0)

            assertEquals(3, getContributionCount(target.second))
            assertEquals(200.0, getAverageContribution(target.second), 0.001)

            val rows = getPlayerContributions(target.second)
            assertEquals(3, rows.size)
        }
    }

    @Test
    fun contribution_emptyAverageIsZero() {
        val target = newPlayer()
        runBlocking {
            assertEquals(0, getContributionCount(target.second))
            assertEquals(0.0, getAverageContribution(target.second), 0.001)
        }
    }
}
