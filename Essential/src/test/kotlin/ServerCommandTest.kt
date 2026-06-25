import PluginTest.Companion.loadGame
import PluginTest.Companion.newPlayer
import PluginTest.Companion.serverCommand
import PluginTest.Companion.waitUntil
import arc.Events
import essential.common.database.data.getPlayerData
import essential.common.database.data.setAchievement
import essential.common.database.table.AchievementTable
import essential.common.database.table.PlayerTable
import kotlinx.coroutines.runBlocking
import mindustry.game.EventType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import kotlin.test.*

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

        assertTrue(
            waitUntil {
                runBlocking {
                    getPlayerData(target.first.uuid())?.exp == 100000 &&
                        getPlayerData(dest.first.uuid())?.exp == 100000
                }
            },
            "Player exp should be persisted before merge"
        )

        serverCommand.handleMessage("mergeplayer ${target.first.uuid()} ${dest.first.uuid()}")

        assertTrue(
            waitUntil { runBlocking { getPlayerData(dest.first.uuid())?.exp == 200000 } },
            "Merged exp should be 200000 but was ${runBlocking { getPlayerData(dest.first.uuid())?.exp }}"
        )
    }

    @Test
    fun server_deletePlayerByUuid() {
        val target = newPlayer()
        runBlocking {
            setAchievement(target.second, "test_achievement")
        }
        Events.fire(EventType.PlayerLeave(target.first))

        val uuid = target.first.uuid()
        val beforeDelete = runBlocking { getPlayerData(uuid) }
        assertNotNull(beforeDelete)

        serverCommand.handleMessage("delete $uuid")

        val afterDelete = runBlocking { getPlayerData(uuid) }
        assertNull(afterDelete)

        val achievementsCount = runBlocking {
            suspendTransaction {
                AchievementTable.selectAll().where { AchievementTable.playerId eq target.second.id }.count()
            }
        }
        assertEquals(0, achievementsCount)
    }

    @Test
    fun server_deletePlayerById() {
        val target = newPlayer()
        Events.fire(EventType.PlayerLeave(target.first))

        val uuid = target.first.uuid()
        val beforeDelete = runBlocking { getPlayerData(uuid) }
        assertNotNull(beforeDelete)
        val id = target.second.id

        serverCommand.handleMessage("delete $id")

        val afterDelete = runBlocking { getPlayerData(uuid) }
        assertNull(afterDelete)
    }

    @Test
    fun server_deletePlayerByName_multiple() {
        val target1 = newPlayer()
        val target2 = newPlayer()

        runBlocking {
            suspendTransaction {
                PlayerTable.update({ PlayerTable.id eq target1.second.id }) {
                    it[name] = "multipleplayer"
                }
                PlayerTable.update({ PlayerTable.id eq target2.second.id }) {
                    it[name] = "MULTIPLEPLAYER"
                }
            }
        }

        Events.fire(EventType.PlayerLeave(target1.first))
        Events.fire(EventType.PlayerLeave(target2.first))

        serverCommand.handleMessage("delete multipleplayer")

        val afterDelete1 = runBlocking { getPlayerData(target1.first.uuid()) }
        assertNotNull(afterDelete1)
        val afterDelete2 = runBlocking { getPlayerData(target2.first.uuid()) }
        assertNotNull(afterDelete2)

        serverCommand.handleMessage("delete ${target1.second.id}")

        val afterDeleteId1 = runBlocking { getPlayerData(target1.first.uuid()) }
        assertNull(afterDeleteId1)
        val afterDeleteId2 = runBlocking { getPlayerData(target2.first.uuid()) }
        assertNotNull(afterDeleteId2)

        serverCommand.handleMessage("delete ${target2.second.id}")
        val afterDeleteId2Clean = runBlocking { getPlayerData(target2.first.uuid()) }
        assertNull(afterDeleteId2Clean)
    }
}