package essential.database.data

import essential.database.table.WorldHistoryTable
import kotlinx.datetime.LocalDateTime
import ksp.table.GenerateCode
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@GenerateCode
data class WorldHistoryData(
    val id: UInt,
    val time: Long,
    val player: String,
    val action: String,
    val x: Short,
    val y: Short,
    val tile: String,
    val rotate: Int,
    val team: String,
    val value: String?,
    val createdAt: LocalDateTime
)

/**
 * Create a new world history entry
 */
suspend fun createWorldHistory(
    time: Long,
    player: String,
    action: String,
    x: Short,
    y: Short,
    tile: String,
    rotate: Int,
    team: String,
    value: String?
): WorldHistoryData {
    return newSuspendedTransaction {
        WorldHistoryTable.insertReturning {
            it[WorldHistoryTable.time] = time
            it[WorldHistoryTable.player] = player
            it[WorldHistoryTable.action] = action
            it[WorldHistoryTable.x] = x
            it[WorldHistoryTable.y] = y
            it[WorldHistoryTable.tile] = tile
            it[WorldHistoryTable.rotate] = rotate
            it[WorldHistoryTable.team] = team
            it[WorldHistoryTable.value] = value
        }.single().toWorldHistoryData()
    }
}

/**
 * Get all world history entries
 */
suspend fun getAllWorldHistory(): List<WorldHistoryData> {
    return newSuspendedTransaction {
        WorldHistoryTable.selectAll()
            .mapToWorldHistoryDataList()
    }
}

/**
 * Get world history entries by player
 */
suspend fun getWorldHistoryByPlayer(player: String): List<WorldHistoryData> {
    return newSuspendedTransaction {
        WorldHistoryTable.selectAll()
            .where { WorldHistoryTable.player eq player }
            .mapToWorldHistoryDataList()
    }
}

/**
 * Get world history entries by action
 */
suspend fun getWorldHistoryByAction(action: String): List<WorldHistoryData> {
    return newSuspendedTransaction {
        WorldHistoryTable.selectAll()
            .where { WorldHistoryTable.action eq action }
            .mapToWorldHistoryDataList()
    }
}

/**
 * Get world history entries by coordinates
 */
suspend fun getWorldHistoryByCoordinates(x: Short, y: Short): List<WorldHistoryData> {
    return newSuspendedTransaction {
        WorldHistoryTable.selectAll()
            .where { (WorldHistoryTable.x eq x) and (WorldHistoryTable.y eq y) }
            .mapToWorldHistoryDataList()
    }
}

/**
 * Delete all world history entries
 */
suspend fun clearWorldHistory() {
    newSuspendedTransaction {
        WorldHistoryTable.deleteAll()
    }
}
